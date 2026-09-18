package cn.shopex.ecshopx.openapi.thirdapi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpenapiHandlerForwardRegistryTest {

	@Test
	void writeoffIsRegistered() {
		OpenapiHandlerForwardRegistry registry = new OpenapiHandlerForwardRegistry();
		assertTrue(registry.isImplemented("2.0", "POST", "ecx.order.writeoff"));
		assertTrue(
				registry
						.resolveForwardPath("2.0", "POST", "ecx.order.writeoff")
						.filter(p -> p.equals("/api/openapi/internal/v2/ecx.order.writeoff"))
						.isPresent());
	}
}
