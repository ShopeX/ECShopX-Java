package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInReminderPushService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 消费「会员签到漏签提醒」延迟消息：校验 + 发送 + 必要时重新武装下一次。 */
@Component
public class MemberSigninReminderSendJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(MemberSigninReminderSendJobHandler.class);

	private final MemberSignInReminderPushService reminderPushService;

	public MemberSigninReminderSendJobHandler(MemberSignInReminderPushService reminderPushService) {
		this.reminderPushService = reminderPushService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"));
		long userId = extractLong(payload.get("user_id"));
		long targetEpoch = extractLong(payload.get("target_epoch"));
		try {
			reminderPushService.handleScheduledPush(companyId, userId, targetEpoch);
		} catch (RuntimeException e) {
			log.warn(
					"[member-signin-reminder] scheduled push failed companyId={} userId={} target={}",
					companyId,
					userId,
					targetEpoch,
					e);
			throw e; // 交给 Dispatch 重试策略（平台默认 3 次）
		}
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
