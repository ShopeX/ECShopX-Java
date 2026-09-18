package cn.shopex.ecshopx.thirdparty.api.thirdapi.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.common.kaquan.port.DmCardTemplateMessageNotifyKaquanPort;
import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import cn.shopex.ecshopx.thirdparty.service.dm.DmMessageNotifyWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DmMessageNotifyControllerMvcTest {

	@Test
	@DisplayName("POST /third/dm/messageNotify/{companyId}: forwards path companyId to webhook service")
	void post_messageNotify_jsonBody_forwardsPathCompanyIdToWebhookService() throws Exception {
		DmMessageNotifyWebhookService webhookService = org.mockito.Mockito.mock(DmMessageNotifyWebhookService.class);
		when(webhookService.handle(any(Long.class), any(), any(HttpServletRequest.class)))
				.thenReturn(new LinkedHashMap<>(Map.of("status", true)));

		DmMessageNotifyController controller = new DmMessageNotifyController(webhookService);
		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver = new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		mockMvc
				.perform(
						post("/api/v1/third/dm/messageNotify/{companyId}", 7L)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"topic\":\"sync_card_template_create\"}"))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> merged = ArgumentCaptor.forClass(Map.class);
		verify(webhookService).handle(eq(7L), merged.capture(), any(HttpServletRequest.class));
		assertThat(merged.getValue()).containsEntry("topic", "sync_card_template_create");
	}

	@Test
	@DisplayName("POST /third/dm/messageNotify/{companyId}: card + cardTemplateModify reaches port with normalized topic")
	void post_messageNotify_cardTemplateModifyCompositeTopic_forwardsNormalizedInputToWebhookService() throws Exception {
		DmCardTemplateMessageNotifyKaquanPort port = Mockito.mock(DmCardTemplateMessageNotifyKaquanPort.class);
		when(port.handle(any(Long.class), any(), anyString())).thenReturn(new LinkedHashMap<>(Map.of("card_id", 99L)));

		DmMessageNotifyWebhookService webhookService = new DmMessageNotifyWebhookService(port);
		DmMessageNotifyController controller = new DmMessageNotifyController(webhookService);
		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver = new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		mockMvc
				.perform(
						post("/api/v1/third/dm/messageNotify/{companyId}", 7L)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"topic\":\"card\",\"event\":\"cardTemplateModify\"}"))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> merged = ArgumentCaptor.forClass(Map.class);
		verify(port).handle(eq(7L), merged.capture(), anyString());
		assertThat(merged.getValue()).containsEntry("topic", "sync_card_template_modify");
	}

	@Test
	@DisplayName("POST /third/dm/messageNotify/{companyId}: card + cardTemplateDelete reaches port with normalized topic")
	void post_messageNotify_cardTemplateDeleteCompositeTopic_forwardsNormalizedInputToWebhookService() throws Exception {
		DmCardTemplateMessageNotifyKaquanPort port = Mockito.mock(DmCardTemplateMessageNotifyKaquanPort.class);
		when(port.handle(any(Long.class), any(), anyString())).thenReturn(new LinkedHashMap<>(Map.of("card_id", 55L)));

		DmMessageNotifyWebhookService webhookService = new DmMessageNotifyWebhookService(port);
		DmMessageNotifyController controller = new DmMessageNotifyController(webhookService);
		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver = new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		mockMvc
				.perform(
						post("/api/v1/third/dm/messageNotify/{companyId}", 7L)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"topic\":\"card\",\"event\":\"cardTemplateDelete\"}"))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> merged = ArgumentCaptor.forClass(Map.class);
		verify(port).handle(eq(7L), merged.capture(), anyString());
		assertThat(merged.getValue()).containsEntry("topic", "sync_card_template_delete");
	}
}
