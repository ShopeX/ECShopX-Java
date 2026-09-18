package cn.shopex.ecshopx.common.port.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LimitedTimeSaleQuotaFenTest {

	@Test
	void unitFen_usesActivityPriceFromOriginalMinusDiscount() {
		assertEquals(100, LimitedTimeSaleQuotaFen.unitFen(800, 1, 700));
		assertEquals(100, LimitedTimeSaleQuotaFen.unitFen(800, 2, 1400));
	}

	@Test
	void unitFen_fallsBackToOriginalWhenNoDiscount() {
		assertEquals(800, LimitedTimeSaleQuotaFen.unitFen(800, 1, 0));
	}
}
