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

package cn.shopex.ecshopx.kaquan.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeBatchActiveDelayService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BatchReceiveMemberCardJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(BatchReceiveMemberCardJobHandler.class);

	private final VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService;

	public BatchReceiveMemberCardJobHandler(VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService) {
		this.vipGradeBatchActiveDelayService = vipGradeBatchActiveDelayService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			long companyId = extractCompanyId(payload);
			long vipGradeId = extractVipGradeId(payload);
			int day = extractDay(payload);
			String vipType = extractVipType(payload);
			vipGradeBatchActiveDelayService.runExpiredJobInOneTransaction(companyId, vipGradeId, day, vipType);
		} catch (RuntimeException e) {
			log.debug("BatchReceiveMemberCardJob failed: {}", e.toString());
		}
	}

	private static long extractCompanyId(Map<String, Object> payload) {
		Object raw = payload.get("company_id");
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static long extractVipGradeId(Map<String, Object> payload) {
		Object raw = payload.get("vip_grade_id");
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static int extractDay(Map<String, Object> payload) {
		Object raw = payload.get("day");
		if (raw == null) {
			raw = payload.get("add_day");
		}
		if (raw == null) {
			throw new BadRequestException("day is required");
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private static String extractVipType(Map<String, Object> payload) {
		Object raw = payload.get("vip_type");
		if (raw == null) {
			throw new BadRequestException("vip_type is required");
		}
		return String.valueOf(raw);
	}
}
