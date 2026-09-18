package cn.shopex.ecshopx.aftersales.api.admin.v1;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit-style wiring check: {@link AftersalesController#sendback} delegates exclusively to
 * {@link AftersalesSendbackService#sendback}; the controller carries no dispatch-bus publisher path.
 */
@ExtendWith(MockitoExtension.class)
class AftersalesControllerAdminSendbackDelegationTest {

	private static final String SENDBACK_PATH = "/api/v1/aftersales/sendback";

	@Test
	void postSendback_delegatesToAftersalesSendbackServiceOnce() throws Exception {
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
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("aftersales_bn", 202605101200001L);
		body.put("corp_code", "SF");
		body.put("logi_no", "1234567890");
		when(aftersalesSendbackService.sendback(any(), any())).thenReturn(body);

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
						post(SENDBACK_PATH)
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"{\"aftersales_bn\":202605101200001,\"corp_code\":\"SF\",\"logi_no\":\"1234567890\"}"))
				.andExpect(status().isOk());

		verify(aftersalesSendbackService).sendback(any(), any());
		verifyNoMoreInteractions(
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
	}
}
