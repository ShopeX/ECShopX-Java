package cn.shopex.ecshopx.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class MerchantListParamsTest {

	@Test
	void merchantListAcceptsContactFiltersAndPageSizeAlias() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("page", "1");
		request.setParameter("page_size", "20");
		request.setParameter("contact_name", "张三");
		request.setParameter("contact_mobile", "13800000000");

		Map<String, Object> params =
				new MerchantListRequestParamsResolver(new ObjectMapper()).resolveToMap(request);
		new MerchantListParamValidator().validateFromMap(params);

		assertEquals("张三", params.get("contact_name"));
		assertEquals("13800000000", params.get("contact_mobile"));
		assertFalse(params.containsKey("legal_name"));
		assertEquals(20, MerchantListParamValidator.pageSizeFromValidatedMap(params));
	}

	@Test
	void settlementApplyListKeepsExistingFilters() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("page", "1");
		request.setParameter("page_size", "20");
		request.setParameter("audit_status", "1");
		request.setParameter("merchant_name", "测试商户");
		request.setParameter("settled_type", "enterprise");

		Map<String, Object> params = new MerchantSettlementApplyListRequestParamsResolver().resolve(request);
		MerchantSettlementApplyListQueryInput input =
				new MerchantSettlementApplyListParamValidator().validateAndExtract(params);

		assertEquals("1", input.auditStatus());
		assertEquals("测试商户", input.merchantName());
		assertEquals("enterprise", input.settledType());
	}
}
