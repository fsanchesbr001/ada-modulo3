package com.fabriciosanches.adamodulo3.comprovantes.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ComprovanteCreateRequest(@NotNull Map<String, Object> payloadPdfJson) {
}
