package com.finsight.marketdata.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "finsight.market-data.mock")
public class MockStockProperties {
    private long updateInterval = 1000;
    private BigDecimal minimumFluctuationPercent = new BigDecimal("-2");
    private BigDecimal maximumFluctuationPercent = new BigDecimal("2");
    private int historyLimit = 300;
    private List<Stock> stocks = new ArrayList<>();

    public long getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(long updateInterval) { this.updateInterval = updateInterval; }
    public BigDecimal getMinimumFluctuationPercent() { return minimumFluctuationPercent; }
    public void setMinimumFluctuationPercent(BigDecimal value) { this.minimumFluctuationPercent = value; }
    public BigDecimal getMaximumFluctuationPercent() { return maximumFluctuationPercent; }
    public void setMaximumFluctuationPercent(BigDecimal value) { this.maximumFluctuationPercent = value; }
    public int getHistoryLimit() { return historyLimit; }
    public void setHistoryLimit(int historyLimit) { this.historyLimit = historyLimit; }
    public List<Stock> getStocks() { return stocks; }
    public void setStocks(List<Stock> stocks) { this.stocks = stocks; }

    public static class Stock {
        private String symbol;
        private BigDecimal initialPrice;
        private String currency = "INR";
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public BigDecimal getInitialPrice() { return initialPrice; }
        public void setInitialPrice(BigDecimal initialPrice) { this.initialPrice = initialPrice; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}
