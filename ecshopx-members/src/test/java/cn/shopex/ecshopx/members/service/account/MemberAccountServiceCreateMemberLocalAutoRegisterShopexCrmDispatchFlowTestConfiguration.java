package cn.shopex.ecshopx.members.service.account;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.thirdparty.dispatch.CreateMemberSuccessShopexCrmSyncDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.CreateMemberSuccessShopexCrmSyncExecutionService;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmSyncSingleMemberPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * In-memory dispatch stack plus a primary {@link MembersCreateMemberSuccessDispatchPublisher} for tests
 * that exercise {@link MemberAccountService#createMemberLocalAutoRegister} without loading bootstrap listener
 * registration (avoids duplicate Shopex CRM listener registration in production configs).
 */
@TestConfiguration
public class MemberAccountServiceCreateMemberLocalAutoRegisterShopexCrmDispatchFlowTestConfiguration {

	@Bean
	ShopexCrmSyncSingleMemberPort localAutoRegisterShopexCrmSyncSingleMemberPort() {
		return mock(ShopexCrmSyncSingleMemberPort.class);
	}

	@Bean
	CreateMemberSuccessShopexCrmSyncExecutionService localAutoRegisterCreateMemberSuccessShopexCrmSyncExecutionService(
			ShopexCrmSyncSingleMemberPort localAutoRegisterShopexCrmSyncSingleMemberPort) {
		return new CreateMemberSuccessShopexCrmSyncExecutionService(
				localAutoRegisterShopexCrmSyncSingleMemberPort, "enabled");
	}

	@Bean
	CreateMemberSuccessShopexCrmSyncDispatchListener localAutoRegisterCreateMemberSuccessShopexCrmSyncDispatchListener(
			CreateMemberSuccessShopexCrmSyncExecutionService localAutoRegisterCreateMemberSuccessShopexCrmSyncExecutionService) {
		return new CreateMemberSuccessShopexCrmSyncDispatchListener(
				localAutoRegisterCreateMemberSuccessShopexCrmSyncExecutionService);
	}

	@Bean
	InMemoryDispatchRegistry localAutoRegisterInMemoryDispatchRegistry(
			CreateMemberSuccessShopexCrmSyncDispatchListener localAutoRegisterCreateMemberSuccessShopexCrmSyncDispatchListener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER,
				ListenerDispatchOptions.asyncDefaults(),
				localAutoRegisterCreateMemberSuccessShopexCrmSyncDispatchListener);
		return registry;
	}

	@Bean
	@Qualifier("localAutoRegisterShopexCrmDispatchCapturedMessages")
	List<DispatchMessage> localAutoRegisterShopexCrmDispatchCapturedMessages() {
		return new ArrayList<>();
	}

	@Bean
	DispatchCore localAutoRegisterDispatchCore(
			InMemoryDispatchRegistry localAutoRegisterInMemoryDispatchRegistry,
			@Qualifier("localAutoRegisterShopexCrmDispatchCapturedMessages")
					List<DispatchMessage> localAutoRegisterShopexCrmDispatchCapturedMessages) {
		return DispatchCore.asyncReady(
				localAutoRegisterInMemoryDispatchRegistry,
				new SyncDispatchDriver(localAutoRegisterInMemoryDispatchRegistry),
				Map.of(DispatchDriverType.REDIS, localAutoRegisterShopexCrmDispatchCapturedMessages::add));
	}

	@Bean
	DispatchFanOutPlanner localAutoRegisterDispatchFanOutPlanner(
			InMemoryDispatchRegistry localAutoRegisterInMemoryDispatchRegistry) {
		return new DispatchFanOutPlanner(localAutoRegisterInMemoryDispatchRegistry);
	}

	@Bean
	DispatchFacade localAutoRegisterDispatchFacade(
			DispatchCore localAutoRegisterDispatchCore,
			DispatchFanOutPlanner localAutoRegisterDispatchFanOutPlanner) {
		return new DispatchFacade(localAutoRegisterDispatchCore, localAutoRegisterDispatchFanOutPlanner);
	}

	@Bean
	@Primary
	MembersCreateMemberSuccessDispatchPublisher localAutoRegisterMembersCreateMemberSuccessDispatchPublisher(
			DispatchFacade localAutoRegisterDispatchFacade) {
		return payload ->
				localAutoRegisterDispatchFacade.publishEvent(
						MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
						payload,
						new DispatchOptions(
								DispatchMode.ASYNC,
								DispatchDriverType.REDIS,
								null,
								null,
								RetryPolicy.platformDefault()));
	}
}
