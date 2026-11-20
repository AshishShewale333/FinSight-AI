package com.finsight.stockexplorer.application;

import com.finsight.stockexplorer.api.StockExplorerListingResponse;
import com.finsight.common.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockExplorerService {
    private final JdbcTemplate jdbc;

    public StockExplorerService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "stockExplorerListings", key = "#query + ':' + #page + ':' + #size")
    public Page<StockExplorerListingResponse> search(String query, int page, int size) {
        String term = query == null ? "" : query.trim();
        if (term.length() > 120) throw new ApiException(HttpStatus.BAD_REQUEST, "QUERY_TOO_LONG", "Search query may not exceed 120 characters");
        int offset = page * size;
        List<StockExplorerListingResponse> rows;
        long total;
        if (term.isBlank()) {
            total = jdbc.queryForObject("SELECT COUNT(*) FROM stock_explorer_listings WHERE active = TRUE", Long.class);
            rows = jdbc.query("SELECT id, exchange_code, symbol, company_name, series, listing_date, paid_up_value, market_lot, isin, face_value, active "
                            + "FROM stock_explorer_listings WHERE active = TRUE ORDER BY symbol LIMIT ? OFFSET ?",
                    (rs, row) -> new StockExplorerListingResponse(rs.getString("id"), rs.getString("exchange_code"),
                            rs.getString("symbol"), rs.getString("company_name"), rs.getString("series"),
                            rs.getObject("listing_date", java.time.LocalDate.class), rs.getBigDecimal("paid_up_value"),
                            (Integer) rs.getObject("market_lot"), rs.getString("isin"), rs.getBigDecimal("face_value"),
                            rs.getBoolean("active")), size, offset);
        } else {
            String like = "%" + term + "%";
            total = jdbc.queryForObject("SELECT COUNT(*) FROM stock_explorer_listings WHERE active = TRUE "
                    + "AND (symbol LIKE ? OR company_name LIKE ? OR isin LIKE ?)", Long.class, like, like, like);
            rows = jdbc.query("SELECT id, exchange_code, symbol, company_name, series, listing_date, paid_up_value, market_lot, isin, face_value, active "
                            + "FROM stock_explorer_listings WHERE active = TRUE AND (symbol LIKE ? OR company_name LIKE ? OR isin LIKE ?) "
                            + "ORDER BY symbol LIMIT ? OFFSET ?",
                    (rs, row) -> new StockExplorerListingResponse(rs.getString("id"), rs.getString("exchange_code"),
                            rs.getString("symbol"), rs.getString("company_name"), rs.getString("series"),
                            rs.getObject("listing_date", java.time.LocalDate.class), rs.getBigDecimal("paid_up_value"),
                            (Integer) rs.getObject("market_lot"), rs.getString("isin"), rs.getBigDecimal("face_value"),
                            rs.getBoolean("active")), like, like, like, size, offset);
        }
        return new PageImpl<>(rows, PageRequest.of(page, size), total);
    }
}
