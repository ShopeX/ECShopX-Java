package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.common.promotions.port.MemberSigninReminderDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 会员签到漏签提醒延迟任务派发实现：走统一 Dispatch Bus 的 Redis 慢队列，
 * 与 WxopenTemplateSend 同一套基础设施（延迟消息由 delayed zset 到点搬运后消费）。
 */
@Component
public class MemberSigninReminderDispatchPublisherImpl implements MemberSigninReminderDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public MemberSigninReminderDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(long companyId, long userId, long targetEpochSeconds, Duration delay) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("user_id", userId);
		payload.put("target_epoch", targetEpochSeconds);
		dispatchFacade.dispatchJob(
				PromotionsDispatchJobNames.MEMBER_SIGNIN_REMINDER_SEND,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						delay,
						RetryPolicy.platformDefault()));
	}
}
