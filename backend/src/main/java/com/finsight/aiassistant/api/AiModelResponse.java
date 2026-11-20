package com.finsight.aiassistant.api;

import java.util.List;

public record AiModelResponse(String answer, List<String> citedChunkIds,
                              List<String> interpretations, List<String> missingInformation) { }
