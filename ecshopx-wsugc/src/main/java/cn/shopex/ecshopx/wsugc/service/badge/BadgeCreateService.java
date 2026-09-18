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

package cn.shopex.ecshopx.wsugc.service.badge;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Badge;
import cn.shopex.ecshopx.wsugc.mapper.BadgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BadgeCreateService {

	private final BadgeMapper badgeMapper;

	public BadgeCreateService(BadgeMapper badgeMapper) {
		this.badgeMapper = badgeMapper;
	}

	public Map<String, Object> createOrUpdate(Map<String, Object> input, Map<String, Object> operatorJwt) {
		Long requestBadgeId = parseOptionalBadgeId(input);
		String badgeName = requireBadgeName(input);
		String badgeMemo = parseBadgeMemo(input);

		long operatorId = readLong(operatorJwt.get("operator_id"), 0L);
		String mobile = readString(operatorJwt.get("mobile"), "");
		long companyId = readLong(operatorJwt.get("company_id"), 1L);

		LambdaQueryWrapper<Badge> w = new LambdaQueryWrapper<>();
		w.eq(Badge::getBadgeName, badgeName);
		List<Badge> existing = badgeMapper.selectList(w);
		if (!existing.isEmpty()) {
			Badge first = existing.get(0);
			if (requestBadgeId == null || !first.getBadgeId().equals(requestBadgeId)) {
				throw new ResourceException("同名角标已存在");
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000);

		if (requestBadgeId == null) {
			Badge entity = new Badge();
			entity.setBadgeName(badgeName);
			entity.setBadgeMemo(badgeMemo);
			entity.setOperatorId(operatorId);
			entity.setUserId(0L);
			entity.setMobile(mobile);
			entity.setCompanyId(companyId);
			entity.setPOrder(0);
			entity.setSource(2);
			entity.setIsTop(0);
			entity.setStatus(1);
			entity.setEnabled(1);
			entity.setCreated(now);
			entity.setUpdated(now);
			badgeMapper.insert(entity);
			return toResultMap(entity, "创建角标成功");
		}

		Badge loaded = badgeMapper.selectById(requestBadgeId);
		if (loaded == null) {
			throw new ResourceException("未查询到更新数据");
		}
		loaded.setBadgeName(badgeName);
		loaded.setBadgeMemo(badgeMemo);
		loaded.setOperatorId(operatorId);
		loaded.setUserId(0L);
		loaded.setMobile(mobile);
		loaded.setCompanyId(companyId);
		loaded.setPOrder(0);
		loaded.setSource(2);
		loaded.setIsTop(0);
		loaded.setStatus(1);
		loaded.setUpdated(now);
		badgeMapper.updateById(loaded);

		return toResultMap(loaded, "更新角标成功");
	}

	private static Long parseOptionalBadgeId(Map<String, Object> input) {
		if (!input.containsKey("badge_id")) {
			return null;
		}
		Object v = input.get("badge_id");
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("badge_id 格式无效");
		}
	}

	private static String requireBadgeName(Map<String, Object> input) {
		Object v = input.get("badge_name");
		if (v == null) {
			throw new ResourceException("角标名称不能为空");
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			throw new ResourceException("角标名称不能为空");
		}
		return s;
	}

	private static String parseBadgeMemo(Map<String, Object> input) {
		if (!input.containsKey("badge_memo")) {
			return null;
		}
		Object v = input.get("badge_memo");
		if (v == null) {
			return null;
		}
		return v.toString();
	}

	private static long readLong(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String readString(Object v, String defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		return v.toString();
	}

	private static LinkedHashMap<String, Object> toResultMap(Badge e, String message) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("badge_id", e.getBadgeId());
		m.put("badge_name", e.getBadgeName());
		m.put("badge_memo", e.getBadgeMemo());
		m.put("created", e.getCreated());
		m.put("p_order", e.getPOrder());
		m.put("is_top", e.getIsTop());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		m.put("company_id", e.getCompanyId());
		m.put("enabled", e.getEnabled());
		m.put("status", e.getStatus());
		m.put("operator_id", e.getOperatorId());
		m.put("source", e.getSource());
		m.put("updated", e.getUpdated());
		m.put("ai_verify_time", e.getAiVerifyTime());
		m.put("manual_verify_time", e.getManualVerifyTime());
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("mobile", e.getMobile());
		m.put("message", message);
		return m;
	}
}
