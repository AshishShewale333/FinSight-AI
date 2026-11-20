package com.finsight.analytics.application;

import com.finsight.analytics.api.PortfolioAnalyticsResponse;
import com.finsight.analytics.domain.CashFlow;
import com.finsight.analytics.domain.RiskMetricsCalculator;
import com.finsight.analytics.domain.XirrCalculator;
import com.finsight.common.error.ApiException;
import com.finsight.marketdata.persistence.HistoricalStockPriceEntity;
import com.finsight.marketdata.persistence.HistoricalStockPriceRepository;
import com.finsight.marketdata.persistence.StockQuoteEntity;
import com.finsight.marketdata.persistence.StockQuoteRepository;
import com.finsight.portfolio.persistence.PortfolioEntity;
import com.finsight.portfolio.persistence.PortfolioHoldingEntity;
import com.finsight.portfolio.persistence.PortfolioHoldingRepository;
import com.finsight.portfolio.persistence.PortfolioRepository;
import com.finsight.transaction.persistence.PortfolioTransactionEntity;
import com.finsight.transaction.persistence.PortfolioTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioAnalyticsService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private final PortfolioRepository portfolios;
    private final PortfolioHoldingRepository holdings;
    private final PortfolioTransactionRepository transactions;
    private final StockQuoteRepository quotes;
    private final HistoricalStockPriceRepository historicalPrices;
    private final Clock clock;

    public PortfolioAnalyticsService(PortfolioRepository portfolios, PortfolioHoldingRepository holdings,
            PortfolioTransactionRepository transactions, StockQuoteRepository quotes,
            HistoricalStockPriceRepository historicalPrices, Clock clock) {
        this.portfolios=portfolios; this.holdings=holdings; this.transactions=transactions;
        this.quotes=quotes; this.historicalPrices=historicalPrices; this.clock=clock;
    }

    @Transactional(readOnly = true)
    public PortfolioAnalyticsResponse calculate(String userId, String portfolioId) {
        PortfolioEntity portfolio = portfolios.findByIdAndUserIdAndArchivedFalse(portfolioId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", "Portfolio was not found"));
        List<PortfolioHoldingEntity> positions = holdings.findByPortfolioId(portfolio.getId()).stream()
                .filter(position -> position.getQuantity().signum() > 0).toList();
        List<PortfolioTransactionEntity> ledger = transactions.findByPortfolioIdOrderByTradedAtDesc(portfolioId);
        BigDecimal totalInvestment = ZERO;
        BigDecimal realized = ZERO;
        List<CashFlow> cashFlows = new ArrayList<>();
        for (PortfolioTransactionEntity transaction : ledger) {
            realized = realized.add(transaction.getRealizedPl());
            if (transaction.getTransactionType().equals("BUY")) {
                BigDecimal outflow = transaction.getQuantity().multiply(transaction.getUnitPrice()).add(transaction.getFees());
                totalInvestment = totalInvestment.add(outflow);
                cashFlows.add(new CashFlow(transaction.getTradedAt(), outflow.negate()));
            } else if (transaction.getTransactionType().equals("SELL")) {
                BigDecimal inflow = transaction.getQuantity().multiply(transaction.getUnitPrice()).subtract(transaction.getFees());
                cashFlows.add(new CashFlow(transaction.getTradedAt(), inflow));
            }
        }
        BigDecimal currentValue = ZERO;
        int unpriced = 0;
        Map<String, BigDecimal> valuesByAsset = new HashMap<>();
        for (PortfolioHoldingEntity holding : positions) {
            StockQuoteEntity quote = quotes.findFirstByCompanyIdOrderByObservedAtDesc(holding.getCompany().getId()).orElse(null);
            if (quote == null || !quote.getCurrency().equals(portfolio.getBaseCurrency())) {
                unpriced++;
                continue;
            }
            BigDecimal value = quote.getPrice().multiply(holding.getQuantity()).setScale(4, RoundingMode.HALF_UP);
            currentValue = currentValue.add(value);
            valuesByAsset.put(holding.getCompany().getSymbol(), value);
        }
        currentValue = currentValue.setScale(4, RoundingMode.HALF_UP);
        boolean complete = unpriced == 0;
        BigDecimal unrealized = complete ? currentValue.subtract(positions.stream().map(PortfolioHoldingEntity::getCostBasis)
                .reduce(ZERO, BigDecimal::add)).setScale(4, RoundingMode.HALF_UP) : null;
        BigDecimal absolute = complete ? realized.add(unrealized).setScale(4, RoundingMode.HALF_UP) : null;
        BigDecimal percent = complete && totalInvestment.signum() > 0
                ? absolute.divide(totalInvestment, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
                : null;
        if (complete && currentValue.signum() > 0) cashFlows.add(new CashFlow(clock.instant(), currentValue));
        var xirrResult = XirrCalculator.calculate(cashFlows);
        Double xirr = xirrResult.isPresent() ? xirrResult.getAsDouble() * 100 : null;
        Double cagr = calculateCagr(ledger, currentValue, complete);
        BigDecimal totalValue = valuesByAsset.values().stream().reduce(ZERO, BigDecimal::add);
        Map<String, BigDecimal> allocation = new java.util.TreeMap<>();
        BigDecimal hhi = ZERO;
        if (complete && totalValue.signum() > 0) {
            for (var entry : valuesByAsset.entrySet()) {
                BigDecimal weight = entry.getValue().divide(totalValue, 8, RoundingMode.HALF_UP);
                allocation.put(entry.getKey(), weight.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP));
                hhi = hhi.add(weight.multiply(weight));
            }
        }
        Risk risk = riskMetrics(positions);
        return new PortfolioAnalyticsResponse(totalInvestment.setScale(4, RoundingMode.HALF_UP),
                complete ? currentValue : null, realized.setScale(4, RoundingMode.HALF_UP), unrealized,
                absolute, percent, xirr, cagr, allocation,
                complete && totalValue.signum() > 0 ? hhi.setScale(8, RoundingMode.HALF_UP) : null,
                risk.volatility, risk.drawdown, complete, unpriced,
                "Weighted-average cost basis; quote coverage and as-of times are available on holdings. XIRR is annualized; CAGR is reported only for a single buy. Risk metrics use the available daily close series and exclude dividends and corporate actions.");
    }

    private Double calculateCagr(List<PortfolioTransactionEntity> ledger, BigDecimal currentValue, boolean complete) {
        if (!complete || ledger.size() != 1 || !ledger.get(0).getTransactionType().equals("BUY")) return null;
        PortfolioTransactionEntity initial = ledger.get(0);
        BigDecimal invested = initial.getQuantity().multiply(initial.getUnitPrice()).add(initial.getFees());
        if (invested.signum() <= 0 || currentValue.signum() <= 0) return null;
        long days = ChronoUnit.DAYS.between(initial.getTradedAt(), clock.instant());
        if (days <= 0) return null;
        return Math.pow(currentValue.divide(invested, 12, RoundingMode.HALF_UP).doubleValue(), 365.25 / days) - 1.0;
    }

    private Risk riskMetrics(List<PortfolioHoldingEntity> positions) {
        if (positions.size() != 1) return new Risk(null, null);
        PortfolioHoldingEntity holding = positions.get(0);
        Instant to = clock.instant();
        Instant from = to.minus(365, ChronoUnit.DAYS);
        List<HistoricalStockPriceEntity> history = historicalPrices
                .findByCompanyIdAndIntervalCodeAndPriceTimeBetweenOrderByPriceTimeAsc(
                        holding.getCompany().getId(), "1d", from, to);
        List<BigDecimal> closes = history.stream().map(HistoricalStockPriceEntity::getClose).toList();
        var volatility = RiskMetricsCalculator.annualizedVolatility(closes);
        var drawdown = RiskMetricsCalculator.maximumDrawdown(closes);
        return new Risk(volatility.isPresent() ? volatility.getAsDouble() * 100 : null,
                drawdown.isPresent() ? drawdown.getAsDouble() * 100 : null);
    }

    private record Risk(Double volatility, Double drawdown) { }
}
