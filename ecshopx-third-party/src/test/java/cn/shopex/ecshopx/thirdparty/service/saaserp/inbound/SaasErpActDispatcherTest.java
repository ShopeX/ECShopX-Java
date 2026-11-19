package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SaasErpActDispatcherTest {

	@Test
	void targetOfReturnsNullWhenActAbsent() {
		SaasErpActDispatcher dispatcher = new SaasErpActDispatcher(List.of());

		assertNull(dispatcher.targetOf("unknown_act"));
	}

	@Test
	void targetOfReturnsProductionTargetForInvoiceSend() {
		SaasErpActDispatcher dispatcher = new SaasErpActDispatcher(List.of());

		assertEquals(
				"InvoiceCallbackSaasErpAct@handle",
				dispatcher.targetOf("b2c.invoice.send"));
		assertFalse(dispatcher.isBroken("b2c.invoice.send"));
	}

	@Test
	void isBrokenWhenTargetHasEmptyHandlerSide() {
		SaasErpActDispatcher dispatcher =
				new SaasErpActDispatcher(Map.of("x", "n@"), List.of());

		assertEquals("n@", dispatcher.targetOf("x"));
		assertTrue(dispatcher.isBroken("x"));
	}

	@Test
	void findReturnsHandlerWhoseActMatchesRequest() {
		SaasErpActHandler matching =
				new SaasErpActHandler() {
					@Override
					public String act() {
						return "ome_create_delivery";
					}

					@Override
					public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
						return SaasErpInboundResult.ok("ok", null);
					}
				};
		SaasErpActHandler other =
				new SaasErpActHandler() {
					@Override
					public String act() {
						return "update_store";
					}

					@Override
					public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
						return SaasErpInboundResult.ok("ok", null);
					}
				};
		SaasErpActDispatcher dispatcher = new SaasErpActDispatcher(List.of(matching, other));

		Optional<SaasErpActHandler> found = dispatcher.find("ome_create_delivery");

		assertTrue(found.isPresent());
		assertEquals(matching, found.get());
		assertTrue(dispatcher.find("missing").isEmpty());
	}
}
