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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VipGradeUserUseListService {

	private static final Logger log = LoggerFactory.getLogger(VipGradeUserUseListService.class);

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final ObjectMapper objectMapper;

	public VipGradeUserUseListService(VipGradeMapper vipGradeMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			ObjectMapper objectMapper) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * 构建指定用户在公司下的付费等级使用列表（未禁用等级 + 有效关系上的到期信息）。
	 */
	public Map<String, Object> buildUseList(long companyId, Long userId) {
		int companyIdInt = (int) companyId;
		long nowSec = Instant.now().getEpochSecond();

		List<VipGradeRelUser> memberRows = listActiveRelUsers(companyIdInt, userId, nowSec);
		List<VipGrade> vipGradeAll = listAllGradesPage(companyIdInt);

		Map<String, Map<String, Object>> useByLvType = new LinkedHashMap<>();
		if (memberRows.isEmpty() && vipGradeAll.isEmpty()) {
			// 分支 A：合并用 Map 为空
		} else if (!vipGradeAll.isEmpty() && memberRows.isEmpty()) {
			// 分支 B：有等级、无有效关系；Controller 合并侧按 lv_type 查找通常为 false，不写入 Map
		} else if (!memberRows.isEmpty()) {
			Map<String, VipGrade> gradeByLvType = new LinkedHashMap<>();
			for (VipGrade g : vipGradeAll) {
				if (g.getLvType() != null) {
					gradeByLvType.put(g.getLvType(), g);
				}
			}
			boolean anyNonDisabled = false;
			for (VipGrade value : vipGradeAll) {
				if (isGradeEffectivelyEnabled(value.getIsDisabled())) {
					anyNonDisabled = true;
					break;
				}
			}
			for (VipGradeRelUser rel : memberRows) {
				String vipType = rel.getVipType();
				if (vipType == null) {
					continue;
				}
				Map<String, Object> cell = new LinkedHashMap<>();
				cell.put("lv_type", vipType);
				cell.put("is_had_vip", Boolean.TRUE);
				cell.put("is_vip", Boolean.TRUE);
				long endSec = parseEndDateSeconds(rel.getEndDate());
				cell.put("end_time", formatYmd(endSec));
				VipGrade gradeData = gradeByLvType.get(vipType);
				if (gradeData != null) {
					cell.put("discount", extractDiscount(gradeData));
					cell.put("grade_name", gradeData.getGradeName());
					cell.put("guide_title", gradeData.getGuideTitle());
					cell.put("background_pic_url", gradeData.getBackgroundPicUrl());
				}
				cell.put("is_open", anyNonDisabled);
				useByLvType.put(vipType, cell);
			}
		}

		List<VipGrade> nonDisabledGrades = listNonDisabledGradesPage(companyIdInt);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("is_open", Boolean.FALSE);
		result.put("list", new ArrayList<Map<String, Object>>());
		if (nonDisabledGrades == null || nonDisabledGrades.isEmpty()) {
			return result;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		result.put("is_open", Boolean.TRUE);
		for (VipGrade entity : nonDisabledGrades) {
			Map<String, Object> row = toRowMap(entity);
			String lvType = entity.getLvType();
			Map<String, Object> use = lvType != null ? useByLvType.get(lvType) : null;
			if (use != null) {
				row.put("end_time", use.get("end_time") != null ? use.get("end_time").toString() : "");
				row.put("is_had_vip", Boolean.TRUE.equals(use.get("is_had_vip")));
			} else {
				row.put("end_time", "");
				row.put("is_had_vip", Boolean.FALSE);
			}
			list.add(row);
		}
		return result;
	}

	private List<VipGradeRelUser> listActiveRelUsers(int companyIdInt, Long userId, long nowSec) {
		LambdaQueryWrapper<VipGradeRelUser> w = new LambdaQueryWrapper<VipGradeRelUser>()
				.eq(VipGradeRelUser::getCompanyId, companyIdInt);
		if (userId == null) {
			w.isNull(VipGradeRelUser::getUserId);
		} else {
			w.eq(VipGradeRelUser::getUserId, userId);
		}
		w.apply("CAST(end_date AS UNSIGNED) > {0}", nowSec)
				.orderByDesc(VipGradeRelUser::getId);
		Page<VipGradeRelUser> page = new Page<>(1, 100);
		Page<VipGradeRelUser> p = vipGradeRelUserMapper.selectPage(page, w);
		List<VipGradeRelUser> rec = p.getRecords();
		return rec != null ? rec : List.of();
	}

	private List<VipGrade> listAllGradesPage(int companyIdInt) {
		LambdaQueryWrapper<VipGrade> wrapper = new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, companyIdInt)
				.orderByAsc(VipGrade::getCreated);
		Page<VipGrade> page = new Page<>(1, 100);
		Page<VipGrade> result = vipGradeMapper.selectPage(page, wrapper);
		List<VipGrade> rec = result.getRecords();
		return rec != null ? rec : List.of();
	}

	private List<VipGrade> listNonDisabledGradesPage(int companyIdInt) {
		LambdaQueryWrapper<VipGrade> wrapper = new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, companyIdInt)
				.eq(VipGrade::getIsDisabled, Boolean.FALSE)
				.orderByAsc(VipGrade::getCreated);
		Page<VipGrade> page = new Page<>(1, 100);
		Page<VipGrade> result = vipGradeMapper.selectPage(page, wrapper);
		List<VipGrade> rec = result.getRecords();
		return rec != null ? rec : List.of();
	}

	private static boolean isGradeEffectivelyEnabled(Boolean isDisabled) {
		if (isDisabled == null) {
			return true;
		}
		return Boolean.FALSE.equals(isDisabled);
	}

	private Object extractDiscount(VipGrade gradeData) {
		Object priv = parseJsonColumn(gradeData.getPrivileges());
		if (priv instanceof Map<?, ?> m) {
			Object d = m.get("discount");
			return d != null ? d : 0;
		}
		return 0;
	}

	private long parseEndDateSeconds(String endDateRaw) {
		if (endDateRaw == null) {
			return 0L;
		}
		String t = endDateRaw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private String formatYmd(long epochSeconds) {
		if (epochSeconds <= 0L) {
			return YMD.format(Instant.ofEpochSecond(0));
		}
		return YMD.format(Instant.ofEpochSecond(epochSeconds));
	}

	private Map<String, Object> toRowMap(VipGrade entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("vip_grade_id", entity.getVipGradeId());
		row.put("company_id", entity.getCompanyId());
		row.put("grade_name", entity.getGradeName());
		row.put("lv_type", entity.getLvType());
		row.put("default_grade", entity.getDefaultGrade());
		row.put("is_disabled", entity.getIsDisabled());
		row.put("background_pic_url", entity.getBackgroundPicUrl());
		row.put("description", entity.getDescription());
		row.put("price_list", parseJsonColumn(entity.getPriceList()));
		row.put("privileges", parseJsonColumn(entity.getPrivileges()));
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("guide_title", entity.getGuideTitle());
		row.put("is_default", entity.getIsDefault());
		String ext = entity.getExternalId();
		row.put("external_id", ext != null ? ext : "");
		return row;
	}

	private Object parseJsonColumn(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(trimmed);
			return objectMapper.convertValue(node, Object.class);
		} catch (Exception e) {
			log.warn("invalid JSON in vip grade column: {}", e.getMessage());
			return null;
		}
	}
}
