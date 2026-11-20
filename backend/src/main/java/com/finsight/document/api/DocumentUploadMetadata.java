package com.finsight.document.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentUploadMetadata(@Size(max=36) String companyId,
        @Size(max=16) @Pattern(regexp="^$|^[0-9]{4}(-[0-9]{2})?$|^FY[0-9]{4}-[0-9]{2}$") String financialYear,
        @Pattern(regexp="ANNUAL_REPORT|QUARTERLY_REPORT|EARNINGS_RELEASE|OTHER") String documentType) { }
