package com.fabriciosanches.adamodulo3.pagamentos.adapter.out.gatewaymock;

public class MockGatewayClient {

    public boolean shouldApprove(String loteId) {
        if (loteId == null || loteId.isBlank()) {
            return false;
        }

        char suffix = loteId.charAt(loteId.length() - 1);
        return suffix != '0' && suffix != '4' && suffix != '9';
    }
}
