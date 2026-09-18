package cn.shopex.ecshopx.members.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.thirdparty.dispatch.CreateMemberSuccessShopexCrmSyncDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.CreateMemberSuccessShopexCrmSyncExecutionService;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmSyncSingleMemberPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * In-memory dispatch: same payload shape as {@link MemberAccountServiceCreatMemberForH5PostDispatchTest}
 * for new H5 members, fan-out to the Shopex CRM listener, then {@link DispatchConsumerRuntime} execution.
 */
class MemberAccountServiceCreatMemberForH5PostShopexCrmSyncDispatchFlowTest {

	@Test
	void h5NewMemberShapedPayload_publishThenConsume_invokesShopexCrmPort() {
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

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("user_id", 902L);
		payload.put("mobile", "13800138002");
		payload.put("openid", "openid-h5");
		payload.put("wxa_appid", "wxa-h5");
		payload.put("source_id", 31L);
		payload.put("monitor_id", 41L);
		payload.put("inviter_id", 51L);
		payload.put("distributor_id", 0L);
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

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		verify(port).syncUpdatedMember(10L, 902L);
	}
}
