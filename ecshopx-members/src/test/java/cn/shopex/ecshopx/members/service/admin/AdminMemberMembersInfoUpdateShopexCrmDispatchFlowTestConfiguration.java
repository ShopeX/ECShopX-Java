package cn.shopex.ecshopx.members.service.admin;

import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.MembersUpdateMemberSuccessDispatchPublisher;
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
import cn.shopex.ecshopx.thirdparty.dispatch.UpdateMemberSuccessShopexCrmSyncDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmSyncSingleMemberPort;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.UpdateMemberSuccessShopexCrmSyncExecutionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class AdminMemberMembersInfoUpdateShopexCrmDispatchFlowTestConfiguration {

	@Bean
	ShopexCrmSyncSingleMemberPort adminUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort() {
		return mock(ShopexCrmSyncSingleMemberPort.class);
	}

	@Bean
	UpdateMemberSuccessShopexCrmSyncExecutionService adminUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncExecutionService(
			ShopexCrmSyncSingleMemberPort adminUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort) {
		return new UpdateMemberSuccessShopexCrmSyncExecutionService(
				adminUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort, "enabled");
	}

	@Bean
	UpdateMemberSuccessShopexCrmSyncDispatchListener adminUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncDispatchListener(
			UpdateMemberSuccessShopexCrmSyncExecutionService
					adminUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncExecutionService) {
		return new UpdateMemberSuccessShopexCrmSyncDispatchListener(
				adminUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncExecutionService);
	}

	@Bean
	InMemoryDispatchRegistry adminUpdateMemberShopexCrmFlowInMemoryDispatchRegistry(
			UpdateMemberSuccessShopexCrmSyncDispatchListener
					adminUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncDispatchListener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_UPDATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_UPDATE_MEMBER,
				ListenerDispatchOptions.asyncDefaults(),
				adminUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncDispatchListener);
		return registry;
	}

	@Bean
	@Qualifier("adminUpdateMemberShopexCrmFlowCapturedMessages")
	List<DispatchMessage> adminUpdateMemberShopexCrmFlowCapturedMessages() {
		return new ArrayList<>();
	}

	@Bean
	DispatchCore adminUpdateMemberShopexCrmFlowDispatchCore(
			InMemoryDispatchRegistry adminUpdateMemberShopexCrmFlowInMemoryDispatchRegistry,
			@Qualifier("adminUpdateMemberShopexCrmFlowCapturedMessages")
					List<DispatchMessage> adminUpdateMemberShopexCrmFlowCapturedMessages) {
		return DispatchCore.asyncReady(
				adminUpdateMemberShopexCrmFlowInMemoryDispatchRegistry,
				new SyncDispatchDriver(adminUpdateMemberShopexCrmFlowInMemoryDispatchRegistry),
				Map.of(DispatchDriverType.REDIS, adminUpdateMemberShopexCrmFlowCapturedMessages::add));
	}

	@Bean
	DispatchFanOutPlanner adminUpdateMemberShopexCrmFlowDispatchFanOutPlanner(
			InMemoryDispatchRegistry adminUpdateMemberShopexCrmFlowInMemoryDispatchRegistry) {
		return new DispatchFanOutPlanner(adminUpdateMemberShopexCrmFlowInMemoryDispatchRegistry);
	}

	@Bean
	DispatchFacade adminUpdateMemberShopexCrmFlowDispatchFacade(
			DispatchCore adminUpdateMemberShopexCrmFlowDispatchCore,
			DispatchFanOutPlanner adminUpdateMemberShopexCrmFlowDispatchFanOutPlanner) {
		return new DispatchFacade(adminUpdateMemberShopexCrmFlowDispatchCore, adminUpdateMemberShopexCrmFlowDispatchFanOutPlanner);
	}

	@Bean
	@Primary
	MembersUpdateMemberSuccessDispatchPublisher adminUpdateMemberShopexCrmFlowMembersUpdateMemberSuccessDispatchPublisher(
			DispatchFacade adminUpdateMemberShopexCrmFlowDispatchFacade) {
		return payload ->
				adminUpdateMemberShopexCrmFlowDispatchFacade.publishEvent(
						MembersDispatchEventNames.EVENT_UPDATE_MEMBER_SUCCESS,
						payload,
						new DispatchOptions(
								DispatchMode.ASYNC,
								DispatchDriverType.REDIS,
								null,
								null,
								RetryPolicy.platformDefault()));
	}
}
