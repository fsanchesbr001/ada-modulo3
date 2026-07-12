package com.fabriciosanches.adamodulo3.pagamentos.unit;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.out.gatewaymock.MockGatewayClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockGatewayClientTest {

    @Test
    void shouldRefuseLoteEndingWithZeroFourOrNine() {
        MockGatewayClient client = new MockGatewayClient();

        assertFalse(client.shouldApprove("lote-0"));
        assertFalse(client.shouldApprove("lote-4"));
        assertFalse(client.shouldApprove("lote-9"));
    }

    @Test
    void shouldApproveLoteWithOtherSuffix() {
        MockGatewayClient client = new MockGatewayClient();

        assertTrue(client.shouldApprove("lote-1"));
        assertTrue(client.shouldApprove("lote-8"));
    }
}
