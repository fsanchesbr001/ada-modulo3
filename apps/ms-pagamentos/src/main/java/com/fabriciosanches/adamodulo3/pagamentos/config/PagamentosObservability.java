package com.fabriciosanches.adamodulo3.pagamentos.config;

import com.fabriciosanches.adamodulo3.observability.MdcEnricher;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagamentosObservability {

    private static final Logger LOG = LoggerFactory.getLogger(PagamentosObservability.class);

    private final Counter pagoBlockedTotal;
    private final Counter compensationsTotal;
    private final Counter pagarConsumedTotal;
    private final Counter pagoConfirmedTotal;

    public PagamentosObservability(
            Counter pagoBlockedTotal,
            Counter compensationsTotal,
            Counter pagarConsumedTotal,
            Counter pagoConfirmedTotal) {
        this.pagoBlockedTotal = pagoBlockedTotal;
        this.compensationsTotal = compensationsTotal;
        this.pagarConsumedTotal = pagarConsumedTotal;
        this.pagoConfirmedTotal = pagoConfirmedTotal;
    }

    public void onPagarConsumed(String faturaId, String traceId) {
        pagarConsumedTotal.increment();
        MdcEnricher.put("fatura_id", faturaId);
        MdcEnricher.put("trace_id", traceId);
        LOG.info("{\"event_type\":\"pagar_consumed\",\"fatura_id\":\"{}\",\"trace_id\":\"{}\"}", faturaId, traceId);
        MdcEnricher.clear();
    }

    public void onPagoBlocked(String faturaId, String traceId) {
        pagoBlockedTotal.increment();
        MdcEnricher.put("fatura_id", faturaId);
        MdcEnricher.put("trace_id", traceId);
        LOG.warn("{\"event_type\":\"pagamento_pago_blocked\",\"fatura_id\":\"{}\",\"trace_id\":\"{}\"}", faturaId, traceId);
        MdcEnricher.clear();
    }

    public void onCompensation(String faturaId, String traceId) {
        compensationsTotal.increment();
        MdcEnricher.put("fatura_id", faturaId);
        MdcEnricher.put("trace_id", traceId);
        LOG.info("{\"event_type\":\"pagamento_compensated\",\"fatura_id\":\"{}\",\"trace_id\":\"{}\"}", faturaId, traceId);
        MdcEnricher.clear();
    }

    public void onPagoConfirmed(String faturaId, String traceId) {
        pagoConfirmedTotal.increment();
        MdcEnricher.put("fatura_id", faturaId);
        MdcEnricher.put("trace_id", traceId);
        LOG.info("{\"event_type\":\"pagamento_pago_confirmed\",\"fatura_id\":\"{}\",\"trace_id\":\"{}\"}", faturaId, traceId);
        MdcEnricher.clear();
    }
}
