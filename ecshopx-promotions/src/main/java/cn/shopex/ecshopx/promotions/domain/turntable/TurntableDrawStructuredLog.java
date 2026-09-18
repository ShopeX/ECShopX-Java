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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 大转盘抽奖结构化日志（BE-05）：仅输出 actId/userId/companyId/requestId/recordId/status/processStep/prizeId/prizeType，
 * 不记录手机号、完整奖品密钥等敏感字段。
 */
public final class TurntableDrawStructuredLog {

	private static final Logger log = LoggerFactory.getLogger(TurntableDrawStructuredLog.class);

	private TurntableDrawStructuredLog() {}

	public static void infoDraw(String event, TurntableLog row) {
		log.info("[turntable-draw] event={} {}", event, format(row, null));
	}

	public static void infoDraw(String event, TurntableLog row, String extra) {
		log.info("[turntable-draw] event={} {} {}", event, format(row, null), safeExtra(extra));
	}

	public static void warnRecover(TurntableLog row, String reason) {
		log.warn("[turntable-draw] event=recover_warn {} reason={}", format(row, null), safeExtra(reason));
	}

	public static void infoRecover(TurntableLog row, String action) {
		log.info("[turntable-draw] event=recover {} action={}", format(row, null), safeExtra(action));
	}

	public static void infoRecoverBatch(int recovered, int scanned, int timeoutSeconds) {
		log.info(
				"[turntable-draw] event=recover_batch recovered={} scanned={} timeoutSeconds={}",
				recovered,
				scanned,
				timeoutSeconds);
	}

	private static String format(TurntableLog row, String prizeValueHint) {
		if (row == null) {
			return "actId= userId= companyId= requestId= recordId= status= processStep= prizeId= prizeType=";
		}
		return "actId="
				+ nullSafe(row.getActId())
				+ " userId="
				+ nullSafe(row.getUserId())
				+ " companyId="
				+ nullSafe(row.getCompanyId())
				+ " requestId="
				+ maskRequestId(row.getRequestId())
				+ " recordId="
				+ nullSafe(row.getId())
				+ " status="
				+ nullSafe(row.getStatus())
				+ " processStep="
				+ nullSafe(row.getProcessStep())
				+ " prizeId="
				+ nullSafe(row.getPrizeId())
				+ " prizeType="
				+ nullSafe(row.getPrizeType())
				+ (StringUtils.hasText(prizeValueHint) ? " prizeValueHint=" + prizeValueHint : "");
	}

	private static String maskRequestId(String requestId) {
		if (!StringUtils.hasText(requestId)) {
			return "";
		}
		String trimmed = requestId.trim();
		if (trimmed.length() <= 8) {
			return "***";
		}
		return trimmed.substring(0, 4) + "***" + trimmed.substring(trimmed.length() - 4);
	}

	private static String safeExtra(String extra) {
		if (!StringUtils.hasText(extra)) {
			return "";
		}
		String masked = DataMasking.maskMobile(extra);
		if (masked.length() > 256) {
			return masked.substring(0, 256);
		}
		return masked;
	}

	private static String nullSafe(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
