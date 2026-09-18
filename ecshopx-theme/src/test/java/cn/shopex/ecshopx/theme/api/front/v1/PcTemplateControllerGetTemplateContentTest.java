package cn.shopex.ecshopx.theme.api.front.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.theme.service.PcLoginPageSettingGetService;
import cn.shopex.ecshopx.theme.service.PcTemplateGetDecorationContentService;
import cn.shopex.ecshopx.theme.service.PcTemplateGetHeaderOrFooterService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class PcTemplateControllerGetTemplateContentTest {

	@Mock
	private PcTemplateGetHeaderOrFooterService headerOrFooterService;

	@Mock
	private PcTemplateGetDecorationContentService decorationContentService;

	@Mock
	private PcLoginPageSettingGetService loginPageSettingGetService;

	private PcTemplateController controller;

	@BeforeEach
	void setUp() {
		LocaleContextHolder.resetLocaleContext();
		controller =
				new PcTemplateController(
						headerOrFooterService,
						decorationContentService,
						new LangueProperties(),
						loginPageSettingGetService);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	@DisplayName("page_type=home 走 decorationContent 且不 seed")
	void homeDelegatesToDecorationContentWithoutSeed() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", "2");
		payload.put("name", "");
		payload.put("config", "{\"type\":\"ECX_SP_WEB_DECORATION_DSL_V1\"}");
		when(decorationContentService.getDecorationContent(
						eq(141L), anyString(), isNull(), eq("home"), isNull(), eq(0L), eq(false)))
				.thenReturn(payload);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("company_id", "141");
		ApiResult<Map<String, Object>> result =
				controller.getTemplateContent(request, null, "home", null, null);

		assertThat(result.getData()).isEqualTo(payload);
		verify(decorationContentService)
				.getDecorationContent(141L, "zh-CN", null, "home", null, 0L, false);
	}

	@Test
	@DisplayName("缺省 page_type 视为 home")
	void blankPageTypeDefaultsToHome() {
		when(decorationContentService.getDecorationContent(
						anyLong(), anyString(), isNull(), eq("home"), isNull(), eq(0L), anyBoolean()))
				.thenReturn(Map.of("id", 0, "name", "", "config", ""));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("company_id", "141");
		controller.getTemplateContent(request, null, null, null, null);

		verify(decorationContentService)
				.getDecorationContent(141L, "zh-CN", null, "home", null, 0L, false);
	}

	@Test
	@DisplayName("page_type=index 按 PHP open 校验拒绝")
	void indexPageTypeRejected() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("company_id", "141");
		assertThatThrownBy(() -> controller.getTemplateContent(request, null, "index", null, null))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("缺少或错误的page_type");
	}

	@Test
	@DisplayName("custom 缺少 page_id 拒绝")
	void customRequiresPageId() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("company_id", "141");
		assertThatThrownBy(() -> controller.getTemplateContent(request, null, "custom", null, null))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("缺少page_id");
	}
}
