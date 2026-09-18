package cn.shopex.ecshopx.thirdparty.service.dm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.kaquan.port.DmCardTemplateMessageNotifyKaquanPort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DmMessageNotifyWebhookServiceCardTemplateModifyTopicTest {

	@Mock
	private DmCardTemplateMessageNotifyKaquanPort kaquanPort;

	@Mock
	private HttpServletRequest request;

	@Test
	@DisplayName("card + cardTemplateModify: normalizes topic before kaquan port and leaves original map unchanged")
	void handle_whenTopicIsCardAndEventIsCardTemplateModify_normalizesTopicAndInvokesKaquanPort() {
		DmMessageNotifyWebhookService svc = new DmMessageNotifyWebhookService(kaquanPort);
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("topic", "card");
		merged.put("event", "cardTemplateModify");
		when(kaquanPort.handle(anyLong(), any(), anyString())).thenReturn(new LinkedHashMap<>(Map.of("card_id", 902L)));

		svc.handle(7L, merged, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(kaquanPort).handle(eq(7L), captor.capture(), anyString());
		assertThat(captor.getValue()).containsEntry("topic", "sync_card_template_modify");
		assertThat(merged).containsEntry("topic", "card");
	}

	@Test
	@DisplayName("SYNC_CARD_TEMPLATE_MODIFY casing: still invokes port with canonical topic")
	void handle_whenTopicIsMixedCaseModify_passesLowercaseCanonicalTopic() {
		DmMessageNotifyWebhookService svc = new DmMessageNotifyWebhookService(kaquanPort);
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("topic", "SyNc_CaRd_TeMpLaTe_MoDiFy");
		when(kaquanPort.handle(anyLong(), any(), anyString())).thenReturn(new LinkedHashMap<>(Map.of("card_id", 1L)));

		svc.handle(2L, merged, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(kaquanPort).handle(eq(2L), captor.capture(), anyString());
		assertThat(captor.getValue()).containsEntry("topic", "sync_card_template_modify");
	}
}
