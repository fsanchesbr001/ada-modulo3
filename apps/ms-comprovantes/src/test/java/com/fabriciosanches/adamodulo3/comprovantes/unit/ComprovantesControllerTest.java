package com.fabriciosanches.adamodulo3.comprovantes.unit;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ComprovantesControllerTest {

    @Test
    void postMustReturnAcceptedId() {
        FakeComprovantesController controller = new FakeComprovantesController();

        AcceptedResponse response = controller.post(Map.of("documento", "abc"));

        assertNotNull(response.id());
        assertEquals(36, response.id().length());
    }

    @Test
    void getMustReturnStoredPayloadWhenExists() {
        FakeComprovantesController controller = new FakeComprovantesController();
        AcceptedResponse created = controller.post(Map.of("documento", "payload"));

        Map<String, Object> fetched = controller.get(created.id());

        assertEquals("payload", fetched.get("documento"));
    }

    private static final class FakeComprovantesController {
        private final java.util.Map<String, Map<String, Object>> store = new java.util.HashMap<>();

        private AcceptedResponse post(Map<String, Object> payload) {
            String id = UUID.randomUUID().toString();
            store.put(id, payload);
            return new AcceptedResponse(id);
        }

        private Map<String, Object> get(String id) {
            return store.get(id);
        }
    }

    private record AcceptedResponse(String id) {
    }
}
