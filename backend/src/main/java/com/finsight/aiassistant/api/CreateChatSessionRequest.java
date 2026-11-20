package com.finsight.aiassistant.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateChatSessionRequest(@Size(max=200) String title,
        @Pattern(regexp="^$|^[A-Fa-f0-9-]{36}$") String companyId,
        @Pattern(regexp="^$|^[A-Fa-f0-9-]{36}$") String documentId) { }
