package com.fabriciosanches.adamodulo3.faturas.adapter.in.web;

import com.fabriciosanches.adamodulo3.faturas.adapter.in.web.dto.CreateLoteRequest;
import com.fabriciosanches.adamodulo3.faturas.adapter.in.web.dto.CreateLoteResponse;
import com.fabriciosanches.adamodulo3.faturas.adapter.in.web.dto.FaturaResponse;
import com.fabriciosanches.adamodulo3.faturas.application.CreateLoteUseCase;
import com.fabriciosanches.adamodulo3.faturas.application.FaturaNotFoundException;
import com.fabriciosanches.adamodulo3.faturas.application.GetFaturaUseCase;
import com.fabriciosanches.adamodulo3.faturas.application.SolicitarPagamentoUseCase;
import com.fabriciosanches.adamodulo3.faturas.application.model.CreateLoteCommand;
import com.fabriciosanches.adamodulo3.faturas.application.model.GetFaturaResult;
import com.fabriciosanches.adamodulo3.faturas.application.model.SolicitarPagamentoCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/faturas")
public class FaturasController {

    private final CreateLoteUseCase createLoteUseCase;
    private final GetFaturaUseCase getFaturaUseCase;
    private final SolicitarPagamentoUseCase solicitarPagamentoUseCase;

    public FaturasController(
            CreateLoteUseCase createLoteUseCase,
            GetFaturaUseCase getFaturaUseCase,
            SolicitarPagamentoUseCase solicitarPagamentoUseCase) {
        this.createLoteUseCase = createLoteUseCase;
        this.getFaturaUseCase = getFaturaUseCase;
        this.solicitarPagamentoUseCase = solicitarPagamentoUseCase;
    }

    @PostMapping("/lote")
    public ResponseEntity<CreateLoteResponse> createLote(
            @Valid @RequestBody CreateLoteRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        List<CreateLoteCommand.CreateLoteItem> items = request.items().stream()
                .map(item -> new CreateLoteCommand.CreateLoteItem(item.clienteDocumento(), item.valorTotal()))
                .toList();

        CreateLoteCommand command = new CreateLoteCommand(request.loteId(), resolveTraceId(traceId), items);
        List<FaturaResponse> responseItems = createLoteUseCase.execute(command).stream()
                .map(FaturaResponse::from)
                .toList();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new CreateLoteResponse(responseItems));
    }

    @PostMapping("/{id}/pagamentos")
    public ResponseEntity<FaturaResponse> solicitarPagamento(
            @PathVariable("id") UUID faturaId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            Authentication authentication) {
        String subject = authentication != null ? authentication.getName() : "unknown";
        SolicitarPagamentoCommand command = new SolicitarPagamentoCommand(faturaId, resolveTraceId(traceId), subject);

        GetFaturaResult updated = solicitarPagamentoUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(FaturaResponse.from(updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FaturaResponse> getFatura(@PathVariable("id") UUID faturaId) {
        GetFaturaResult result = getFaturaUseCase.execute(faturaId);
        return ResponseEntity.ok(FaturaResponse.from(result));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(FaturaNotFoundException.class)
    public ResponseEntity<Void> handleNotFound(FaturaNotFoundException ignored) {
        return ResponseEntity.notFound().build();
    }

    private String resolveTraceId(String traceIdHeader) {
        if (traceIdHeader != null && !traceIdHeader.isBlank()) {
            return traceIdHeader;
        }
        return UUID.randomUUID().toString();
    }
}
