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
 * Unit-style wiring check: {@link AftersalesController#aftersalesReview} delegates exclusively to
 * {@link AftersalesReviewService#aftersalesReview}; the controller carries no duplicate dispatch-bus or Jushuitan
 * publisher path.
 */
class AftersalesControllerAftersalesReviewDelegationTest {

	private static final String REVIEW_PATH = "/api/v1/aftersales/review";

	@Test
	void aftersalesReview_delegatesToAftersalesReviewServiceOnly() throws Exception {
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
								.param("is_approved", "1")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"aftersales_bn\":42}"))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LinkedHashMap<String, Object>> merged = ArgumentCaptor.forClass(LinkedHashMap.class);
		verify(aftersalesReviewService).aftersalesReview(merged.capture(), any());
		assertThat(merged.getValue().get("is_approved")).isEqualTo("1");
		assertThat(merged.getValue().get("aftersales_bn")).isEqualTo(42);
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
