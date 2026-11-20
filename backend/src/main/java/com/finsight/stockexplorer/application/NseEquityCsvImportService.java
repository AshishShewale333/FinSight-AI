package com.finsight.stockexplorer.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class NseEquityCsvImportService {
    private static final String SOURCE_KEY = "NSE_EQUITY_L";
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter LISTING_DATE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-uuuu")
            .toFormatter(Locale.ENGLISH);
    private static final String UPSERT = "INSERT INTO stock_explorer_listings "
            + "(id, exchange_code, symbol, company_name, series, listing_date, paid_up_value, market_lot, isin, face_value, active, updated_at) "
            + "VALUES (?, 'NSE', ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?) "
            + "ON DUPLICATE KEY UPDATE company_name=VALUES(company_name), series=VALUES(series), listing_date=VALUES(listing_date), "
            + "paid_up_value=VALUES(paid_up_value), market_lot=VALUES(market_lot), isin=VALUES(isin), face_value=VALUES(face_value), "
            + "active=TRUE, updated_at=VALUES(updated_at)";

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public NseEquityCsvImportService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public boolean alreadyImportedToday(LocalDate today) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM stock_explorer_import_state " +
                        "WHERE source_key = ? AND last_import_date = ? " +
                        "AND EXISTS (SELECT 1 FROM stock_explorer_listings WHERE active = TRUE)",
                Integer.class, SOURCE_KEY, today);
        return count != null && count > 0;
    }

    @Transactional
    @CacheEvict(cacheNames = "stockExplorerListings", allEntries = true)
    public int importFile(Path path) throws IOException {
        List<SecurityRow> securities = parse(path);
        if (securities.isEmpty()) throw new IllegalArgumentException("The NSE CSV contains no valid securities");

        Timestamp importedAt = Timestamp.from(clock.instant());
        jdbc.update("UPDATE stock_explorer_listings SET active = FALSE WHERE exchange_code = 'NSE'");
        jdbc.batchUpdate(UPSERT, securities, 250, (statement, security) -> {
            statement.setString(1, security.id());
            statement.setString(2, security.symbol());
            statement.setString(3, security.name());
            statement.setString(4, security.series());
            if (security.listingDate() == null) statement.setNull(5, Types.DATE);
            else statement.setObject(5, security.listingDate());
            if (security.paidUpValue() == null) statement.setNull(6, Types.DECIMAL);
            else statement.setBigDecimal(6, security.paidUpValue());
            if (security.marketLot() == null) statement.setObject(7, null);
            else statement.setInt(7, security.marketLot());
            statement.setString(8, security.isin());
            if (security.faceValue() == null) statement.setNull(9, Types.DECIMAL);
            else statement.setBigDecimal(9, security.faceValue());
            statement.setTimestamp(10, importedAt);
        });

        LocalDate importedDate = LocalDate.now(clock.withZone(INDIA));
        jdbc.update("INSERT INTO stock_explorer_import_state (source_key, last_import_date, last_imported_at, records_imported) "
                        + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_import_date=VALUES(last_import_date), "
                        + "last_imported_at=VALUES(last_imported_at), records_imported=VALUES(records_imported)",
                SOURCE_KEY, importedDate, importedAt, securities.size());
        return securities.size();
    }

    private List<SecurityRow> parse(Path path) throws IOException {
        if (Files.size(path) > 10 * 1024 * 1024) throw new IllegalArgumentException("NSE CSV exceeds the 10 MB import limit");
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
        List<List<String>> rows = parseCsv(text);
        if (rows.size() < 2) throw new IllegalArgumentException("NSE CSV must include a header and at least one data row");

        Map<String, Integer> columns = new HashMap<>();
        List<String> header = rows.get(0);
        for (int index = 0; index < header.size(); index++) columns.put(normalize(header.get(index)), index);
        int symbolColumn = requiredColumn(columns, "SYMBOL");
        int nameColumn = requiredColumn(columns, "NAME OF COMPANY");
        int seriesColumn = requiredColumn(columns, "SERIES");
        int listingDateColumn = requiredColumn(columns, "DATE OF LISTING");
        int paidUpColumn = requiredColumn(columns, "PAID UP VALUE");
        int marketLotColumn = requiredColumn(columns, "MARKET LOT");
        int isinColumn = requiredColumn(columns, "ISIN NUMBER");
        int faceValueColumn = requiredColumn(columns, "FACE VALUE");

        List<SecurityRow> securities = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            String symbol = value(row, symbolColumn).toUpperCase(Locale.ROOT);
            String name = value(row, nameColumn);
            if (symbol.isBlank() || name.isBlank()) continue;
            String series = value(row, seriesColumn);
            if (series.isBlank()) continue;
            securities.add(new SecurityRow(stableId("NSE", symbol), symbol, name, series,
                    parseDate(value(row, listingDateColumn)), parseDecimal(value(row, paidUpColumn)),
                    parseInteger(value(row, marketLotColumn)), emptyToNull(value(row, isinColumn)),
                    parseDecimal(value(row, faceValueColumn))));
        }
        return securities;
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (current == ',' && !quoted) {
                row.add(field.toString().trim());
                field.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') index++;
                row.add(field.toString().trim());
                field.setLength(0);
                if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row);
                row = new ArrayList<>();
            } else field.append(current);
        }
        if (quoted) throw new IllegalArgumentException("NSE CSV has an unterminated quoted field");
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString().trim());
            if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row);
        }
        return rows;
    }

    private int requiredColumn(Map<String, Integer> columns, String name) {
        Integer index = columns.get(normalize(name));
        if (index == null) throw new IllegalArgumentException("NSE CSV is missing expected column: " + name);
        return index;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace('\uFEFF', ' ').trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String value(List<String> row, int column) {
        return column < row.size() && row.get(column) != null ? row.get(column).trim() : "";
    }

    private LocalDate parseDate(String value) {
        if (value.isBlank()) return null;
        try { return LocalDate.parse(value, LISTING_DATE); }
        catch (Exception exception) { throw new IllegalArgumentException("Invalid listing date in NSE CSV: " + value); }
    }

    private BigDecimal parseDecimal(String value) {
        if (value.isBlank()) return null;
        try { return new BigDecimal(value.replace(",", "")); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid numeric value in NSE CSV: " + value); }
    }

    private Integer parseInteger(String value) {
        if (value.isBlank()) return null;
        try { return Integer.valueOf(value.replace(",", "")); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid market lot in NSE CSV: " + value); }
    }

    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private String stableId(String exchange, String symbol) {
        return UUID.nameUUIDFromBytes((exchange + ":" + symbol).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private record SecurityRow(String id, String symbol, String name, String series, LocalDate listingDate,
            BigDecimal paidUpValue, Integer marketLot, String isin, BigDecimal faceValue) { }
}
