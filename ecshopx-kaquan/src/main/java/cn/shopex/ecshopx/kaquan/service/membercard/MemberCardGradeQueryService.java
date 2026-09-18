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

package cn.shopex.ecshopx.kaquan.service.membercard;

import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageBindListQueryService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MemberCardGradeQueryService {

	private static final Logger log = LoggerFactory.getLogger(MemberCardGradeQueryService.class);

	private static final String DEFAULT_LANG = "zh-CN";

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final ObjectMapper objectMapper;
	private final MembersMapper membersMapper;
	private final MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService;
	private final CardPackageBindListQueryService cardPackageBindListQueryService;

	public MemberCardGradeQueryService(MemberCardGradeMapper memberCardGradeMapper, ObjectMapper objectMapper,
			MembersMapper membersMapper,
			MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService,
			CardPackageBindListQueryService cardPackageBindListQueryService) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.objectMapper = objectMapper;
		this.membersMapper = membersMapper;
		this.memberCardGradeMultiLangReadService = memberCardGradeMultiLangReadService;
		this.cardPackageBindListQueryService = cardPackageBindListQueryService;
	}

	/**
	 * 按商户与默认等级标志查询一条等级；无记录返回空列表（JSON data 为 []）。
	 */
	public Object getDefaultGradeData(long companyId) {
		String companyIdStr = String.valueOf(companyId);
		log.info("query default member card grade: company_id={}, default_grade=true", companyIdStr);

		MemberCardGrade entity = memberCardGradeMapper.selectOne(new LambdaQueryWrapper<MemberCardGrade>()
				.eq(MemberCardGrade::getCompanyId, companyIdStr)
				.eq(MemberCardGrade::getDefaultGrade, Boolean.TRUE)
				.last("LIMIT 1"));

		if (entity == null) {
			log.info("default grade query result: no row for company_id={}", companyIdStr);
			return Collections.emptyList();
		}
		log.info("default grade query result: hit grade_id={} company_id={}", entity.getGradeId(), companyIdStr);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("grade_id", entity.getGradeId());
		row.put("company_id", entity.getCompanyId());
		row.put("grade_name", entity.getGradeName());
		row.put("default_grade", entity.getDefaultGrade());
		row.put("background_pic_url", entity.getBackgroundPicUrl());
		row.put("promotion_condition", parseJsonColumn(entity.getPromotionCondition()));
		row.put("privileges", parseJsonColumn(entity.getPrivileges()));
		row.put("description", entity.getDescription());
		return row;
	}

	/**
	 * 按公司查询全部等级行（含多语言、人数、JSON 列解析、排序与券包绑定摘要）。
	 */
	public List<Map<String, Object>> getGradeListByCompanyId(long companyId, boolean isMemberCount) {
		String companyIdStr = String.valueOf(companyId);
		List<MemberCardGrade> entities = memberCardGradeMapper
				.selectList(new LambdaQueryWrapper<MemberCardGrade>().eq(MemberCardGrade::getCompanyId, companyIdStr));
		if (entities == null || entities.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> rows = new ArrayList<>();
		for (MemberCardGrade entity : entities) {
			rows.add(buildBaseGradeRow(entity));
		}
		memberCardGradeMultiLangReadService.applyOverlays(companyId, rows, DEFAULT_LANG);
		for (int i = 0; i < rows.size(); i++) {
			MemberCardGrade entity = entities.get(i);
			Map<String, Object> row = rows.get(i);
			if (isMemberCount) {
				Long gradeId = entity.getGradeId();
				Long cnt = membersMapper.selectCount(new LambdaQueryWrapper<Members>()
						.eq(Members::getCompanyId, companyId)
						.eq(Members::getGradeId, gradeId));
				row.put("member_count", cnt == null ? 0 : cnt.intValue());
			}
			row.put("promotion_condition", parseJsonColumnForGradeList(entity.getPromotionCondition()));
			row.put("privileges", parseJsonColumnForGradeList(entity.getPrivileges()));
			row.put("default_grade", Boolean.TRUE.equals(entity.getDefaultGrade()));
			row.put("crm_open", "false");
		}
		rows.sort(Comparator.comparingDouble(this::promotionTotalConsumptionSortKey));
		List<Long> gradeIds = new ArrayList<>();
		for (MemberCardGrade e : entities) {
			if (e.getGradeId() != null) {
				gradeIds.add(e.getGradeId());
			}
		}
		Map<Long, List<Map<String, Object>>> bindIndex =
				cardPackageBindListQueryService.getBindPackageList(companyId, gradeIds, "grade");
		for (Map<String, Object> row : rows) {
			Object gid = row.get("grade_id");
			long gradeId = gid instanceof Number ? ((Number) gid).longValue() : 0L;
			row.put("voucher_package", bindIndex.getOrDefault(gradeId, Collections.emptyList()));
		}
		return rows;
	}

	private Map<String, Object> buildBaseGradeRow(MemberCardGrade entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", entity.getCompanyId());
		row.put("grade_id", entity.getGradeId());
		row.put("grade_name", entity.getGradeName());
		row.put("background_pic_url", entity.getBackgroundPicUrl());
		row.put("grade_background", entity.getGradeBackground());
		row.put("promotion_condition", entity.getPromotionCondition());
		row.put("privileges", entity.getPrivileges());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("third_data", entity.getThirdData());
		row.put("external_id", entity.getExternalId());
		row.put("description", entity.getDescription());
		row.put("dm_grade_code", entity.getDmGradeCode());
		return row;
	}

	private double promotionTotalConsumptionSortKey(Map<String, Object> row) {
		Object pc = row.get("promotion_condition");
		if (!(pc instanceof Map<?, ?> m)) {
			return 0;
		}
		Object v = m.get("total_consumption");
		return toSortDouble(v);
	}

	private static double toSortDouble(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0;
			}
			try {
				return Double.parseDouble(t);
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private Object parseJsonColumn(String raw) {
		if (raw == null) {
			return Collections.emptyList();
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			JsonNode node = objectMapper.readTree(trimmed);
			return objectMapper.convertValue(node, Object.class);
		} catch (Exception e) {
			log.warn("invalid JSON in member card grade column: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 等级列表用：列为 null、空串或仅空白时返回 null；否则解析 JSON，非法则记告警并返回 null。
	 */
	private Object parseJsonColumnForGradeList(String raw) {
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
			log.warn("invalid JSON in member card grade column: {}", e.getMessage());
			return null;
		}
	}
}
