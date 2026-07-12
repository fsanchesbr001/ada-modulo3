package com.fabriciosanches.adamodulo3.faturas.unit;

import com.fabriciosanches.adamodulo3.faturas.adapter.in.web.FaturasController;
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
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FaturasControllerTest {

    @Test
    void createLoteMustReturnAcceptedAndForwardProvidedTraceHeader() {
        CreateLoteUseCase createLoteUseCase = mock(CreateLoteUseCase.class);
        GetFaturaUseCase getFaturaUseCase = mock(GetFaturaUseCase.class);
        SolicitarPagamentoUseCase solicitarPagamentoUseCase = mock(SolicitarPagamentoUseCase.class);

        FaturasController controller = new FaturasController(createLoteUseCase, getFaturaUseCase, solicitarPagamentoUseCase);

        UUID loteId = UUID.randomUUID();
        GetFaturaResult created = new GetFaturaResult(
                UUID.randomUUID(),
                loteId,
                "12345678901",
                new BigDecimal("150.00"),
                FaturaStatus.PENDENTE,
                0);

        when(createLoteUseCase.execute(any())).thenReturn(List.of(created));

        CreateLoteRequest request = new CreateLoteRequest(
                loteId,
                List.of(new CreateLoteRequest.Item("12345678901", new BigDecimal("150.00"))));

        ResponseEntity<CreateLoteResponse> response = controller.createLote(request, "trace-header-1");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(1, response.getBody().faturas().size());
        assertEquals(created.id(), response.getBody().faturas().get(0).id());

        ArgumentCaptor<CreateLoteCommand> commandCaptor = ArgumentCaptor.forClass(CreateLoteCommand.class);
        verify(createLoteUseCase).execute(commandCaptor.capture());
        assertEquals("trace-header-1", commandCaptor.getValue().traceId());
    }

    @Test
    void createLoteMustGenerateTraceWhenHeaderIsMissing() {
        CreateLoteUseCase createLoteUseCase = mock(CreateLoteUseCase.class);
        FaturasController controller = new FaturasController(createLoteUseCase, mock(GetFaturaUseCase.class), mock(SolicitarPagamentoUseCase.class));

        when(createLoteUseCase.execute(any())).thenReturn(List.of(new GetFaturaResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("20.00"),
                FaturaStatus.PENDENTE,
                0)));

        CreateLoteRequest request = new CreateLoteRequest(
                null,
                List.of(new CreateLoteRequest.Item("12345678901", new BigDecimal("20.00"))));

        controller.createLote(request, null);

        ArgumentCaptor<CreateLoteCommand> commandCaptor = ArgumentCaptor.forClass(CreateLoteCommand.class);
        verify(createLoteUseCase).execute(commandCaptor.capture());
        assertNotNull(commandCaptor.getValue().traceId());
        assertEquals(36, commandCaptor.getValue().traceId().length());
    }

    @Test
    void solicitarPagamentoMustReturnAcceptedAndPropagateSubjectFromAuthentication() {
        SolicitarPagamentoUseCase solicitarPagamentoUseCase = mock(SolicitarPagamentoUseCase.class);
        FaturasController controller = new FaturasController(mock(CreateLoteUseCase.class), mock(GetFaturaUseCase.class), solicitarPagamentoUseCase);

        UUID faturaId = UUID.randomUUID();
        GetFaturaResult updated = new GetFaturaResult(
                faturaId,
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("99.00"),
                FaturaStatus.SOLICITADO,
                0);
        when(solicitarPagamentoUseCase.execute(any())).thenReturn(updated);

        Authentication auth = new TestingAuthenticationToken("gateway-user", "n/a");

        ResponseEntity<FaturaResponse> response = controller.solicitarPagamento(faturaId, "trace-22", auth);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("SOLICITADO", response.getBody().status());

        ArgumentCaptor<SolicitarPagamentoCommand> commandCaptor = ArgumentCaptor.forClass(SolicitarPagamentoCommand.class);
        verify(solicitarPagamentoUseCase).execute(commandCaptor.capture());
        assertEquals("gateway-user", commandCaptor.getValue().authorizationSubject());
        assertEquals("trace-22", commandCaptor.getValue().traceId());
    }

    @Test
    void getFaturaMustReturnOkWhenPresent() {
        GetFaturaUseCase getFaturaUseCase = mock(GetFaturaUseCase.class);
        FaturasController controller = new FaturasController(mock(CreateLoteUseCase.class), getFaturaUseCase, mock(SolicitarPagamentoUseCase.class));

        UUID faturaId = UUID.randomUUID();
        when(getFaturaUseCase.execute(faturaId)).thenReturn(new GetFaturaResult(
                faturaId,
                UUID.randomUUID(),
                "12345678901",
                new BigDecimal("88.00"),
                FaturaStatus.RECUSADO,
                2));

        ResponseEntity<FaturaResponse> response = controller.getFatura(faturaId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RECUSADO", response.getBody().status());
    }

    @Test
    void handleNotFoundMustReturnNotFound() {
        FaturasController controller = new FaturasController(mock(CreateLoteUseCase.class), mock(GetFaturaUseCase.class), mock(SolicitarPagamentoUseCase.class));

        ResponseEntity<Void> response = controller.handleNotFound(new FaturaNotFoundException(UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
