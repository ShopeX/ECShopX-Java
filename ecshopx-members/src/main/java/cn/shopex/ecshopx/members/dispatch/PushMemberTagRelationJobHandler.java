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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.reltag.MemberTagRelationMarketingSyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PushMemberTagRelationJobHandler implements DispatchHandler {

	private static final int BATCH_SIZE = 200;

	private final MemberTagRelationMarketingSyncService memberTagRelationMarketingSyncService;

	public PushMemberTagRelationJobHandler(
			MemberTagRelationMarketingSyncService memberTagRelationMarketingSyncService) {
		this.memberTagRelationMarketingSyncService = memberTagRelationMarketingSyncService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"), 0L);
		String action = trimToEmpty(payload.get("action"));
		List<Map<String, Object>> relations = readRelationsList(payload.get("relations"));
		if (companyId <= 0L || !StringUtils.hasText(action) || relations.isEmpty()) {
			return;
		}
		executeBatches(companyId, action, relations);
	}

	private void executeBatches(long companyId, String action, List<Map<String, Object>> relations) {
		int total = relations.size();
		int batchCount = (total + BATCH_SIZE - 1) / BATCH_SIZE;
		for (int i = 0; i < batchCount; i++) {
			int from = i * BATCH_SIZE;
			int to = Math.min(from + BATCH_SIZE, total);
			List<Map<String, Object>> chunk = relations.subList(from, to);
			memberTagRelationMarketingSyncService.syncRelationBatchesToShoppingGuide(companyId, action, chunk);
			if (batchCount > 1 && i < batchCount - 1) {
				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					ResourceException ex = new ResourceException("interrupted during tag relation batch pause");
					ex.initCause(e);
					throw ex;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> readRelationsList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					out.add((Map<String, Object>) m);
				}
			}
			return out;
		}
		return List.of();
	}

	private static String trimToEmpty(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
	}

	private static long readLong(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
