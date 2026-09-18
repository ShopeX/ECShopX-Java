/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.promotions.domain.turntable;

import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 抽奖稳定码 {@link TurntableErrorCodes} → i18n 用户提示（PRD §6.6）。
 * API {@code data.message} 返回本地化文案；稳定码写入 log {@code error_code}。
 */
public final class TurntableErrorMessages {

	private static final Map<String, String> I18N_KEYS =
			Map.ofEntries(
					Map.entry(
							TurntableErrorCodes.ACTIVITY_NOT_FOUND,
							"promotions.turntable.activity_not_exist_or_wrong_id"),
					Map.entry(
							TurntableErrorCodes.NOT_STARTED,
							"promotions.turntable.draw_activity_not_started"),
					Map.entry(TurntableErrorCodes.ENDED, "promotions.turntable.draw_activity_ended"),
					Map.entry(
							TurntableErrorCodes.TOTAL_LIMIT,
							"promotions.turntable.draw_times_reach_activity_limit"),
					Map.entry(
							TurntableErrorCodes.DAILY_LIMIT,
							"promotions.turntable.daily_draw_limit_reached"),
					Map.entry(
							TurntableErrorCodes.POINT_NOT_ENOUGH,
							"promotions.turntable.insufficient_user_points"),
					Map.entry(
							TurntableErrorCodes.REQUEST_ID_INVALID,
							"promotions.turntable.request_id_invalid"),
					Map.entry(TurntableErrorCodes.GRANT_FAILED, "promotions.turntable.grant_failed"),
					Map.entry(TurntableErrorCodes.COST_FAILED, "promotions.turntable.cost_failed"),
					Map.entry(
							TurntableErrorCodes.CONFIG_VERSION_CONFLICT,
							"promotions.turntable.config_version_conflict"));

	private static final Map<String, String> ZH_CN_FALLBACKS =
			Map.ofEntries(
					Map.entry(TurntableErrorCodes.ACTIVITY_NOT_FOUND, "错误，活动不存在或id错误"),
					Map.entry(TurntableErrorCodes.NOT_STARTED, "抽奖活动未开始"),
					Map.entry(TurntableErrorCodes.ENDED, "抽奖活动已结束"),
					Map.entry(TurntableErrorCodes.TOTAL_LIMIT, "已达到活动抽奖次数上限"),
					Map.entry(TurntableErrorCodes.DAILY_LIMIT, "已达到每日抽奖次数上限"),
					Map.entry(TurntableErrorCodes.POINT_NOT_ENOUGH, "用户积分不足"),
					Map.entry(TurntableErrorCodes.REQUEST_ID_INVALID, "requestId 缺失或格式不正确"),
					Map.entry(TurntableErrorCodes.GRANT_FAILED, "发奖处理失败，请稍后重试"),
					Map.entry(TurntableErrorCodes.COST_FAILED, "积分扣减失败，请稍后重试"),
					Map.entry(
							TurntableErrorCodes.CONFIG_VERSION_CONFLICT,
							"活动配置已被他人修改，请刷新后重试"));

	private TurntableErrorMessages() {}

	public static String message(MessageSource messageSource, String errorCode) {
		return message(messageSource, errorCode, LocaleContextHolder.getLocale());
	}

	public static String message(MessageSource messageSource, String errorCode, Locale locale) {
		if (messageSource == null || errorCode == null) {
			return errorCode;
		}
		String key = I18N_KEYS.get(errorCode);
		if (key == null) {
			return errorCode;
		}
		Locale effective = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
		String fallback = ZH_CN_FALLBACKS.getOrDefault(errorCode, errorCode);
		return messageSource.getMessage(key, null, fallback, effective);
	}
}
