package com.finsight.marketdata.application;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.marketdata.api.MockStockPriceResponse;
import com.finsight.marketdata.domain.MarketQuote;
import com.finsight.marketdata.domain.QuoteStatus;
import com.finsight.marketdata.persistence.StockQuoteEntity;
import com.finsight.marketdata.persistence.StockQuoteRepository;
import com.finsight.notification.application.NotificationService;
import com.finsight.portfolio.application.PortfolioOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "finsight.market-data.provider", havingValue = "mock")
@EnableConfigurationProperties(MockStockProperties.class)
public class MockStockPriceService {
    public static final String PROVIDER = "LOCAL_DEMO_MOCK";
    private static final Logger log = LoggerFactory.getLogger(MockStockPriceService.class);
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal RANDOM_SCALE = new BigDecimal("1000000");

    private final MockStockProperties properties;
    private final CompanyRepository companies;
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;
    private final PortfolioOrderService orders;
    private final MarketPriceRegistry marketPrices;
    private final StockQuoteRepository quotes;
    private final SimpMessagingTemplate websocket;
    private final CacheManager cacheManager;
    private final Clock clock;
    private final Map<String, StockState> stocks = new ConcurrentHashMap<>();
    private Map<String, CompanyEntity> companyBySymbol = Map.of();

    public MockStockPriceService(MockStockProperties properties, CompanyRepository companies, JdbcTemplate jdbc,
            NotificationService notifications, PortfolioOrderService orders, MarketPriceRegistry marketPrices,
            StockQuoteRepository quotes, SimpMessagingTemplate websocket,
            CacheManager cacheManager, Clock clock) {
        this.properties = properties;
        this.companies = companies;
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.orders = orders;
        this.marketPrices = marketPrices;
        this.quotes = quotes;
        this.websocket = websocket;
        this.cacheManager = cacheManager;
        this.clock = clock;
    }

    @PostConstruct
    void initialize() {
        if (properties.getUpdateInterval() < 1 || properties.getHistoryLimit() < 2) {
            throw new IllegalStateException("Mock stock update interval and history limit must be positive");
        }
        if (properties.getMinimumFluctuationPercent().compareTo(new BigDecimal("-100")) <= 0
                || properties.getMaximumFluctuationPercent().compareTo(properties.getMinimumFluctuationPercent()) < 0) {
            throw new IllegalStateException("Mock price fluctuations must be ordered and greater than -100 percent");
        }
        Instant start = clock.instant();
        for (MockStockProperties.Stock stock : properties.getStocks()) {
            String symbol = normalize(stock.getSymbol());
            if (symbol.isBlank() || stock.getInitialPrice() == null || stock.getInitialPrice().signum() <= 0) {
                throw new IllegalStateException("Each mock stock needs a symbol and a positive initial price");
            }
            if (stock.getCurrency() == null || !stock.getCurrency().matches("[A-Z]{3}")) {
                throw new IllegalStateException("Mock stock currency must be a three-letter uppercase code");
            }
            if (stocks.putIfAbsent(symbol, new StockState(symbol, stock.getInitialPrice()
                    .setScale(4, RoundingMode.HALF_UP), stock.getCurrency(), start)) != null) {
                throw new IllegalStateException("Duplicate mock stock symbol: " + symbol);
            }
        }
        stocks.values().forEach(state -> jdbc.update(
                "INSERT INTO companies (id, exchange_code, symbol, company_name, currency, active) "
                        + "VALUES (?, 'NSE', ?, ?, ?, TRUE) ON DUPLICATE KEY UPDATE active=TRUE",
                java.util.UUID.randomUUID().toString(), state.symbol, state.symbol, state.currency));
        companyBySymbol = companies.findByActiveTrueAndSymbolIn(stocks.keySet()).stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        company -> normalize(company.getSymbol()), company -> company));
        if (stocks.isEmpty()) throw new IllegalStateException("Configure at least one mock stock");
        stocks.values().forEach(state -> {
            CompanyEntity company = companyBySymbol.get(state.symbol);
            if (company != null) {
                marketPrices.update(company.getId(), state.symbol, state.price, state.currency, start);
                persistLatestQuote(company, state.price, state.currency, start);
            }
        });
        log.info("Initialized {} local mock stock prices", stocks.size());
    }

    @Scheduled(fixedDelayString = "${finsight.market-data.mock.update-interval:1000}")
    public void updatePrices() {
        stocks.values().stream().sorted(Comparator.comparing(state -> state.symbol))
                .forEach(this::updatePrice);
    }

    private void updatePrice(StockState state) {
        synchronized (state) {
            BigDecimal previous = state.price;
            BigDecimal current = nextPrice(previous);
            Instant observedAt = clock.instant();
            CompanyEntity company = companyBySymbol.get(state.symbol);
            try {
                if (company != null) {
                    notifications.processPriceChange(company.getId(), previous, current,
                            state.currency, QuoteStatus.MOCK.name(), observedAt);
                    try {
                        Cache quotes = cacheManager.getCache("latestQuotes");
                        if (quotes != null) quotes.evict(company.getId());
                    } catch (RuntimeException cacheFailure) {
                        // Cache availability must not stop the in-memory mock market feed.
                        log.debug("Could not evict latest quote cache for symbol={}", state.symbol, cacheFailure);
                    }
                }
                MockStockPriceResponse point = new MockStockPriceResponse(state.symbol, current,
                        state.currency, observedAt, PROVIDER, QuoteStatus.MOCK.name());
                state.append(point, properties.getHistoryLimit());
                if (company != null) {
                    marketPrices.update(company.getId(), state.symbol, current, state.currency, observedAt);
                    persistLatestQuote(company, current, state.currency, observedAt);
                    try {
                        orders.processPriceUpdate(company.getId(), current, observedAt);
                    } catch (RuntimeException orderFailure) {
                        log.warn("Could not process pending orders for mock symbol={}", state.symbol, orderFailure);
                    }
                }
                try {
                    websocket.convertAndSend("/topic/stock-prices", point);
                    websocket.convertAndSend("/topic/stock-prices/" + state.symbol, point);
                } catch (RuntimeException exception) {
                    log.debug("WebSocket broadcast unavailable for mock symbol={}", state.symbol, exception);
                }
            } catch (RuntimeException exception) {
                log.warn("Mock price tick failed symbol={}; keeping previous price", state.symbol, exception);
            }
        }
    }

    private void persistLatestQuote(CompanyEntity company, BigDecimal price, String currency, Instant observedAt) {
        StockQuoteEntity quote = quotes.findFirstByCompanyIdOrderByObservedAtDesc(company.getId()).orElse(null);
        if (quote == null) {
            quote = new StockQuoteEntity(java.util.UUID.randomUUID().toString(), company, price, currency,
                    PROVIDER, QuoteStatus.MOCK, observedAt);
        } else {
            quote.update(price, currency, PROVIDER, QuoteStatus.MOCK, observedAt);
        }
        quotes.save(quote);
    }

    private BigDecimal nextPrice(BigDecimal previous) {
        BigDecimal min = properties.getMinimumFluctuationPercent();
        BigDecimal max = properties.getMaximumFluctuationPercent();
        BigDecimal fraction = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1_000_001))
                .divide(RANDOM_SCALE, 8, RoundingMode.HALF_UP);
        BigDecimal changePercent = min.add(max.subtract(min).multiply(fraction));
        BigDecimal multiplier = ONE.add(changePercent.divide(ONE_HUNDRED, 12, RoundingMode.HALF_UP));
        BigDecimal result = previous.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
        return result.signum() > 0 ? result : new BigDecimal("0.0001");
    }

    public Optional<MarketQuote> latestQuote(String symbol) {
        StockState state = stocks.get(normalize(symbol));
        if (state == null) return Optional.empty();
        synchronized (state) {
            MockStockPriceResponse latest = state.history.peekLast();
            return latest == null ? Optional.empty() : Optional.of(new MarketQuote(
                    Optional.ofNullable(companyBySymbol.get(state.symbol)).map(CompanyEntity::getId).orElse(state.symbol),
                    latest.price(), latest.currency(), PROVIDER, QuoteStatus.MOCK, latest.observedAt()));
        }
    }

    public List<MockStockPriceResponse> latestPrices() {
        return stocks.values().stream().map(state -> {
            synchronized (state) { return state.history.peekLast(); }
        }).filter(java.util.Objects::nonNull).sorted(Comparator.comparing(MockStockPriceResponse::symbol)).toList();
    }

    public List<MockStockPriceResponse> history(String symbol) {
        StockState state = stocks.get(normalize(symbol));
        if (state == null) return List.of();
        synchronized (state) { return List.copyOf(state.history); }
    }

    private String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static final class StockState {
        private final String symbol;
        private final String currency;
        private final ArrayDeque<MockStockPriceResponse> history = new ArrayDeque<>();
        private BigDecimal price;

        private StockState(String symbol, BigDecimal initialPrice, String currency, Instant startedAt) {
            this.symbol = symbol;
            this.price = initialPrice;
            this.currency = currency;
            history.addLast(new MockStockPriceResponse(symbol, initialPrice, currency,
                    startedAt, PROVIDER, QuoteStatus.MOCK.name()));
        }

        private void append(MockStockPriceResponse point, int limit) {
            price = point.price();
            history.addLast(point);
            while (history.size() > limit) history.removeFirst();
        }
    }
}
