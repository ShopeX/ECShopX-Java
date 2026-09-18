package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.CreateMemberSuccessShopexCrmSyncDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.CreateMemberSuccessShopexCrmSyncExecutionService;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmSyncSingleMemberPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateMemberSuccessShopexCrmSyncDispatchFlowTest {

	@Test
	void publishEvent_async_enqueuesShopexCrmListener_andConsumerInvokesPort() {
		ShopexCrmSyncSingleMemberPort port = mock(ShopexCrmSyncSingleMemberPort.class);
		CreateMemberSuccessShopexCrmSyncExecutionService executionService =
				new CreateMemberSuccessShopexCrmSyncExecutionService(port, "enabled");
		CreateMemberSuccessShopexCrmSyncDispatchListener listener =
				new CreateMemberSuccessShopexCrmSyncDispatchListener(executionService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);
		payload.put("mobile", "13800138000");
		payload.put("openid", "open-id");
		payload.put("wxa_appid", "");
		payload.put("inviter_id", 0L);
		payload.put("distributor_id", 0L);
		payload.put("source_id", 0L);
		payload.put("monitor_id", 0L);
		payload.put("salesperson_id", 0L);
		payload.put("if_register_promotion", true);

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER, msg.listenerName());
		assertEquals("default", msg.queue());
		assertEquals(7L, msg.payload().get("company_id"));
		assertEquals(99L, msg.payload().get("user_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		verify(port).syncUpdatedMember(7L, 99L);
	}
}
