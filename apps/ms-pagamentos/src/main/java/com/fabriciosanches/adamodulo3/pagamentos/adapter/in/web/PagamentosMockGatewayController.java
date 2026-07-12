package com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web.dto.PagamentosMockGatewayRequest;
import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.web.dto.PagamentosMockGatewayResponse;
import com.fabriciosanches.adamodulo3.pagamentos.application.ProcessPagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.model.PagarEvent;
import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagamentos/mock/gateway")
public class PagamentosMockGatewayController {

    private final ProcessPagamentoUseCase processPagamentoUseCase;

    public PagamentosMockGatewayController(ProcessPagamentoUseCase processPagamentoUseCase) {
        this.processPagamentoUseCase = processPagamentoUseCase;
    }

    @PostMapping("/lote")
    public ResponseEntity<PagamentosMockGatewayResponse> processarLote(@Valid @RequestBody PagamentosMockGatewayRequest request) {
        Pagamento pagamento = processPagamentoUseCase.processPagarEvent(new PagarEvent(
                request.eventId(),
                request.faturaId(),
                request.loteId(),
                request.valorTotal(),
                Instant.now(),
                request.traceId(),
                request.authorizationSubject()));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PagamentosMockGatewayResponse(pagamento.getFaturaId(), pagamento.getStatus().name()));
    }
}
