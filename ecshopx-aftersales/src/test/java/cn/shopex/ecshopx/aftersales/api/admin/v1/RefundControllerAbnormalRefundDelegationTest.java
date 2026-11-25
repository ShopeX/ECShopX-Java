package cn.shopex.ecshopx.aftersales.api.admin.v1;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesOfflineRefundSaveService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundListService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundLogExportService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundOfflineBankService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RefundControllerAbnormalRefundDelegationTest {

	private static final String PATH = "/api/v1/refund/2202511250001/abnormal-refund";

	@Test
	void abnormalRefund_delegatesToRefundAbnormalTrade() throws Exception {
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefund saved = new AftersalesRefund();
		saved.setRefundBn(2202511250001L);
		saved.setRefundStatus("SUCCESS");
		when(aftersalesRefundService.refundAbnormalTrade(eq(1L), eq(2202511250001L)))
				.thenReturn(saved);

		RefundController controller =
				new RefundController(
						mock(AftersalesOfflineRefundSaveService.class),
						mock(AftersalesRefundListService.class),
						mock(AftersalesRefundLogExportService.class),
						mock(AftersalesRefundOfflineBankService.class),
						aftersalesRefundService);

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		mockMvc
				.perform(
						post(PATH)
								.requestAttr(
										OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA,
										Map.of("company_id", 1))
								.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		verify(aftersalesRefundService).refundAbnormalTrade(1L, 2202511250001L);
		verifyNoMoreInteractions(aftersalesRefundService);
	}
}