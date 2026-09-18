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
import cn.shopex.ecshopx.members.service.admin.AdminMemberBatchOperatingChunkExecutor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BatchActionMembersJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(BatchActionMembersJobHandler.class);

	private final AdminMemberBatchOperatingChunkExecutor chunkExecutor;

	public BatchActionMembersJobHandler(AdminMemberBatchOperatingChunkExecutor chunkExecutor) {
		this.chunkExecutor = chunkExecutor;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"), 0L);
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorParams = (Map<String, Object>) payload.get("params");
		String actionType = payload.get("action_type") == null ? null : String.valueOf(payload.get("action_type"));
		@SuppressWarnings("unchecked")
		Map<String, Object> chunkFilter = (Map<String, Object>) payload.get("filter");
		int page = readInt(payload.get("page"), 1);
		int pageSize = readInt(payload.get("page_size"), 50);
		boolean isQueue = Boolean.TRUE.equals(payload.get("is_queue"));

		if (operatorParams == null || chunkFilter == null) {
			if (isQueue) {
				log.warn("batch action members job skipped: missing params or filter, companyId={}", companyId);
			}
			return;
		}

		if (isQueue) {
			try {
				chunkExecutor.executeChunk(companyId, operatorParams, actionType, chunkFilter, page, pageSize);
			} catch (RuntimeException e) {
				log.error("batch action members chunk failed, companyId={}", companyId, e);
			}
		} else {
			chunkExecutor.executeChunk(companyId, operatorParams, actionType, chunkFilter, page, pageSize);
		}
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

	private static int readInt(Object raw, int defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
