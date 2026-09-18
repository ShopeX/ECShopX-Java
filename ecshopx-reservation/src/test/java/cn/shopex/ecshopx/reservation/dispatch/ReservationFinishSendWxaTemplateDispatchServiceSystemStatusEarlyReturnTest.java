package cn.shopex.ecshopx.reservation.dispatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.port.reservation.ReservationFinishSuccWxaTemplatePort;
import cn.shopex.ecshopx.common.port.reservation.ReservationUserWxappIdentityResolvePort;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReservationFinishSendWxaTemplateDispatchServiceSystemStatusEarlyReturnTest {

	@Test
	void handle_whenMergedParamsHaveStatusSystem_doesNotInvokeOutboundPorts() {
		ReservationShopDetailPort shopPort = mock(ReservationShopDetailPort.class);
		ReservationFinishSuccWxaTemplatePort templatePort = mock(ReservationFinishSuccWxaTemplatePort.class);
		ReservationUserWxappIdentityResolvePort resolvePort = mock(ReservationUserWxappIdentityResolvePort.class);

		ReservationFinishSendWxaTemplateDispatchService service =
				new ReservationFinishSendWxaTemplateDispatchService(shopPort, templatePort, resolvePort);

		Map<String, Object> postdata = new LinkedHashMap<>();
		postdata.put("user_id", 42L);
		postdata.put("company_id", 99L);
		postdata.put("status", "success");

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("record_id", 1L);
		result.put("company_id", 99L);
		result.put("status", "system");

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("postdata", postdata);
		entities.put("result", result);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		service.handle(payload);

		verifyNoInteractions(shopPort, templatePort, resolvePort);
	}
}
