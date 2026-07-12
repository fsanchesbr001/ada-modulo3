package com.fabriciosanches.adamodulo3.pagamentos.domain;

public class PagamentoFinalityPolicy {

    public boolean canFinalizeAsPago(Pagamento pagamento) {
        return pagamento.isComprovanteConfirmado();
    }
}
