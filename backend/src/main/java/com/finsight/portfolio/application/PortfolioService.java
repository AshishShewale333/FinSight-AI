package com.finsight.portfolio.application;

import com.finsight.common.error.ApiException;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.infrastructure.outbox.OutboxService;
import com.finsight.marketdata.persistence.StockQuoteEntity;
import com.finsight.marketdata.persistence.StockQuoteRepository;
import com.finsight.portfolio.api.CreatePortfolioRequest;
import com.finsight.portfolio.api.HoldingResponse;
import com.finsight.portfolio.api.PortfolioResponse;
import com.finsight.portfolio.persistence.PortfolioEntity;
import com.finsight.portfolio.persistence.PortfolioHoldingEntity;
import com.finsight.portfolio.persistence.PortfolioHoldingRepository;
import com.finsight.portfolio.persistence.PortfolioRepository;
import com.finsight.transaction.api.CreateTransactionRequest;
import com.finsight.transaction.api.TransactionResponse;
import com.finsight.transaction.persistence.PortfolioTransactionEntity;
import com.finsight.transaction.persistence.PortfolioTransactionRepository;
import com.finsight.user.persistence.UserEntity;
import com.finsight.user.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolios;
    private final PortfolioHoldingRepository holdings;
    private final PortfolioTransactionRepository transactions;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final StockQuoteRepository quotes;
    private final OutboxService outbox;
    private final Clock clock;

    public PortfolioService(PortfolioRepository portfolios, PortfolioHoldingRepository holdings,
            PortfolioTransactionRepository transactions, CompanyRepository companies, UserRepository users,
            StockQuoteRepository quotes, OutboxService outbox, Clock clock) {
        this.portfolios=portfolios; this.holdings=holdings; this.transactions=transactions;
        this.companies=companies; this.users=users; this.quotes=quotes; this.outbox=outbox; this.clock=clock;
    }

    @Transactional(readOnly = true)
    public Page<PortfolioResponse> list(String userId, int page, int size) {
        return portfolios.findByUserIdAndArchivedFalse(userId, PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "updatedAt"))).map(this::toResponse);
    }

    @Transactional
    public PortfolioResponse create(String userId, CreatePortfolioRequest request) {
        UserEntity user = users.findById(userId).orElseThrow(() -> notFound("USER_NOT_FOUND", "User was not found"));
        Instant now = clock.instant();
        PortfolioEntity portfolio = portfolios.save(new PortfolioEntity(UUID.randomUUID().toString(), user,
                request.name().trim(), request.baseCurrency() == null ? "INR" : request.baseCurrency(), now));
        return toResponse(portfolio);
    }

    @Transactional(readOnly = true)
    public PortfolioResponse get(String userId, String portfolioId) {
        return toResponse(owned(userId, portfolioId));
    }

    @Transactional
    public void archive(String userId, String portfolioId) {
        PortfolioEntity portfolio = owned(userId, portfolioId);
        portfolio.archive(clock.instant());
    }

    @Transactional
    public TransactionResponse addTransaction(String userId, String portfolioId,
                                              CreateTransactionRequest request, String idempotencyKey) {
        PortfolioEntity portfolio = owned(userId, portfolioId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKey.length() > 100) throw new ApiException(HttpStatus.BAD_REQUEST,
                    "IDEMPOTENCY_KEY_TOO_LONG", "Idempotency key may not exceed 100 characters");
            var previous = transactions.findByPortfolioIdAndIdempotencyKey(portfolioId, idempotencyKey);
            if (previous.isPresent()) return toResponse(previous.get());
        }
        CompanyEntity company = companies.findById(request.companyId())
                .orElseThrow(() -> notFound("COMPANY_NOT_FOUND", "Company was not found"));
        if (!company.isActive()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "COMPANY_INACTIVE", "Transactions cannot be added for an inactive listing");
        if (!company.getCurrency().equals(request.currency() == null ? company.getCurrency() : request.currency())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CURRENCY_MISMATCH",
                    "Transaction currency must match the listed instrument currency");
        }
        Instant now = clock.instant();
        PortfolioHoldingEntity holding = holdings.findByPortfolioIdAndCompanyId(portfolioId, company.getId()).orElse(null);
        BigDecimal realized = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (request.type().equals("BUY")) {
            BigDecimal cost = request.quantity().multiply(request.unitPrice()).add(request.fees()).setScale(4, RoundingMode.HALF_UP);
            if (holding == null) {
                holding = new PortfolioHoldingEntity(UUID.randomUUID().toString(), portfolio, company,
                        request.quantity(), cost.divide(request.quantity(), 4, RoundingMode.HALF_UP), cost, now);
            } else {
                holding.buy(request.quantity(), cost, now);
            }
            holdings.save(holding);
        } else {
            if (holding == null || request.quantity().compareTo(holding.getQuantity()) > 0) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_HOLDING",
                        "Sell quantity exceeds the portfolio position");
            }
            BigDecimal removedCost = holding.sell(request.quantity(), now);
            realized = request.quantity().multiply(request.unitPrice()).subtract(request.fees())
                    .subtract(removedCost).setScale(4, RoundingMode.HALF_UP);
            holdings.save(holding);
        }
        PortfolioTransactionEntity transaction = new PortfolioTransactionEntity(UUID.randomUUID().toString(),
                portfolio, company, request.type(), request.quantity(), request.unitPrice(), request.fees(),
                request.currency() == null ? company.getCurrency() : request.currency(),
                request.tradedAt(), idempotencyKey, realized, now);
        transactions.saveAndFlush(transaction);
        portfolio.touch(now);
        outbox.record("Portfolio", portfolioId, "PortfolioTransactionRecorded",
                java.util.Map.of("portfolioId", portfolioId, "transactionId", transaction.getId(),
                        "companyId", company.getId(), "quantity", request.quantity(), "type", request.type()));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> transactions(String userId, String portfolioId, int page, int size) {
        owned(userId, portfolioId);
        return transactions.findByPortfolioIdOrderByTradedAtDesc(portfolioId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tradedAt"))).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> holdings(String userId, String portfolioId) {
        owned(userId, portfolioId);
        return holdings.findByPortfolioId(portfolioId).stream().map(this::toHoldingResponse).toList();
    }

    private HoldingResponse toHoldingResponse(PortfolioHoldingEntity holding) {
        CompanyEntity company = holding.getCompany();
        StockQuoteEntity quote = quotes.findFirstByCompanyIdOrderByObservedAtDesc(company.getId()).orElse(null);
        BigDecimal currentPrice = quote == null ? null : quote.getPrice();
        BigDecimal marketValue = currentPrice == null ? null : currentPrice.multiply(holding.getQuantity()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal unrealized = marketValue == null ? null : marketValue.subtract(holding.getCostBasis()).setScale(4, RoundingMode.HALF_UP);
        return new HoldingResponse(company.getId(), company.getExchangeCode(), company.getSymbol(),
                company.getCompanyName(), holding.getQuantity(), holding.getAverageCost(), holding.getCostBasis(),
                currentPrice, marketValue, unrealized, quote == null ? "UNAVAILABLE" : quote.getStatus().name(),
                quote == null ? null : quote.getObservedAt());
    }

    private PortfolioEntity owned(String userId, String portfolioId) {
        return portfolios.findByIdAndUserIdAndArchivedFalse(portfolioId, userId)
                .orElseThrow(() -> notFound("PORTFOLIO_NOT_FOUND", "Portfolio was not found"));
    }

    private PortfolioResponse toResponse(PortfolioEntity portfolio) {
        return new PortfolioResponse(portfolio.getId(), portfolio.getName(), portfolio.getBaseCurrency(),
                portfolio.getCreatedAt(), portfolio.getUpdatedAt());
    }

    private TransactionResponse toResponse(PortfolioTransactionEntity transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getCompany().getId(),
                transaction.getCompany().getSymbol(), transaction.getTransactionType(), transaction.getQuantity(),
                transaction.getUnitPrice(), transaction.getFees(), transaction.getCurrency(),
                transaction.getTradedAt(), transaction.getRealizedPl());
    }

    private ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }
}
