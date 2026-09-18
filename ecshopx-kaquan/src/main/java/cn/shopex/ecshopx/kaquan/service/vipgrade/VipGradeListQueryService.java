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
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageBindListQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VipGradeListQueryService {

	private static final Logger log = LoggerFactory.getLogger(VipGradeListQueryService.class);

	private static final String TRIGGER_TYPE_VIP_GRADE = "vip_grade";

	private final VipGradeMapper vipGradeMapper;
	private final CardPackageBindListQueryService cardPackageBindListQueryService;
	private final ObjectMapper objectMapper;

	public VipGradeListQueryService(VipGradeMapper vipGradeMapper,
			CardPackageBindListQueryService cardPackageBindListQueryService,
			ObjectMapper objectMapper) {
		this.vipGradeMapper = vipGradeMapper;
		this.cardPackageBindListQueryService = cardPackageBindListQueryService;
		this.objectMapper = objectMapper;
	}

	/**
	 * 分页查询付费会员等级（第 1 页、每页 100 条、created 升序），并附加券包绑定摘要。
	 */
	public List<Map<String, Object>> listDataVipGrade(long companyId, boolean filterOnlyNonDisabled) {
		LambdaQueryWrapper<VipGrade> wrapper = new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId);
		if (filterOnlyNonDisabled) {
			wrapper.eq(VipGrade::getIsDisabled, Boolean.FALSE);
		}
		wrapper.orderByAsc(VipGrade::getCreated);
		Page<VipGrade> page = new Page<>(1, 100);
		Page<VipGrade> result = vipGradeMapper.selectPage(page, wrapper);
		List<VipGrade> records = result.getRecords();
		if (records == null || records.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> rows = new ArrayList<>();
		List<Long> gradeIdList = new ArrayList<>();
		for (VipGrade entity : records) {
			Map<String, Object> row = toRowMap(entity);
			rows.add(row);
			if (entity.getVipGradeId() != null) {
				gradeIdList.add(entity.getVipGradeId());
			}
		}
		Map<Long, List<Map<String, Object>>> bindIndex = gradeIdList.isEmpty()
				? Collections.emptyMap()
				: cardPackageBindListQueryService.getBindPackageList(companyId, gradeIdList, TRIGGER_TYPE_VIP_GRADE);
		for (Map<String, Object> row : rows) {
			Object idObj = row.get("vip_grade_id");
			if (!(idObj instanceof Number) || ((Number) idObj).longValue() <= 0L) {
				row.put("voucher_package", Collections.emptyList());
				continue;
			}
			long id = ((Number) idObj).longValue();
			row.put("voucher_package", bindIndex.getOrDefault(id, Collections.emptyList()));
		}
		return rows;
	}

	/**
	 * 付费会员等级列表（第 1 页、每页 100 条、created 升序、仅未禁用），不含券包摘要列。
	 */
	public List<Map<String, Object>> listVipGradesForWxappMembercardGrades(long companyId) {
		LambdaQueryWrapper<VipGrade> wrapper = new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId)
				.eq(VipGrade::getIsDisabled, Boolean.FALSE);
		wrapper.orderByAsc(VipGrade::getCreated);
		Page<VipGrade> page = new Page<>(1, 100);
		Page<VipGrade> pageResult = vipGradeMapper.selectPage(page, wrapper);
		List<VipGrade> records = pageResult.getRecords();
		if (records == null || records.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> rows = new ArrayList<>(records.size());
		for (VipGrade entity : records) {
			rows.add(toRowMap(entity));
		}
		return rows;
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
