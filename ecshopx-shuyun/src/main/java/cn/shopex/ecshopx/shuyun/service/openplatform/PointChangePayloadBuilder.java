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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/** D9：对齐 PHP {@code PointMemberShuyunOpenPlatformPointWriteService::buildChangePayload}。 */
public final class PointChangePayloadBuilder {

	private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private PointChangePayloadBuilder() {}

	public static Map<String, Object> build(
			long userId,
			long companyId,
			int point,
			boolean plus,
			int journalType,
			String record,
			String orderId,
			String shopId,
			Map<String, Object> otherParams) {
		Map<String, Object> extras = otherParams == null ? Map.of() : otherParams;
		int absPoint = Math.abs(point);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("platCode", "OFFLINE");
		body.put("id", String.valueOf(userId));
		body.put("shopId", shopId);
		body.put("sequence", resolveSequence(companyId, userId, journalType, plus, absPoint, orderId, extras));
		body.put("created", DT.format(LocalDateTime.now()));
		body.put("source", resolveSource(journalType, plus));
		body.put("changePoint", plus ? absPoint : -absPoint);
		body.put("operator", resolveOperator(userId, journalType, plus, orderId));
		body.put("desc", StringUtils.hasText(record) ? record : "无记录");
		return body;
	}

	static String resolveSource(int journalType, boolean plus) {
		if (plus) {
			if (journalType == 9 || journalType == 10) {
				return "REFUND";
			}
			if (journalType == 1 || journalType == 2 || journalType == 16) {
				return "MARKET";
			}
			if (journalType == 7) {
				return "TRADE";
			}
			return "OTHER";
		}
		return "CONSUME";
	}

	static String resolveOperator(long userId, int journalType, boolean plus, String orderId) {
		if (!plus && StringUtils.hasText(orderId) && (journalType == 5 || journalType == 6)) {
			return String.valueOf(userId);
		}
		return "system";
	}

	static String resolveSequence(
			long companyId,
			long userId,
			int journalType,
			boolean plus,
			int point,
			String orderId,
			Map<String, Object> otherParams) {
		Object forced = otherParams.get("shuyun_sequence");
		if (forced != null && StringUtils.hasText(String.valueOf(forced))) {
			return String.valueOf(forced).trim();
		}
		String ext = String.valueOf(otherParams.getOrDefault("external_id", ""));
		String pointType = String.valueOf(otherParams.getOrDefault("point_type", ""));
		String raw =
				companyId
						+ "|"
						+ userId
						+ "|"
						+ journalType
						+ "|"
						+ (orderId == null ? "" : orderId)
						+ "|"
						+ ext
						+ "|"
						+ pointType
						+ "|"
						+ (plus ? "add" : "sub")
						+ "|"
						+ point;
		return "sxop_" + sha256Hex(raw).substring(0, 48);
	}

	private static String sha256Hex(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}
}
