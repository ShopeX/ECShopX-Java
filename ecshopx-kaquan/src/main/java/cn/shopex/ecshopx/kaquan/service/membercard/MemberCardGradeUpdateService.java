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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageTriggerGradeBindService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

@Service
public class MemberCardGradeUpdateService {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MemberCardGradeMultiLangWriteService memberCardGradeMultiLangWriteService;
	private final CardPackageTriggerGradeBindService cardPackageTriggerGradeBindService;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public MemberCardGradeUpdateService(MemberCardGradeMapper memberCardGradeMapper,
			MemberCardGradeMultiLangWriteService memberCardGradeMultiLangWriteService,
			CardPackageTriggerGradeBindService cardPackageTriggerGradeBindService,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.memberCardGradeMultiLangWriteService = memberCardGradeMultiLangWriteService;
		this.cardPackageTriggerGradeBindService = cardPackageTriggerGradeBindService;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public boolean updateGrades(long companyId, List<Map<String, Object>> newGrades) {
		for (Map<String, Object> row : newGrades) {
			normalizePrivilegesAndCompanyId(row, companyId);
		}

		List<MemberCardGrade> existingRows = memberCardGradeMapper.selectList(new LambdaQueryWrapper<MemberCardGrade>()
				.eq(MemberCardGrade::getCompanyId, String.valueOf(companyId)));
		List<Long> gradeIds = existingRows.stream().map(MemberCardGrade::getGradeId).toList();

		Set<Long> retained = new HashSet<>();
		for (Map<String, Object> g : newGrades) {
			Long gid = parseExplicitGradeId(g);
			if (gid != null) {
				retained.add(gid);
			}
		}
		List<Long> deleteIds = gradeIds.stream().filter(id -> !retained.contains(id)).toList();

		List<GradeUpsertOutcome> outcomes = new ArrayList<>();
		transactionTemplate.executeWithoutResult(status -> {
			for (Long delId : deleteIds) {
				MemberCardGrade found = memberCardGradeMapper.selectOne(new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, String.valueOf(companyId))
						.eq(MemberCardGrade::getGradeId, delId));
				if (found == null) {
					throw new ResourceException("要删除的等级不存在或已变更");
				}
				memberCardGradeMapper.deleteById(delId);
			}
			for (Map<String, Object> g : newGrades) {
				outcomes.add(upsertOneGrade(companyId, g));
			}
		});

		if (!gradeIds.isEmpty()) {
			cardPackageTriggerGradeBindService.deleteTriggersForGrades(companyId, gradeIds, "grade");
		}
		for (GradeUpsertOutcome item : outcomes) {
			if (!CollectionUtils.isEmpty(item.voucherPackage())) {
				cardPackageTriggerGradeBindService.setTriggersByPackageSet(companyId, item.voucherPackage(), item.gradeId(),
						"grade");
				cardPackageTriggerGradeBindService.clearReceiveRecords(companyId, item.gradeId(), "grade");
			}
		}
		return true;
	}

	private GradeUpsertOutcome upsertOneGrade(long companyId, Map<String, Object> g) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long requestGradeId = parseExplicitGradeId(g);
		MemberCardGrade entity;
		boolean isNew = requestGradeId == null;
		if (isNew) {
			entity = new MemberCardGrade();
		} else {
			MemberCardGrade loaded = memberCardGradeMapper.selectById(requestGradeId);
			if (loaded == null) {
				throw new ResourceException("要更新的等级不存在");
			}
			if (!String.valueOf(companyId).equals(loaded.getCompanyId())) {
				throw new ResourceException("要更新的等级不存在");
			}
			entity = loaded;
		}

		applyGradeFieldsFromMap(entity, g, isNew, now);

		if (isNew) {
			memberCardGradeMapper.insert(entity);
		} else {
			memberCardGradeMapper.updateById(entity);
		}
		Long finalGradeId = entity.getGradeId();
		if (finalGradeId == null || finalGradeId <= 0L) {
			throw new ResourceException("等级保存失败");
		}

		Map<String, Object> tempItem = buildTempItem(companyId, entity);
		memberCardGradeMultiLangWriteService.addOrUpdateDefaultLang(finalGradeId, tempItem, companyId);

		List<?> voucherPackage = extractVoucherPackage(g);
		return new GradeUpsertOutcome(finalGradeId, voucherPackage);
	}

	private static List<?> extractVoucherPackage(Map<String, Object> g) {
		Object vp = g.get("voucher_package");
		if (vp instanceof List<?> l) {
			return l.isEmpty() ? List.of() : l;
		}
		return List.of();
	}

	private Map<String, Object> buildTempItem(long companyId, MemberCardGrade e) {
		Map<String, Object> tempItem = new LinkedHashMap<>();
		tempItem.put("company_id", companyId);
		tempItem.put("grade_id", e.getGradeId());
		tempItem.put("grade_name", e.getGradeName());
		tempItem.put("default_grade", e.getDefaultGrade());
		tempItem.put("background_pic_url", e.getBackgroundPicUrl());
		tempItem.put("grade_background", e.getGradeBackground());
		tempItem.put("promotion_condition", e.getPromotionCondition());
		tempItem.put("privileges", e.getPrivileges());
		tempItem.put("created", e.getCreated());
		tempItem.put("updated", e.getUpdated());
		tempItem.put("third_data", e.getThirdData());
		tempItem.put("external_id", e.getExternalId());
		tempItem.put("description", e.getDescription());
		tempItem.put("dm_grade_code", e.getDmGradeCode());
		return tempItem;
	}

	private void applyGradeFieldsFromMap(MemberCardGrade entity, Map<String, Object> g, boolean isNew, int now) {
		entity.setCompanyId(String.valueOf(g.get("company_id")));

		if (g.containsKey("grade_name")) {
			entity.setGradeName(toNullableString(g.get("grade_name")));
		}
		if (g.containsKey("default_grade")) {
			entity.setDefaultGrade(toBoolean(g.get("default_grade")));
		}
		if (g.containsKey("background_pic_url")) {
			entity.setBackgroundPicUrl(toNullableString(g.get("background_pic_url")));
		}
		if (g.containsKey("grade_background")) {
			entity.setGradeBackground(toNullableString(g.get("grade_background")));
		}
		if (g.containsKey("promotion_condition")) {
			entity.setPromotionCondition(toDbJsonString(g.get("promotion_condition")));
		}
		if (g.containsKey("privileges")) {
			entity.setPrivileges(toDbJsonString(g.get("privileges")));
		}
		if (g.containsKey("third_data")) {
			entity.setThirdData(toDbJsonString(g.get("third_data")));
		}
		if (g.containsKey("external_id")) {
			entity.setExternalId(toStringOrEmpty(g.get("external_id")));
		} else if (isNew) {
			entity.setExternalId("");
		}
		if (g.containsKey("description")) {
			entity.setDescription(toNullableString(g.get("description")));
		}
		if (g.containsKey("dm_grade_code")) {
			entity.setDmGradeCode(toStringOrEmpty(g.get("dm_grade_code")));
		}

		if (isNew) {
			entity.setCreated(now);
			entity.setUpdated(now);
		} else {
			entity.setUpdated(now);
		}
	}

	private String toDbJsonString(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			return s;
		}
		if (v instanceof Map || v instanceof List) {
			try {
				return objectMapper.writeValueAsString(v);
			} catch (JsonProcessingException e) {
				String msg = e.getMessage();
				throw new ResourceException(msg == null || msg.isEmpty() ? "JSON 序列化失败" : msg);
			}
		}
		return String.valueOf(v);
	}

	private static String toNullableString(Object v) {
		if (v == null) {
			return null;
		}
		return v instanceof String s ? s : String.valueOf(v);
	}

	private static String toStringOrEmpty(Object v) {
		if (v == null) {
			return "";
		}
		return v instanceof String s ? s : String.valueOf(v);
	}

	private static Boolean toBoolean(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		if (v instanceof String s) {
			return "1".equals(s) || "true".equalsIgnoreCase(s.trim());
		}
		return Boolean.FALSE;
	}

	private static void normalizePrivilegesAndCompanyId(Map<String, Object> row, long companyId) {
		row.put("company_id", String.valueOf(companyId));
		Object privObj = row.get("privileges");
		Map<String, Object> privMap;
		if (privObj instanceof Map<?, ?> p) {
			privMap = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : p.entrySet()) {
				if (e.getKey() != null) {
					privMap.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
		} else {
			privMap = new LinkedHashMap<>();
		}
		Object discountObj = privMap.get("discount");
		boolean thenBranch = false;
		if (discountObj != null) {
			BigDecimal numeric = tryParseBigDecimal(discountObj);
			if (numeric != null && numeric.compareTo(BigDecimal.TEN) != 0) {
				thenBranch = true;
			}
		}
		if (thenBranch) {
			BigDecimal orig = tryParseBigDecimal(discountObj);
			if (orig == null) {
				thenBranch = false;
			} else {
				int intval = orig.multiply(BigDecimal.TEN).intValue();
				privMap.put("discount", 100 - intval);
				privMap.put("discount_desc", String.valueOf(discountObj));
			}
		}
		if (!thenBranch) {
			privMap.put("discount", 0);
			privMap.put("discount_desc", "10");
		}
		row.put("privileges", privMap);
	}

	private static BigDecimal tryParseBigDecimal(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof BigDecimal b) {
			return b;
		}
		if (o instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		if (o instanceof String s) {
			try {
				return new BigDecimal(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
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

	private record GradeUpsertOutcome(long gradeId, List<?> voucherPackage) {}
}
