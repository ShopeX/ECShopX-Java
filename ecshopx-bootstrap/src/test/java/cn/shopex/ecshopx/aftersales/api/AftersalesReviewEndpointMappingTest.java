package cn.shopex.ecshopx.aftersales.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.aftersales.api.admin.v1.AftersalesController;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminDetailService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminListService;
import cn.shopex.ecshopx.aftersales.service.AftersalesApplyService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundConfirmService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRemindService;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.aftersales.service.AftersalesSendbackService;
import cn.shopex.ecshopx.aftersales.service.AftersalesService;
import cn.shopex.ecshopx.aftersales.service.export.AftersalesFinancialExportService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminLogExportService;
import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Static wiring check: {@code POST /api/v1/aftersales/review} maps to {@link AftersalesController} and delegates review
 * handling to {@link AftersalesReviewService} (same entry chain as async job scheduling downstream).
 */
class AftersalesReviewEndpointMappingTest {

	private static final String REVIEW_PATH = "/api/v1/aftersales/review";

	@Test
	void post_aftersalesReview_invokesReviewService() throws Exception {
		AftersalesApplyService aftersalesApplyService = Mockito.mock(AftersalesApplyService.class);
		AftersalesRefundConfirmService aftersalesRefundConfirmService = Mockito.mock(AftersalesRefundConfirmService.class);
		AftersalesReviewService aftersalesReviewService = Mockito.mock(AftersalesReviewService.class);
		AftersalesRemindService aftersalesRemindService = Mockito.mock(AftersalesRemindService.class);
		AftersalesSendbackService aftersalesSendbackService = Mockito.mock(AftersalesSendbackService.class);
		AftersalesService aftersalesService = Mockito.mock(AftersalesService.class);
		AftersalesAdminListService aftersalesAdminListService = Mockito.mock(AftersalesAdminListService.class);
		AftersalesAdminDetailService aftersalesAdminDetailService = Mockito.mock(AftersalesAdminDetailService.class);
		AftersalesFinancialExportService aftersalesFinancialExportService = Mockito.mock(AftersalesFinancialExportService.class);
		AftersalesAdminLogExportService aftersalesAdminLogExportService = Mockito.mock(AftersalesAdminLogExportService.class);

		ObjectMapper objectMapper = new ObjectMapper();
		when(aftersalesReviewService.aftersalesReview(any(), any())).thenReturn(Map.of());

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
						post(REVIEW_PATH)
								.param("company_id", "9001")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"aftersales_bn\":202601091200001}"))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LinkedHashMap<String, Object>> merged = ArgumentCaptor.forClass(LinkedHashMap.class);
		verify(aftersalesReviewService).aftersalesReview(merged.capture(), any());
		assertThat(merged.getValue().get("company_id")).isEqualTo("9001");
		assertThat(merged.getValue().get("aftersales_bn")).isEqualTo(202601091200001L);
	}
}
