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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreateMemberSuccessRegisterPointExecutionService {

	private static final Logger log = LoggerFactory.getLogger(CreateMemberSuccessRegisterPointExecutionService.class);

	private final RegisterPointConfigService registerPointConfigService;
	private final PointMemberAddPointService pointMemberAddPointService;

	public CreateMemberSuccessRegisterPointExecutionService(
			RegisterPointConfigService registerPointConfigService,
			PointMemberAddPointService pointMemberAddPointService) {
		this.registerPointConfigService = registerPointConfigService;
		this.pointMemberAddPointService = pointMemberAddPointService;
	}

	/**
	 * Awards register gift points when Redis register-point config is enabled and {@code point} is positive.
	 */
	public void apply(Map<String, Object> payload) {
		try {
			long companyId = longFrom(payload.get("company_id"));
			long userId = longFrom(payload.get("user_id"));
			if (companyId == 0L || userId == 0L) {
				return;
			}
			Map<String, Object> config = registerPointConfigService.getRegisterPointConfig(companyId, "point");
			if (!isRegisterPointOpen(config)) {
				return;
			}
			int point = positivePoints(config.get("point"));
			if (point <= 0) {
				return;
			}
			pointMemberAddPointService.addPointForMemberRegisterGift(userId, companyId, point);
		} catch (RuntimeException e) {
			log.warn("CreateMemberSuccess register point execution failed", e);
		}
	}

	private static boolean isRegisterPointOpen(Map<String, Object> config) {
		Object raw = config.get("is_open");
		if (Boolean.TRUE.equals(raw)) {
			return true;
		}
		if (raw instanceof String s && "true".equalsIgnoreCase(s.trim())) {
			return true;
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		return false;
	}

	private static int positivePoints(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0 || v > Integer.MAX_VALUE) {
				return 0;
			}
			return (int) v;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0 || v > Integer.MAX_VALUE) {
				return 0;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longFrom(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
