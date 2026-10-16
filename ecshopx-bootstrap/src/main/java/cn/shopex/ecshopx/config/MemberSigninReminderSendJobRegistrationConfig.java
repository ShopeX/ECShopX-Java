package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.promotions.dispatch.MemberSigninReminderSendJobHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/** 注册「会员签到漏签提醒」延迟发送任务的消费者（无需 xxl-job 调度）。 */
@Configuration
public class MemberSigninReminderSendJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final MemberSigninReminderSendJobHandler memberSigninReminderSendJobHandler;

	public MemberSigninReminderSendJobRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			MemberSigninReminderSendJobHandler memberSigninReminderSendJobHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.memberSigninReminderSendJobHandler = memberSigninReminderSendJobHandler;
	}

	@PostConstruct
	public void registerMemberSigninReminderSendJob() {
		dispatchRegistry.registerJob(
				PromotionsDispatchJobNames.MEMBER_SIGNIN_REMINDER_SEND, memberSigninReminderSendJobHandler);
	}
}
