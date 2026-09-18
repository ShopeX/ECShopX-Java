package cn.shopex.ecshopx.members.service.admin;

import static org.mockito.Mockito.mock;

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

/**
 * Narrow dispatch stack for admin {@link AdminMemberCreateMemberService#createMember} tests: registers a single
 * in-memory listener for {@link MembersDispatchEventNames#LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER} and a
 * {@link Primary} {@link MembersCreateMemberSuccessDispatchPublisher}, without loading bootstrap listener registration.
 */
@TestConfiguration
public class AdminMemberCreateMemberShopexCrmDispatchFlowTestConfiguration {

	@Bean
	ShopexCrmSyncSingleMemberPort adminCreateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort() {
		return mock(ShopexCrmSyncSingleMemberPort.class);
	}

	@Bean
	CreateMemberSuccessShopexCrmSyncExecutionService adminCreateMemberShopexCrmFlowCreateMemberSuccessShopexCrmSyncExecutionService(
			ShopexCrmSyncSingleMemberPort adminCreateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort) {
		return new CreateMemberSuccessShopexCrmSyncExecutionService(
				adminCreateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort, "enabled");
	}

	@Bean
	CreateMemberSuccessShopexCrmSyncDispatchListener adminCreateMemberShopexCrmFlowCreateMemberSuccessShopexCrmSyncDispatchListener(
			CreateMemberSuccessShopexCrmSyncExecutionService adminCreateMemberShopexCrmFlowCreateMemberSuccessShopexCrmSyncExecutionService) {
		return new CreateMemberSuccessShopexCrmSyncDispatchListener(
				adminCreateMemberShopexCrmFlowCreateMemberSuccessShopexCrmSyncExecutionService);
	}

	@Bean
	InMemoryDispatchRegistry adminCreateMemberShopexCrmFlowInMemoryDispatchRegistry(
			CreateMemberSuccessShopexCrmSyncDispatchListener adminCreateMemberShopexCrmFlowCreateMemberSuccessShopexCrmSyncDispatchListener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER,
				ListenerDispatchOptions.asyncDefaults(),
				adminCreateMemberShopexCrmFlowCreateMemberSuccessShopexCrmSyncDispatchListener);
		return registry;
	}

	@Bean
	@Qualifier("adminCreateMemberShopexCrmFlowCapturedMessages")
	List<DispatchMessage> adminCreateMemberShopexCrmFlowCapturedMessages() {
		return new ArrayList<>();
	}

	@Bean
	DispatchCore adminCreateMemberShopexCrmFlowDispatchCore(
			InMemoryDispatchRegistry adminCreateMemberShopexCrmFlowInMemoryDispatchRegistry,
			@Qualifier("adminCreateMemberShopexCrmFlowCapturedMessages")
					List<DispatchMessage> adminCreateMemberShopexCrmFlowCapturedMessages) {
		return DispatchCore.asyncReady(
				adminCreateMemberShopexCrmFlowInMemoryDispatchRegistry,
				new SyncDispatchDriver(adminCreateMemberShopexCrmFlowInMemoryDispatchRegistry),
				Map.of(DispatchDriverType.REDIS, adminCreateMemberShopexCrmFlowCapturedMessages::add));
	}

	@Bean
	DispatchFanOutPlanner adminCreateMemberShopexCrmFlowDispatchFanOutPlanner(
			InMemoryDispatchRegistry adminCreateMemberShopexCrmFlowInMemoryDispatchRegistry) {
		return new DispatchFanOutPlanner(adminCreateMemberShopexCrmFlowInMemoryDispatchRegistry);
	}

	@Bean
	DispatchFacade adminCreateMemberShopexCrmFlowDispatchFacade(
			DispatchCore adminCreateMemberShopexCrmFlowDispatchCore,
			DispatchFanOutPlanner adminCreateMemberShopexCrmFlowDispatchFanOutPlanner) {
		return new DispatchFacade(adminCreateMemberShopexCrmFlowDispatchCore, adminCreateMemberShopexCrmFlowDispatchFanOutPlanner);
	}

	@Bean
	@Primary
	MembersCreateMemberSuccessDispatchPublisher adminCreateMemberShopexCrmFlowMembersCreateMemberSuccessDispatchPublisher(
			DispatchFacade adminCreateMemberShopexCrmFlowDispatchFacade) {
		return payload ->
				adminCreateMemberShopexCrmFlowDispatchFacade.publishEvent(
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
