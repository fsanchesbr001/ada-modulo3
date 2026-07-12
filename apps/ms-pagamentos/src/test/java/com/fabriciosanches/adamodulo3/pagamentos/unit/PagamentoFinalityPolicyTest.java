package com.fabriciosanches.adamodulo3.pagamentos.unit;

import com.fabriciosanches.adamodulo3.pagamentos.domain.Pagamento;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoFinalityPolicy;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagamentoFinalityPolicyTest {

    @Test
    void canFinalizeAsPagoMustRequireComprovanteConfirmation() {
        PagamentoFinalityPolicy policy = new PagamentoFinalityPolicy();
        Pagamento pagamento = new Pagamento(
                "pg-1",
                "fatura-1",
                "lote-1",
                new BigDecimal("10.00"),
                "trace-1",
                "admin",
                PagamentoStatus.AGUARDANDO_COMPROVANTE,
                0,
                null,
                false,
                Instant.now());

        assertFalse(policy.canFinalizeAsPago(pagamento));

        pagamento.confirmComprovante();
        assertTrue(policy.canFinalizeAsPago(pagamento));
    }
}
