package com.finsight.company.application;

import com.finsight.common.error.ApiException;
import com.finsight.company.api.CompanyComparisonItem;
import com.finsight.company.metrics.CompanyFinancialMetricsEntity;
import com.finsight.company.metrics.CompanyFinancialMetricsRepository;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompanyComparisonService {
    private final CompanyRepository companies;
    private final CompanyFinancialMetricsRepository metrics;
    public CompanyComparisonService(CompanyRepository companies,CompanyFinancialMetricsRepository metrics) {
        this.companies=companies; this.metrics=metrics;
    }

    @Transactional(readOnly=true)
    public List<CompanyComparisonItem> compare(String rawIds) {
        List<String> ids=java.util.Arrays.stream(rawIds.split(",")).map(String::trim).distinct().toList();
        if (ids.size()<2 || ids.size()>5 || ids.stream().anyMatch(id -> !id.matches("[A-Fa-f0-9-]{36}"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_COMPARISON","Supply two to five valid company IDs");
        }
        List<CompanyComparisonItem> result=new ArrayList<>();
        for (String id:ids) {
            CompanyEntity company=companies.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"COMPANY_NOT_FOUND","Company was not found"));
            CompanyFinancialMetricsEntity data=metrics.findFirstByCompanyIdOrderByPeriodEndDesc(id).orElse(null);
            if (data==null) {
                result.add(new CompanyComparisonItem(company.getId(),company.getExchangeCode(),company.getSymbol(),
                        company.getCompanyName(),company.getCurrency(),"UNAVAILABLE",null,null,null,
                        null,null,null,null,null,null));
                continue;
            }
            BigDecimal dte=data.getTotalDebt()!=null && data.getTotalEquity()!=null && data.getTotalEquity().signum()>0
                    ? data.getTotalDebt().divide(data.getTotalEquity(),6,RoundingMode.HALF_UP):null;
            BigDecimal roe=data.getNetProfit()!=null && data.getTotalEquity()!=null && data.getTotalEquity().signum()>0
                    ? data.getNetProfit().divide(data.getTotalEquity(),8,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(4,RoundingMode.HALF_UP):null;
            result.add(new CompanyComparisonItem(company.getId(),company.getExchangeCode(),company.getSymbol(),
                    company.getCompanyName(),data.getCurrency(),"AVAILABLE",data.getSource(),data.getFiscalPeriod(),
                    data.getPeriodEnd(),data.getRevenue(),data.getOperatingProfit(),data.getNetProfit(),dte,roe,data.getObservedAt()));
        }
        return result;
    }
}
