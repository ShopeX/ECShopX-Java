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

import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.StringUtils;

/** 抽奖 / 结果查询响应组装（PRD §6.5）。 */
public final class TurntableDrawResponseMapper {

	private TurntableDrawResponseMapper() {}

	public static Map<String, Object> fromLog(TurntableLog log, Long remainPoints) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (log == null) {
			return out;
		}
		out.put("activityId", log.getActId());
		out.put("recordId", log.getId());
		out.put("requestId", log.getRequestId());
		String status =
				StringUtils.hasText(log.getStatus()) ? log.getStatus() : TurntableDrawStatus.SUCCESS;
		out.put("status", status);
		out.put("prizeId", log.getPrizeId());
		out.put("sectorIndex", log.getSectorIndex());
		out.put("prizeType", log.getPrizeType());
		out.put("prizeTitle", log.getPrizeTitle());
		out.put("costPoints", log.getCostPoints());
		if (remainPoints != null) {
			out.put("remainPoints", remainPoints);
		}
		boolean isWin =
				TurntableDrawStatus.SUCCESS.equals(status)
						&& StringUtils.hasText(log.getPrizeType())
						&& !"thanks".equalsIgnoreCase(log.getPrizeType());
		out.put("isWin", isWin);
		if (StringUtils.hasText(log.getErrorCode())) {
			out.put("messageCode", log.getErrorCode());
		}
		return out;
	}

	public static Map<String, Object> processing(long activityId, Long recordId, String requestId) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("activityId", activityId);
		out.put("recordId", recordId);
		out.put("requestId", requestId);
		out.put("status", TurntableDrawStatus.PROCESSING);
		return out;
	}

	public static boolean isBlankRequestId(String requestId) {
		return !StringUtils.hasText(requestId == null ? null : requestId.trim());
	}

	public static String normalizeRequestId(String requestId) {
		return requestId == null ? "" : requestId.trim();
	}

	public static boolean isValidRequestId(String requestId) {
		String t = normalizeRequestId(requestId);
		if (!StringUtils.hasText(t)) {
			return false;
		}
		return t.length() <= 64;
	}

	public static String prizeIdOf(Map<String, Object> prize) {
		if (prize == null) {
			return "";
		}
		Object v = prize.containsKey("prize_id") ? prize.get("prize_id") : prize.get("prizeId");
		return v == null ? "" : Objects.toString(v, "").trim();
	}
}
