package com.fabriciosanches.adamodulo3.comprovantes.adapter.in.web.dto;

import com.fabriciosanches.adamodulo3.comprovantes.domain.Comprovante;
import java.util.Map;

public record ComprovanteResponse(String id, Map<String, Object> payloadPdfJson) {

    public static ComprovanteResponse from(Comprovante comprovante) {
        return new ComprovanteResponse(comprovante.getId(), comprovante.getPayloadPdfJson());
    }
}
