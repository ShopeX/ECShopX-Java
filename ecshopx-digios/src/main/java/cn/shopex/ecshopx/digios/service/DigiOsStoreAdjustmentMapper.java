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

package cn.shopex.ecshopx.digios.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 对齐 PHP {@code DigiOsStoreAdjustmentMapper}。 */
public final class DigiOsStoreAdjustmentMapper {

	public static final String DOC_OUTBOUND = "code_000008";
	public static final String DOC_INBOUND = "code_000009";
	public static final int CRT_METHOD = 7;

	private DigiOsStoreAdjustmentMapper() {}

	public static Map<String, Object> buildPayload(
			String nm, String docTypCd, String logWhCd, List<Map<String, Object>> details) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("nm", nm);
		payload.put("docTypCd", docTypCd);
		payload.put("logWhCd", logWhCd);
		payload.put("details", details);
		payload.put("crtMethod", CRT_METHOD);
		payload.put("autoConfirm", true);
		payload.put("autoAudit", true);
		return payload;
	}

	public static List<Map<String, Object>> mapDetails(List<Map<String, Object>> lines, boolean outbound) {
		List<Map<String, Object>> details = new ArrayList<>();
		for (Map<String, Object> line : lines) {
			if (line == null) {
				continue;
			}
			String skuId = stringField(line, "sku_id");
			int qty = intField(line, "qty");
			if (skuId.isEmpty() || qty == 0) {
				continue;
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("skuCd", skuId);
			row.put("adjQty", outbound ? -Math.abs(qty) : Math.abs(qty));
			details.add(row);
		}
		return details;
	}

	public static List<List<Map<String, Object>>> chunkDetails(
			List<Map<String, Object>> details, int maxPerRequest) {
		if (details == null || details.isEmpty()) {
			return List.of();
		}
		int max = Math.max(1, maxPerRequest);
		if (details.size() <= max) {
			return List.of(details);
		}
		List<List<Map<String, Object>>> chunks = new ArrayList<>();
		for (int i = 0; i < details.size(); i += max) {
			chunks.add(details.subList(i, Math.min(i + max, details.size())));
		}
		return chunks;
	}

	private static String stringField(Map<String, Object> row, String key) {
		Object raw = row.get(key);
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static int intField(Map<String, Object> row, String key) {
		Object raw = row.get(key);
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
