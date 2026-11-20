package com.finsight.stockexplorer.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(name = "finsight.stock-explorer.import-enabled", havingValue = "true", matchIfMissing = true)
public class NseEquityCsvStartupImporter implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NseEquityCsvStartupImporter.class);
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final NseEquityCsvImportService importer;
    private final Clock clock;
    private final Path csvPath;

    public NseEquityCsvStartupImporter(NseEquityCsvImportService importer, Clock clock,
            @Value("${finsight.stock-explorer.csv-path:./data/nse/EQUITY_L.csv}") String csvPath) {
        this.importer = importer;
        this.clock = clock;
        this.csvPath = Path.of(csvPath).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now(clock.withZone(INDIA));
        if (importer.alreadyImportedToday(today)) {
            log.info("Stock Explorer CSV was already imported today; skipping daily startup import");
            return;
        }
        if (!Files.isRegularFile(csvPath)) {
            log.info("Stock Explorer CSV not found at {}; place the downloaded file there to import it", csvPath);
            return;
        }
        try {
            int imported = importer.importFile(csvPath);
            log.info("Imported {} NSE equity directory rows from local CSV", imported);
        } catch (Exception exception) {
            log.error("Stock Explorer CSV import failed; existing directory rows were left unchanged", exception);
        }
    }
}
