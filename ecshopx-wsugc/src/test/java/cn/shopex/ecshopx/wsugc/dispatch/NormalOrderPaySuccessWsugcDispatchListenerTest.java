package cn.shopex.ecshopx.wsugc.dispatch;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wsugc.service.WsugcNormalOrderPaySuccessApplyService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalOrderPaySuccessWsugcDispatchListenerTest {

	@Test
	void onEvent_invokesApplyServiceWithCompanyAndOrder() {
		WsugcNormalOrderPaySuccessApplyService apply = mock(WsugcNormalOrderPaySuccessApplyService.class);
		NormalOrderPaySuccessWsugcDispatchListener listener = new NormalOrderPaySuccessWsugcDispatchListener(apply);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);
		payload.put("pay_type", "alipay");

		listener.onEvent(payload);

		verify(apply).applyAfterNormalOrderPaySuccess(7L, 99L);
	}

	@Test
	void onEvent_throwsBadRequestWhenMissingCompanyId() {
		WsugcNormalOrderPaySuccessApplyService apply = mock(WsugcNormalOrderPaySuccessApplyService.class);
		NormalOrderPaySuccessWsugcDispatchListener listener = new NormalOrderPaySuccessWsugcDispatchListener(apply);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(apply);
	}

	@Test
	void onEvent_throwsBadRequestWhenMissingOrderId() {
		WsugcNormalOrderPaySuccessApplyService apply = mock(WsugcNormalOrderPaySuccessApplyService.class);
		NormalOrderPaySuccessWsugcDispatchListener listener = new NormalOrderPaySuccessWsugcDispatchListener(apply);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(apply);
	}
}
