package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RefundPayloadHelpersTest {

	@Test
	void allocateProportionalLastLineGetsRemainder() {
		Map<Object, Integer> weights = new LinkedHashMap<>();
		weights.put(1L, 100);
		weights.put(2L, 100);
		weights.put(3L, 100);
		Map<Object, Integer> out = RefundLineFeeAllocator.allocateProportional(100, weights);
		assertEquals(33, out.get(1L));
		assertEquals(33, out.get(2L));
		assertEquals(34, out.get(3L));
	}

	@Test
	void itemProductIdPrefersGoodsId() {
		assertEquals("99", ItemProductIdResolver.resolve(99, 11, 22));
		assertEquals("11", ItemProductIdResolver.resolve(0, 11, 22));
		assertEquals("22", ItemProductIdResolver.resolve(0, 0, 22));
	}

	@Test
	void refundStatusMapping() {
		assertEquals("SY_REFUND_SUCC", RefundStatusMapper.mapRefundStatus("SUCCESS"));
		assertEquals("SY_CHECKING", RefundStatusMapper.mapRefundStatus("READY"));
		assertEquals("SY_RETURN_FEE_GOOD", RefundStatusMapper.mapGoodReturnFromAftersalesDetailType("REFUND_GOODS"));
		assertEquals(2, RefundStatusMapper.resolveRefundPhase("DONE", "0"));
		assertEquals(1, RefundStatusMapper.resolveRefundPhase("PAYED", "0"));
	}
}
