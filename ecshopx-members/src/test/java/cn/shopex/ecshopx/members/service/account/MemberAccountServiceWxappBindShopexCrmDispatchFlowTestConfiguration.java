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
 * that exercise {@link MemberAccountService#createMemberForWxappBind} without loading bootstrap listener
 * registration (avoids duplicate Shopex CRM listener registration in production configs).
 */
@TestConfiguration
public class MemberAccountServiceWxappBindShopexCrmDispatchFlowTestConfiguration {

	@Bean
	ShopexCrmSyncSingleMemberPort wxappBindShopexCrmSyncSingleMemberPort() {
		return mock(ShopexCrmSyncSingleMemberPort.class);
	}

	@Bean
	CreateMemberSuccessShopexCrmSyncExecutionService wxappBindCreateMemberSuccessShopexCrmSyncExecutionService(
			ShopexCrmSyncSingleMemberPort wxappBindShopexCrmSyncSingleMemberPort) {
		return new CreateMemberSuccessShopexCrmSyncExecutionService(wxappBindShopexCrmSyncSingleMemberPort, "enabled");
	}

	@Bean
	CreateMemberSuccessShopexCrmSyncDispatchListener wxappBindCreateMemberSuccessShopexCrmSyncDispatchListener(
			CreateMemberSuccessShopexCrmSyncExecutionService wxappBindCreateMemberSuccessShopexCrmSyncExecutionService) {
		return new CreateMemberSuccessShopexCrmSyncDispatchListener(wxappBindCreateMemberSuccessShopexCrmSyncExecutionService);
	}

	@Bean
	InMemoryDispatchRegistry wxappBindInMemoryDispatchRegistry(
			CreateMemberSuccessShopexCrmSyncDispatchListener wxappBindCreateMemberSuccessShopexCrmSyncDispatchListener) {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER,
				ListenerDispatchOptions.asyncDefaults(),
				wxappBindCreateMemberSuccessShopexCrmSyncDispatchListener);
		return registry;
	}

	@Bean
	@Qualifier("wxappBindShopexCrmDispatchCapturedMessages")
	List<DispatchMessage> wxappBindShopexCrmDispatchCapturedMessages() {
		return new ArrayList<>();
	}

	@Bean
	DispatchCore wxappBindDispatchCore(
			InMemoryDispatchRegistry wxappBindInMemoryDispatchRegistry,
			@Qualifier("wxappBindShopexCrmDispatchCapturedMessages")
					List<DispatchMessage> wxappBindShopexCrmDispatchCapturedMessages) {
		return DispatchCore.asyncReady(
				wxappBindInMemoryDispatchRegistry,
				new SyncDispatchDriver(wxappBindInMemoryDispatchRegistry),
				Map.of(DispatchDriverType.REDIS, wxappBindShopexCrmDispatchCapturedMessages::add));
	}

	@Bean
	DispatchFanOutPlanner wxappBindDispatchFanOutPlanner(InMemoryDispatchRegistry wxappBindInMemoryDispatchRegistry) {
		return new DispatchFanOutPlanner(wxappBindInMemoryDispatchRegistry);
	}

	@Bean
	DispatchFacade wxappBindDispatchFacade(
			DispatchCore wxappBindDispatchCore, DispatchFanOutPlanner wxappBindDispatchFanOutPlanner) {
		return new DispatchFacade(wxappBindDispatchCore, wxappBindDispatchFanOutPlanner);
	}

	@Bean
	@Primary
	MembersCreateMemberSuccessDispatchPublisher wxappBindMembersCreateMemberSuccessDispatchPublisher(
			DispatchFacade wxappBindDispatchFacade) {
		return payload ->
				wxappBindDispatchFacade.publishEvent(
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
