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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VipGradeUserVipGradeGetService {

	private static final String DEFAULT_VIP = "svip";

	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final VipGradeMapper vipGradeMapper;
	private final ObjectMapper objectMapper;

	public VipGradeUserVipGradeGetService(VipGradeRelUserMapper vipGradeRelUserMapper,
			VipGradeMapper vipGradeMapper,
			ObjectMapper objectMapper) {
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.vipGradeMapper = vipGradeMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> userVipGradeGet(long companyId, long userId, boolean ifAll) {
		long now = System.currentTimeMillis() / 1000L;
		List<VipGradeRelUser> relRows = vipGradeRelUserMapper.selectList(new LambdaQueryWrapper<VipGradeRelUser>()
				.eq(VipGradeRelUser::getUserId, userId)
				.eq(VipGradeRelUser::getCompanyId, (int) companyId)
				.apply("CAST(end_date AS UNSIGNED) > {0}", now));
		List<VipGrade> gradeRows = vipGradeMapper.selectList(new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId));

		if (relRows.isEmpty() && gradeRows.isEmpty()) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("is_open", false);
			m.put("is_vip", false);
			m.put("is_had_vip", false);
			return m;
		}

		if (!gradeRows.isEmpty() && relRows.isEmpty()) {
			Map<String, Object> gradedata = new LinkedHashMap<>();
			gradedata.put("is_open", false);
			for (VipGrade value : gradeRows) {
				if (!Boolean.TRUE.equals(gradedata.get("is_open")) && gradeEnabled(value)) {
					gradedata = mapGradeConfigRow(value);
					gradedata.put("is_open", true);
					break;
				}
				if (Boolean.TRUE.equals(value.getIsDefault()) && gradeEnabled(value)) {
					gradedata = mapGradeConfigRow(value);
					gradedata.put("is_open", true);
					break;
				}
			}
			gradedata.putIfAbsent("discount", 0);
			gradedata.put("is_vip", false);
			gradedata.put("is_had_vip", false);
			return gradedata;
		}

		Map<String, VipGrade> userVipGradeByLv = new LinkedHashMap<>();
		for (VipGrade g : gradeRows) {
			if (g.getLvType() != null) {
				userVipGradeByLv.put(g.getLvType(), g);
			}
		}
		boolean isOpen = false;
		for (VipGrade value : gradeRows) {
			if (gradeEnabled(value)) {
				isOpen = true;
				break;
			}
		}
		Map<String, Map<String, Object>> result = new LinkedHashMap<>();
		for (VipGradeRelUser rel : relRows) {
			Map<String, Object> row = mapRelUserRow(rel, now);
			row.put("lv_type", rel.getVipType());
			row.put("is_had_vip", true);
			boolean valid = Boolean.TRUE.equals(row.get("valid"));
			row.put("is_vip", valid);
			row.put("is_open", isOpen);
			VipGrade gradeData = userVipGradeByLv.get(rel.getVipType());
			if (gradeData != null) {
				row.put("discount", discountFromPrivileges(gradeData.getPrivileges()));
				row.put("grade_name", gradeData.getGradeName());
				row.put("guide_title", gradeData.getGuideTitle());
				row.put("background_pic_url", gradeData.getBackgroundPicUrl());
			} else {
				row.put("valid", false);
			}
			String vt = rel.getVipType();
			if (vt != null) {
				result.put(vt, row);
			}
		}

		if (ifAll) {
			Map<String, Object> out = new LinkedHashMap<>();
			result.forEach(out::put);
			return out;
		}
		if (result.containsKey(DEFAULT_VIP)) {
			return result.get(DEFAULT_VIP);
		}
		if (result.isEmpty()) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("is_vip", false);
			m.put("is_open", isOpen);
			return m;
		}
		return result.values().iterator().next();
	}

	private Map<String, Object> mapRelUserRow(VipGradeRelUser rel, long now) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("vip_grade_id", rel.getVipGradeId() != null ? rel.getVipGradeId() : 0L);
		m.put("vip_type", rel.getVipType());
		long end = parseEndDateEpoch(rel.getEndDate());
		m.put("end_date", end);
		m.put("end_time", java.time.Instant.ofEpochSecond(Math.min(end, Integer.MAX_VALUE))
				.atZone(java.time.ZoneId.systemDefault())
				.toLocalDate()
				.toString());
		long day = 0L;
		if (end > now) {
			day = (long) Math.ceil(Math.abs(end - now) / 86400.0);
		}
		m.put("day", day);
		boolean valid = day > 0;
		m.put("valid", valid);
		if (!valid) {
			m.put("is_vip", false);
		}
		return m;
	}

	private Map<String, Object> mapGradeConfigRow(VipGrade value) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("vip_grade_id", value.getVipGradeId() != null ? value.getVipGradeId() : 0L);
		m.put("lv_type", value.getLvType());
		m.put("grade_name", value.getGradeName());
		m.put("guide_title", value.getGuideTitle());
		m.put("background_pic_url", value.getBackgroundPicUrl());
		m.put("discount", discountFromPrivileges(value.getPrivileges()));
		return m;
	}

	private static boolean gradeEnabled(VipGrade value) {
		return value.getIsDisabled() == null || Boolean.FALSE.equals(value.getIsDisabled());
	}

	private int discountFromPrivileges(String privilegesJson) {
		if (!StringUtils.hasText(privilegesJson)) {
			return 0;
		}
		try {
			JsonNode n = objectMapper.readTree(privilegesJson);
			JsonNode d = n.get("discount");
			return d != null && d.isNumber() ? d.intValue() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private static long parseEndDateEpoch(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
