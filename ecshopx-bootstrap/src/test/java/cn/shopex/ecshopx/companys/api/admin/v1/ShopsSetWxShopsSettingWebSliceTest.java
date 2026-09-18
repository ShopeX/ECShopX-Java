package cn.shopex.ecshopx.companys.api.admin.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.common.distribution.SelfDistributorDisplayUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingGetService;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingRedisReadService;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingSetService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsCreateService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsDeleteService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsDetailService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsJwtShopIdWhitelist;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsListService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSetDefaultService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSetResourceService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSetShopStatusService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSyncService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@code entry-03-api-shops-setwxshopssetting}
 */
class ShopsSetWxShopsSettingWebSliceTest {

	private static final String JWT_ATTR =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final String SETTING_PATH = "/api/v1/shops/wxshops/setting";

	@Test
	@DisplayName("entry-03-api-shops-setwxshopssetting")
	void putWxShopsSetting_invokesSelfDistributorDisplayUpdatePortThenRedisSaveInOrder() throws Exception {
		SelfDistributorDisplayUpdatePort selfDistributorDisplayUpdatePort = mock(SelfDistributorDisplayUpdatePort.class);
		WxShopsSettingRedisReadService wxShopsSettingRedisReadService = mock(WxShopsSettingRedisReadService.class);
		WxShopsSettingSetService wxShopsSettingSetService =
				new WxShopsSettingSetService(selfDistributorDisplayUpdatePort, wxShopsSettingRedisReadService);

		ObjectMapper objectMapper = new ObjectMapper();
		ShopsController shopsController =
				new ShopsController(
						mock(WxShopsCreateService.class),
						mock(WxShopsUpdateService.class),
						mock(WxShopsSetDefaultService.class),
						mock(WxShopsSetResourceService.class),
						mock(WxShopsSetShopStatusService.class),
						wxShopsSettingSetService,
						mock(WxShopsSettingGetService.class),
						mock(WxShopsListService.class),
						mock(WxShopsDetailService.class),
						mock(WxShopsJwtShopIdWhitelist.class),
						mock(WxShopsSyncService.class),
						mock(WxShopsDeleteService.class),
						objectMapper);

		FlexibleBodyMethodArgumentResolver flexibleResolver = new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(shopsController).setCustomArgumentResolvers(flexibleResolver).build();

		Map<String, Object> jwtMap = new LinkedHashMap<>();
		jwtMap.put("company_id", 88001L);

		String bodyJson =
				"{\"brand_name\":\"FixtureBrand\",\"logo\":\"/fixtures/logo.png\",\"intro\":\"hi\",\"background\":\"/bg.png\"}";

		mockMvc
				.perform(
						put(SETTING_PATH)
								.requestAttr(JWT_ATTR, jwtMap)
								.param("country_code", "en-US")
								.contentType(MediaType.APPLICATION_JSON)
								.content(bodyJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.status").value(true));

		ArgumentCaptor<String> brandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> logoCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> langCaptor = ArgumentCaptor.forClass(String.class);

		InOrder chain = inOrder(selfDistributorDisplayUpdatePort, wxShopsSettingRedisReadService);
		chain
				.verify(selfDistributorDisplayUpdatePort)
				.syncSelfDistributorBrandAndLogoIfPresent(
						eq(88001L), brandCaptor.capture(), logoCaptor.capture(), langCaptor.capture());
		assertEquals("FixtureBrand", brandCaptor.getValue());
		assertEquals("/fixtures/logo.png", logoCaptor.getValue());
		assertEquals("en-US", langCaptor.getValue());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> redisOuterCaptor = ArgumentCaptor.forClass(Map.class);
		chain.verify(wxShopsSettingRedisReadService).saveWxShopsSettingJson(eq(88001L), redisOuterCaptor.capture());

		Map<String, Object> outer = redisOuterCaptor.getValue();
		@SuppressWarnings("unchecked")
		Map<String, Object> inner = (Map<String, Object>) outer.get("en-US");
		assertEquals("/fixtures/logo.png", inner.get("logo"));
		assertEquals("hi", inner.get("intro"));
		assertEquals("FixtureBrand", inner.get("brand_name"));
		assertEquals("/bg.png", inner.get("background"));

		Mockito.verifyNoMoreInteractions(selfDistributorDisplayUpdatePort);
	}
}
