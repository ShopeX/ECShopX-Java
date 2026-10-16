package cn.shopex.ecshopx.common.promotions.port;

import java.time.Duration;

/**
 * 派发「会员签到漏签提醒」一次性延迟发送任务。
 *
 * <p>在用户开启提醒 / 重新获得订阅授权时调用：delay 计算到下一次推送时刻（如每日 12:00），
 * 到点后由消费端校验（仍开启、仍有订阅额度、当天未签到）后发送订阅消息；
 * 当天已签到或发送链路配置缺失时由消费端重新武装下一次，保证提醒链不断。
 */
public interface MemberSigninReminderDispatchPublisher {

	/**
	 * @param companyId 公司ID
	 * @param userId 用户ID
	 * @param targetEpochSeconds 本次排定的目标推送时刻（epoch 秒），消费端用于幂等比对
	 * @param delay 距当前时间的延迟（= targetEpochSeconds - now）
	 */
	void publish(long companyId, long userId, long targetEpochSeconds, Duration delay);
}
