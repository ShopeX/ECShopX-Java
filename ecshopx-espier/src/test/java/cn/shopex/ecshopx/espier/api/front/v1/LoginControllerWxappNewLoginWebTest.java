package cn.shopex.ecshopx.espier.api.front.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import cn.shopex.ecshopx.espier.service.front.FrontWxappNewLoginService;
import cn.shopex.ecshopx.members.service.h5.H5LoginRequestAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP slice: {@link LoginController#login} merges the request then delegates the wxapp new-login flow to
 * {@link FrontWxappNewLoginService#login}; no duplicate async bus wiring on this controller.
 */
class LoginControllerWxappNewLoginWebTest {

	private static final String NEW_LOGIN_PATH = "/api/v1/h5app/wxapp/new_login";

	@Test
	void postWxappNewLogin_delegatesToFrontWxappNewLoginService() throws Exception {
		H5LoginRequestAssembler assembler = mock(H5LoginRequestAssembler.class);
		FrontWxappNewLoginService wxappService = mock(FrontWxappNewLoginService.class);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("auth_type", "wxapp");
		merged.put("appid", "wx-slice-appid");
		when(assembler.merge(any(), any())).thenReturn(merged);

		LinkedHashMap<String, Object> servicePayload = new LinkedHashMap<>();
		servicePayload.put("token", "slice-token");
		servicePayload.put("status", 1);
		when(wxappService.login(any(), any())).thenReturn(servicePayload);

		LoginController controller = new LoginController(assembler, wxappService);
		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver =
				new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		mockMvc
				.perform(
						post(NEW_LOGIN_PATH)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"auth_type\":\"wxapp\",\"appid\":\"wx-slice-appid\"}"))
				.andExpect(status().isOk());

		ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
		verify(assembler).merge(any(), bodyCaptor.capture());
		assertEquals("wxapp", bodyCaptor.getValue().get("auth_type"));

		ArgumentCaptor<Map<String, Object>> loginMergedCaptor = ArgumentCaptor.forClass(Map.class);
		verify(wxappService).login(any(), loginMergedCaptor.capture());
		assertEquals("wxapp", loginMergedCaptor.getValue().get("auth_type"));
		assertEquals("wx-slice-appid", loginMergedCaptor.getValue().get("appid"));
	}
}
