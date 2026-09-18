package cn.shopex.ecshopx.aftersales.api.admin.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.aftersales.service.AftersalesAdminDetailService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminListService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminLogExportService;
import cn.shopex.ecshopx.aftersales.service.AftersalesApplyService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundConfirmService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRemindService;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.aftersales.service.AftersalesSendbackService;
import cn.shopex.ecshopx.aftersales.service.AftersalesService;
import cn.shopex.ecshopx.aftersales.service.export.AftersalesFinancialExportService;
import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit-style wiring check: {@link AftersalesController#refundCheck} delegates exclusively to
 * {@link AftersalesRefundConfirmService#refundCheck}; the controller carries no duplicate dispatch-bus or third-party
 * publisher path.
 */
class AftersalesControllerRefundCheckDelegationTest {

	private static final String REFUND_CHECK_PATH = "/api/v1/aftersales/refundCheck";

	@Test
	void refundCheck_delegatesToAftersalesRefundConfirmServiceOnly() throws Exception {
		AftersalesApplyService aftersalesApplyService = mock(AftersalesApplyService.class);
		AftersalesRefundConfirmService aftersalesRefundConfirmService = mock(AftersalesRefundConfirmService.class);
		AftersalesReviewService aftersalesReviewService = mock(AftersalesReviewService.class);
		AftersalesRemindService aftersalesRemindService = mock(AftersalesRemindService.class);
		AftersalesSendbackService aftersalesSendbackService = mock(AftersalesSendbackService.class);
		AftersalesService aftersalesService = mock(AftersalesService.class);
		AftersalesAdminListService aftersalesAdminListService = mock(AftersalesAdminListService.class);
		AftersalesAdminDetailService aftersalesAdminDetailService = mock(AftersalesAdminDetailService.class);
		AftersalesFinancialExportService aftersalesFinancialExportService = mock(AftersalesFinancialExportService.class);
		AftersalesAdminLogExportService aftersalesAdminLogExportService = mock(AftersalesAdminLogExportService.class);

		ObjectMapper objectMapper = new ObjectMapper();
		when(aftersalesRefundConfirmService.refundCheck(any(), any()))
				.thenReturn(Map.of("status", true, "result", Map.of()));

		AftersalesController controller =
				new AftersalesController(
						aftersalesApplyService,
						aftersalesRefundConfirmService,
						aftersalesReviewService,
						aftersalesRemindService,
						aftersalesSendbackService,
						aftersalesService,
						aftersalesAdminListService,
						aftersalesAdminDetailService,
						aftersalesFinancialExportService,
						aftersalesAdminLogExportService);

		FlexibleBodyMethodArgumentResolver flexibleResolver = new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		mockMvc
				.perform(
						post(REFUND_CHECK_PATH)
								.param("check_refund", "1")
								.param("aftersales_bn", "202602061111222")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"refund_memo\":\"卖家同意\",\"refund_fee\":100}"))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LinkedHashMap<String, Object>> merged = ArgumentCaptor.forClass(LinkedHashMap.class);
		verify(aftersalesRefundConfirmService).refundCheck(merged.capture(), any());
		assertThat(merged.getValue().get("aftersales_bn")).isEqualTo("202602061111222");
		assertThat(merged.getValue().get("check_refund")).isEqualTo("1");
		assertThat(merged.getValue().get("refund_memo")).isEqualTo("卖家同意");
		assertThat(merged.getValue().get("refund_fee")).isEqualTo(100);
		verifyNoMoreInteractions(
				aftersalesApplyService,
				aftersalesReviewService,
				aftersalesRemindService,
				aftersalesSendbackService,
				aftersalesService,
				aftersalesAdminListService,
				aftersalesAdminDetailService,
				aftersalesFinancialExportService,
				aftersalesAdminLogExportService);
	}
}
