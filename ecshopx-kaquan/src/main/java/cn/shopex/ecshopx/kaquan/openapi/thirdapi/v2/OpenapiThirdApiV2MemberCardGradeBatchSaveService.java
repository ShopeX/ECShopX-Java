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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeMultiLangReadService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeUpdateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberCardGradeBatchSaveService {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final MemberCardGradeUpdateService memberCardGradeUpdateService;
	private final MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardGradeBatchSaveService(
			MemberCardGradeMapper memberCardGradeMapper,
			MemberCardGradeQueryService memberCardGradeQueryService,
			MemberCardGradeUpdateService memberCardGradeUpdateService,
			MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService,
			ObjectMapper objectMapper) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.memberCardGradeUpdateService = memberCardGradeUpdateService;
		this.memberCardGradeMultiLangReadService = memberCardGradeMultiLangReadService;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> executeOpenapiBatchSave(
			long companyId, List<Map<String, Object>> gradeInfoItems) {
		List<Map<String, Object>> sorted = new ArrayList<>(gradeInfoItems);
		sorted.sort(Comparator.comparingDouble(this::gradeLevelSortKey));

		String companyIdStr = String.valueOf(companyId);
		List<String> externalIds = sorted.stream()
				.map(item -> stringOrNull(item.get("grade_id")))
				.filter(Objects::nonNull)
				.distinct()
				.toList();

		Map<String, MemberCardGrade> curExternalById = new LinkedHashMap<>();
		if (!externalIds.isEmpty()) {
			List<MemberCardGrade> existingNonDefault = memberCardGradeMapper.selectList(
					new LambdaQueryWrapper<MemberCardGrade>()
							.eq(MemberCardGrade::getCompanyId, companyIdStr)
							.eq(MemberCardGrade::getDefaultGrade, Boolean.FALSE)
							.in(MemberCardGrade::getExternalId, externalIds));
			for (MemberCardGrade g : existingNonDefault) {
				if (g.getExternalId() != null) {
					curExternalById.put(g.getExternalId(), g);
				}
			}
		}

		MemberCardGrade defaultGradeEntity = memberCardGradeMapper.selectOne(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr)
						.eq(MemberCardGrade::getDefaultGrade, Boolean.TRUE)
						.last("LIMIT 1"));
		Long defaultGradeId = defaultGradeEntity != null ? defaultGradeEntity.getGradeId() : null;

		List<Map<String, Object>> gradeInfoMaps = new ArrayList<>();
		for (int key = 0; key < sorted.size(); key++) {
			Map<String, Object> item = sorted.get(key);
			String externalId = stringOrEmpty(item.get("grade_id"));

			Map<String, Object> grade = new LinkedHashMap<>();
			grade.put("company_id", companyIdStr);
			grade.put("grade_name", item.get("grade_name"));
			grade.put("external_id", externalId);
			grade.put("privileges", Map.of("discount", "10"));
			grade.put("promotion_condition", Map.of("total_consumption", item.get("grade_level")));
			grade.put("default_grade", Boolean.FALSE);

			if (key == 0) {
				grade.put("default_grade", Boolean.TRUE);
				if (defaultGradeId != null) {
					grade.put("grade_id", defaultGradeId);
				}
			} else if (curExternalById.containsKey(externalId)) {
				grade.put("grade_id", curExternalById.get(externalId).getGradeId());
			} else {
				grade.put("grade_id", "");
			}
			gradeInfoMaps.add(grade);
		}

		List<Map<String, Object>> curList =
				memberCardGradeQueryService.getGradeListByCompanyId(companyId, true);

		Set<Long> retainedGradeIds = new HashSet<>();
		for (Map<String, Object> g : gradeInfoMaps) {
			Long gid = parseExplicitGradeId(g);
			if (gid != null) {
				retainedGradeIds.add(gid);
			}
		}

		List<String> noDeleteMsg = new ArrayList<>();
		for (Map<String, Object> cur : curList) {
			Long gradeId = toLong(cur.get("grade_id"));
			if (gradeId == null || retainedGradeIds.contains(gradeId)) {
				continue;
			}
			int memberCount = intOrZero(cur.get("member_count"));
			if (memberCount > 0) {
				String extId = stringOrEmpty(cur.get("external_id"));
				String name = stringOrEmpty(cur.get("grade_name"));
				noDeleteMsg.add(extId + "-" + name);
			}
		}
		if (!noDeleteMsg.isEmpty()) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_GRADE_ERROR,
					String.join(";", noDeleteMsg) + "，已有会员在使用，不能变更。");
		}

		memberCardGradeUpdateService.updateGrades(companyId, gradeInfoMaps);

		List<MemberCardGrade> entities = memberCardGradeMapper.selectList(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr));

		if (entities.isEmpty()) {
			return List.of();
		}

		List<Map<String, Object>> overlayRows = entities.stream()
				.map(OpenapiMemberCardGradeOpenApiFormatSupport::toOverlayRow)
				.collect(Collectors.toCollection(ArrayList::new));
		memberCardGradeMultiLangReadService.applyOverlays(companyId, overlayRows, "zh-CN");

		return overlayRows.stream()
				.map(row -> OpenapiMemberCardGradeOpenApiFormatSupport.formatShuyunGradeRow(row, objectMapper))
				.toList();
	}

	private double gradeLevelSortKey(Map<String, Object> item) {
		return toSortDouble(item.get("grade_level"));
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

	private static Long parseExplicitGradeId(Map<String, Object> g) {
		if (!g.containsKey("grade_id")) {
			return null;
		}
		Object v = g.get("grade_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static String stringOrNull(Object value) {
		if (value == null) {
			return null;
		}
		String s = String.valueOf(value);
		return s.isEmpty() ? null : s;
	}

	private static String stringOrEmpty(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static Long toLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value == null) {
			return null;
		}
		try {
			return Long.parseLong(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int intOrZero(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
