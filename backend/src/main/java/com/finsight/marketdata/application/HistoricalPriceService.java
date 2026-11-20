package com.finsight.marketdata.application;

import com.finsight.common.error.ApiException;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.marketdata.api.HistoricalPriceResponse;
import com.finsight.marketdata.persistence.HistoricalStockPriceEntity;
import com.finsight.marketdata.persistence.HistoricalStockPriceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class HistoricalPriceService {
    private final CompanyRepository companies;
    private final HistoricalStockPriceRepository prices;
    public HistoricalPriceService(CompanyRepository companies,HistoricalStockPriceRepository prices){this.companies=companies;this.prices=prices;}
    @Transactional(readOnly=true)
    public List<HistoricalPriceResponse> prices(String companyId,Instant from,Instant to){
        if(from==null||to==null||from.isAfter(to)||Duration.between(from,to).compareTo(Duration.ofDays(3650))>0)
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_DATE_RANGE","Date range must be ordered and no longer than ten years");
        if(!companies.existsById(companyId))throw new ApiException(HttpStatus.NOT_FOUND,"COMPANY_NOT_FOUND","Company was not found");
        List<HistoricalStockPriceEntity> rows=prices.findByCompanyIdAndIntervalCodeAndPriceTimeBetweenOrderByPriceTimeAsc(companyId,"1d",from,to);
        if(rows.isEmpty())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"HISTORICAL_DATA_UNAVAILABLE","No historical prices are available from the configured provider");
        return rows.stream().map(row->new HistoricalPriceResponse(row.getPriceTime(),row.getOpen(),row.getHigh(),row.getLow(),row.getClose(),row.getVolume(),row.getCurrency(),row.getProvider())).toList();
    }
}
