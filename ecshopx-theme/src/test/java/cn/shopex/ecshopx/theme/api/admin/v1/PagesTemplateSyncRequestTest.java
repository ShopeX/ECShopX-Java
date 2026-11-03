package cn.shopex.ecshopx.theme.api.admin.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateSyncRequest;
import cn.shopex.ecshopx.theme.service.PagesTemplateAddService;
import cn.shopex.ecshopx.theme.service.PagesTemplateCopyService;
import cn.shopex.ecshopx.theme.service.PagesTemplateDeleteService;
import cn.shopex.ecshopx.theme.service.PagesTemplateDetailService;
import cn.shopex.ecshopx.theme.service.PagesTemplateEditService;
import cn.shopex.ecshopx.theme.service.PagesTemplateListService;
import cn.shopex.ecshopx.theme.service.PagesTemplateModifyStatusService;
import cn.shopex.ecshopx.theme.service.PagesTemplateSyncService;
import cn.shopex.ecshopx.theme.service.PagesTemplateWidgetItemsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PagesTemplateSyncRequestTest {

	@Mock
	private PagesTemplateSyncService pagesTemplateSyncService;

	@Mock
	private HttpServletRequest request;

	@Test
	void deserializesCountryCodeFromRequestBody() throws Exception {
		PagesTemplateSyncRequest request = new ObjectMapper()
				.readValue("{\"pages_template_id\":1,\"country_code\":\"zh-CN\"}", PagesTemplateSyncRequest.class);

		assertThat(request.getPagesTemplateId()).isEqualTo(1L);
		assertThat(request.getCountryCode()).isEqualTo("zh-CN");
	}

	@Test
	void syncPassesBodyCountryCodeToSyncService() {
		PagesTemplateSyncRequest body = new PagesTemplateSyncRequest();
		body.setPagesTemplateId(1L);
		body.setCountryCode("zh-TW");
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(Map.of("company_id", 38));
		when(pagesTemplateSyncService.sync(38L, body, "zh-TW")).thenReturn(Map.of("status", true));

		controller().sync(request, body);

		verify(pagesTemplateSyncService).sync(eq(38L), same(body), eq("zh-TW"));
	}

	private PagesTemplateController controller() {
		return new PagesTemplateController(
				org.mockito.Mockito.mock(PagesTemplateAddService.class),
				org.mockito.Mockito.mock(PagesTemplateCopyService.class),
				org.mockito.Mockito.mock(PagesTemplateDeleteService.class),
				org.mockito.Mockito.mock(PagesTemplateDetailService.class),
				org.mockito.Mockito.mock(PagesTemplateListService.class),
				new ObjectMapper(),
				org.mockito.Mockito.mock(PagesTemplateEditService.class),
				org.mockito.Mockito.mock(PagesTemplateModifyStatusService.class),
				pagesTemplateSyncService,
				new LangueProperties(),
				org.mockito.Mockito.mock(PagesTemplateWidgetItemsService.class));
	}
}
