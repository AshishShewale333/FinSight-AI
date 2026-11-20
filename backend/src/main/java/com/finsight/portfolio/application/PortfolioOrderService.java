package com.finsight.portfolio.application;

import com.finsight.common.error.ApiException;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.marketdata.application.MarketPriceRegistry;
import com.finsight.portfolio.api.OrderResponse;
import com.finsight.portfolio.api.PlaceOrderRequest;
import com.finsight.portfolio.persistence.PortfolioEntity;
import com.finsight.portfolio.persistence.PortfolioHoldingEntity;
import com.finsight.portfolio.persistence.PortfolioHoldingRepository;
import com.finsight.portfolio.persistence.PortfolioOrderEntity;
import com.finsight.portfolio.persistence.PortfolioOrderRepository;
import com.finsight.portfolio.persistence.PortfolioRepository;
import com.finsight.transaction.api.CreateTransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PortfolioOrderService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioOrderService.class);
    private static final BigDecimal ZERO_FEES = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private final PortfolioOrderRepository orders;
    private final PortfolioRepository portfolios;
    private final CompanyRepository companies;
    private final PortfolioHoldingRepository holdings;
    private final PortfolioService portfolioService;
    private final MarketPriceRegistry prices;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public PortfolioOrderService(PortfolioOrderRepository orders, PortfolioRepository portfolios,
            CompanyRepository companies, PortfolioHoldingRepository holdings, PortfolioService portfolioService,
            MarketPriceRegistry prices, JdbcTemplate jdbc, Clock clock) {
        this.orders = orders;
        this.portfolios = portfolios;
        this.companies = companies;
        this.holdings = holdings;
        this.portfolioService = portfolioService;
        this.prices = prices;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse place(String userId, String portfolioId, PlaceOrderRequest request) {
        PortfolioEntity portfolio = portfolios.findOwnedForUpdate(portfolioId, userId)
                .orElseThrow(() -> notFound("PORTFOLIO_NOT_FOUND", "Portfolio was not found"));
        CompanyEntity company = instrument(request.listingId());
        BigDecimal limitPrice = validateLimitPrice(request);

        MarketPriceRegistry.PriceSnapshot price = prices.latest(company.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LIVE_PRICE_UNAVAILABLE",
                        "No WebSocket market price is available for this stock. Choose a stock with a live quote."));
        Instant now = clock.instant();
        PortfolioOrderEntity order = orders.save(new PortfolioOrderEntity(UUID.randomUUID().toString(), portfolio,
                company, request.side(), request.orderType(), request.quantity(), limitPrice, ZERO_FEES, now));

        if (isExecutable(order, price.price())) fill(order, price.price(), now);
        log.info("Order submitted orderId={} portfolioId={} symbol={} side={} type={} status={}",
                order.getId(), portfolioId, company.getSymbol(), order.getSide(), order.getOrderType(), order.getStatus());
        return response(order);
    }

    @Transactional
    public OrderResponse update(String userId, String portfolioId, String orderId, PlaceOrderRequest request) {
        portfolios.findOwnedForUpdate(portfolioId, userId)
                .orElseThrow(() -> notFound("PORTFOLIO_NOT_FOUND", "Portfolio was not found"));
        PortfolioOrderEntity order = orders.findByIdAndPortfolioIdForUpdate(orderId, portfolioId)
                .orElseThrow(() -> notFound("ORDER_NOT_FOUND", "Order was not found"));
        if (!"PENDING".equals(order.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_PENDING",
                    "Only a pending order can be modified");
        }

        CompanyEntity company = instrument(request.listingId());
        BigDecimal limitPrice = validateLimitPrice(request);
        MarketPriceRegistry.PriceSnapshot price = prices.latest(company.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LIVE_PRICE_UNAVAILABLE",
                        "No WebSocket market price is available for this stock. Choose a stock with a live quote."));
        Instant now = clock.instant();
        order.updatePending(company, request.side(), request.orderType(), request.quantity(), limitPrice, now);
        if (isExecutable(order, price.price())) fill(order, price.price(), now);
        log.info("Pending order modified orderId={} portfolioId={} symbol={} side={} type={} status={}",
                order.getId(), portfolioId, company.getSymbol(), order.getSide(), order.getOrderType(), order.getStatus());
        return response(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(String userId, String portfolioId) {
        owned(userId, portfolioId);
        return orders.findByPortfolioIdOrderByCreatedAtDesc(portfolioId).stream().map(this::response).toList();
    }

    @Transactional
    public OrderResponse cancel(String userId, String portfolioId, String orderId) {
        owned(userId, portfolioId);
        PortfolioOrderEntity order = orders.findByIdAndPortfolioIdForUpdate(orderId, portfolioId)
                .orElseThrow(() -> notFound("ORDER_NOT_FOUND", "Order was not found"));
        if (!"PENDING".equals(order.getStatus())) throw new ApiException(HttpStatus.CONFLICT,
                "ORDER_NOT_PENDING", "Only a pending order can be cancelled");
        order.cancel(clock.instant());
        log.info("Pending order cancelled orderId={} portfolioId={} symbol={}", order.getId(), portfolioId,
                order.getCompany().getSymbol());
        return response(order);
    }

    @Transactional
    public void processPriceUpdate(String companyId, BigDecimal currentPrice, Instant observedAt) {
        for (PortfolioOrderEntity order : orders.findPendingForCompanyForUpdate(companyId)) {
            if (!isExecutable(order, currentPrice)) continue;
            fill(order, currentPrice, observedAt);
        }
    }

    private boolean isExecutable(PortfolioOrderEntity order, BigDecimal currentPrice) {
        if ("MARKET".equals(order.getOrderType())) return true;
        return "BUY".equals(order.getSide())
                ? currentPrice.compareTo(order.getLimitPrice()) <= 0
                : currentPrice.compareTo(order.getLimitPrice()) >= 0;
    }

    private CompanyEntity instrument(String listingId) {
        Listing listing = jdbc.query("SELECT exchange_code, symbol FROM stock_explorer_listings " +
                        "WHERE id = ? AND active = TRUE",
                (rs, row) -> new Listing(rs.getString("exchange_code"), rs.getString("symbol")), listingId)
                .stream().findFirst()
                .orElseThrow(() -> notFound("LISTING_NOT_FOUND", "Active Stock Explorer listing was not found"));
        return companies.findByActiveTrueAndSymbolIn(List.of(listing.symbol())).stream()
                .filter(candidate -> candidate.getExchangeCode().equals(listing.exchangeCode()))
                .findFirst().orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "ORDER_INSTRUMENT_UNAVAILABLE", "This NSE listing has no configured portfolio instrument"));
    }

    private BigDecimal validateLimitPrice(PlaceOrderRequest request) {
        BigDecimal limitPrice = request.limitPrice();
        if ("LIMIT".equals(request.orderType()) && (limitPrice == null || limitPrice.signum() <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LIMIT_PRICE_REQUIRED",
                    "A positive limit price is required for a limit order");
        }
        if ("MARKET".equals(request.orderType()) && limitPrice != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MARKET_ORDER_HAS_LIMIT_PRICE",
                    "A market order must not include a limit price");
        }
        return limitPrice;
    }

    private void fill(PortfolioOrderEntity order, BigDecimal executionPrice, Instant at) {
        PortfolioEntity portfolio = portfolios.findOwnedForUpdate(order.getPortfolio().getId(),
                order.getPortfolio().getUser().getId()).orElse(null);
        if (portfolio == null) {
            order.reject("The portfolio was archived before the order could execute", at);
            log.warn("Order rejected orderId={} reason=portfolio_archived", order.getId());
            return;
        }
        if (!order.getCompany().isActive()) {
            order.reject("The stock listing is inactive", at);
            log.warn("Order rejected orderId={} symbol={} reason=inactive_instrument",
                    order.getId(), order.getCompany().getSymbol());
            return;
        }
        if ("SELL".equals(order.getSide())) {
            PortfolioHoldingEntity holding = holdings.findByPortfolioIdAndCompanyIdForUpdate(
                    portfolio.getId(), order.getCompany().getId()).orElse(null);
            if (holding == null || holding.getQuantity().compareTo(order.getQuantity()) < 0) {
                order.reject("Sell quantity exceeds the current portfolio position", at);
                log.warn("Order rejected orderId={} portfolioId={} symbol={} reason=insufficient_holding",
                        order.getId(), portfolio.getId(), order.getCompany().getSymbol());
                return;
            }
        }

        CreateTransactionRequest transaction = new CreateTransactionRequest(order.getCompany().getId(),
                order.getSide(), order.getQuantity(), executionPrice, order.getFees(),
                order.getCompany().getCurrency(), at);
        portfolioService.addTransaction(portfolio.getUser().getId(), portfolio.getId(),
                transaction, "order:" + order.getId());
        order.fill(executionPrice, at);
        log.info("Order filled orderId={} portfolioId={} symbol={} side={} quantity={} price={}",
                order.getId(), portfolio.getId(), order.getCompany().getSymbol(), order.getSide(),
                order.getQuantity(), executionPrice);
    }

    private PortfolioEntity owned(String userId, String portfolioId) {
        return portfolios.findByIdAndUserIdAndArchivedFalse(portfolioId, userId)
                .orElseThrow(() -> notFound("PORTFOLIO_NOT_FOUND", "Portfolio was not found"));
    }

    private OrderResponse response(PortfolioOrderEntity order) {
        return new OrderResponse(order.getId(), order.getCompany().getId(), order.getCompany().getSymbol(),
                order.getSide(), order.getOrderType(), order.getStatus(), order.getQuantity(),
                order.getLimitPrice(), order.getExecutedPrice(), order.getFees(), order.getCompany().getCurrency(),
                order.getRejectionReason(), order.getCreatedAt(), order.getUpdatedAt(), order.getFilledAt());
    }

    private ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    private record Listing(String exchangeCode, String symbol) { }
}
