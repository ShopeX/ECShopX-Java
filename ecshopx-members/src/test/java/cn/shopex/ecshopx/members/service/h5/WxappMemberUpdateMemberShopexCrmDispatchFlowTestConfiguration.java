package cn.shopex.ecshopx.members.service.h5;

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
public class WxappMemberUpdateMemberShopexCrmDispatchFlowTestConfiguration {

	@Bean
	ShopexCrmSyncSingleMemberPort wxappUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort() {
		return mock(ShopexCrmSyncSingleMemberPort.class);
	}

	@Bean
	UpdateMemberSuccessShopexCrmSyncExecutionService wxappUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncExecutionService(
			ShopexCrmSyncSingleMemberPort wxappUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort) {
		return new UpdateMemberSuccessShopexCrmSyncExecutionService(
				wxappUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort, "enabled");
	}

	@Bean
	UpdateMemberSuccessShopexCrmSyncDispatchListener wxappUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncDispatchListener(
			UpdateMemberSuccessShopexCrmSyncExecutionService
					wxappUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncExecutionService) {
		return new UpdateMemberSuccessShopexCrmSyncDispatchListener(
				wxappUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncExecutionService);
	}

	@Bean
	InMemoryDispatchRegistry wxappUpdateMemberShopexCrmFlowInMemoryDispatchRegistry(
			UpdateMemberSuccessShopexCrmSyncDispatchListener
					wxappUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncDispatchListener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_UPDATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_UPDATE_MEMBER,
				ListenerDispatchOptions.asyncDefaults(),
				wxappUpdateMemberShopexCrmFlowUpdateMemberSuccessShopexCrmSyncDispatchListener);
		return registry;
	}

	@Bean
	@Qualifier("wxappUpdateMemberShopexCrmFlowCapturedMessages")
	List<DispatchMessage> wxappUpdateMemberShopexCrmFlowCapturedMessages() {
		return new ArrayList<>();
	}

	@Bean
	DispatchCore wxappUpdateMemberShopexCrmFlowDispatchCore(
			InMemoryDispatchRegistry wxappUpdateMemberShopexCrmFlowInMemoryDispatchRegistry,
			@Qualifier("wxappUpdateMemberShopexCrmFlowCapturedMessages")
					List<DispatchMessage> wxappUpdateMemberShopexCrmFlowCapturedMessages) {
		return DispatchCore.asyncReady(
				wxappUpdateMemberShopexCrmFlowInMemoryDispatchRegistry,
				new SyncDispatchDriver(wxappUpdateMemberShopexCrmFlowInMemoryDispatchRegistry),
				Map.of(DispatchDriverType.REDIS, wxappUpdateMemberShopexCrmFlowCapturedMessages::add));
	}

	@Bean
	DispatchFanOutPlanner wxappUpdateMemberShopexCrmFlowDispatchFanOutPlanner(
			InMemoryDispatchRegistry wxappUpdateMemberShopexCrmFlowInMemoryDispatchRegistry) {
		return new DispatchFanOutPlanner(wxappUpdateMemberShopexCrmFlowInMemoryDispatchRegistry);
	}

	@Bean
	DispatchFacade wxappUpdateMemberShopexCrmFlowDispatchFacade(
			DispatchCore wxappUpdateMemberShopexCrmFlowDispatchCore,
			DispatchFanOutPlanner wxappUpdateMemberShopexCrmFlowDispatchFanOutPlanner) {
		return new DispatchFacade(wxappUpdateMemberShopexCrmFlowDispatchCore, wxappUpdateMemberShopexCrmFlowDispatchFanOutPlanner);
	}

	@Bean
	@Primary
	MembersUpdateMemberSuccessDispatchPublisher wxappUpdateMemberShopexCrmFlowMembersUpdateMemberSuccessDispatchPublisher(
			DispatchFacade wxappUpdateMemberShopexCrmFlowDispatchFacade) {
		return payload ->
				wxappUpdateMemberShopexCrmFlowDispatchFacade.publishEvent(
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
