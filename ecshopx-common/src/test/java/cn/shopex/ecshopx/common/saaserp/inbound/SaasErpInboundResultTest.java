package cn.shopex.ecshopx.common.saaserp.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SaasErpInboundResultTest {

	@Test
	void okUsesPhpTrueStatus() {
		SaasErpInboundResult result = SaasErpInboundResult.ok("操作成功", null);
		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertNull(result.info());
	}
}
