package com.fabriciosanches.adamodulo3.comprovantes.unit;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ComprovanteFactoryTest {

    @Test
    void factoryMustCreateEntityWithUuidV4LikeId() {
        FakeComprovanteFactory factory = new FakeComprovanteFactory();

        ComprovanteEntity entity = factory.create(Map.of("nome", "arquivo"));

        assertNotNull(entity.id());
        assertEquals(4, UUID.fromString(entity.id()).version());
        assertEquals("arquivo", entity.payload().get("nome"));
    }

    private static final class FakeComprovanteFactory {
        private ComprovanteEntity create(Map<String, Object> payload) {
            return new ComprovanteEntity(UUID.randomUUID().toString(), payload);
        }
    }

    private record ComprovanteEntity(String id, Map<String, Object> payload) {
    }
}
