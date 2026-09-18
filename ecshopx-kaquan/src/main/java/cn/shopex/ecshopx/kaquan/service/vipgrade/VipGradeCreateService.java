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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageTriggerGradeBindService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class VipGradeCreateService {

	private static final Logger log = LoggerFactory.getLogger(VipGradeCreateService.class);

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final CardPackageTriggerGradeBindService cardPackageTriggerGradeBindService;
	private final ObjectMapper objectMapper;

	public VipGradeCreateService(VipGradeMapper vipGradeMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			CardPackageTriggerGradeBindService cardPackageTriggerGradeBindService,
			ObjectMapper objectMapper) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.cardPackageTriggerGradeBindService = cardPackageTriggerGradeBindService;
		this.objectMapper = objectMapper;
	}

	public void createVipGrades(long companyId, List<Map<String, Object>> gradeInfoList) {
		int companyIdInt = (int) companyId;
		List<VipGrade> gradeList = vipGradeMapper.selectList(new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, companyIdInt));
		List<Long> gradeIds = gradeList.stream()
				.map(VipGrade::getVipGradeId)
				.filter(Objects::nonNull)
				.toList();
		Set<Long> gradeIdsSnapshot = new HashSet<>(gradeIds);

		Set<Long> newGradeIds = new HashSet<>();
		for (Map<String, Object> row : gradeInfoList) {
			Long id = parseVipGradeId(row);
			if (id != null && id > 0L) {
				newGradeIds.add(id);
			}
		}
		List<Long> deleteIds = gradeIds.stream().filter(id -> !newGradeIds.contains(id)).toList();

		if (!deleteIds.isEmpty()) {
			long relCount = vipGradeRelUserMapper.selectCount(new LambdaQueryWrapper<VipGradeRelUser>()
					.eq(VipGradeRelUser::getCompanyId, companyIdInt)
					.in(VipGradeRelUser::getVipGradeId, deleteIds));
			if (relCount > 0L) {
				throw new ResourceException("该等级下仍有会员，无法删除");
			}
			vipGradeMapper.delete(new LambdaQueryWrapper<VipGrade>()
					.eq(VipGrade::getCompanyId, companyIdInt)
					.in(VipGrade::getVipGradeId, deleteIds));
			cardPackageTriggerGradeBindService.deleteTriggersForGrades(companyId, deleteIds, "vip_grade");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Map<String, Object> source : gradeInfoList) {
			Long associationId = null;
			Map<String, Object> normalized = new LinkedHashMap<>(source);
			normalizePrivilegesAndCompanyId(normalized, companyId);
			log.info("vip grade sync: {}", normalized);

			Long rawId = parseVipGradeId(normalized);
			if (rawId != null && rawId > 0L && !gradeIdsSnapshot.contains(rawId)) {
				log.debug("skip vip grade row: id {} not in company grade snapshot before sync", rawId);
				continue;
			}

			List<?> voucherPackage = extractVoucherPackage(normalized);

			if (rawId != null && rawId > 0L && gradeIdsSnapshot.contains(rawId)) {
				VipGrade entity = vipGradeMapper.selectOne(new LambdaQueryWrapper<VipGrade>()
						.eq(VipGrade::getCompanyId, companyIdInt)
						.eq(VipGrade::getVipGradeId, rawId));
				if (entity == null) {
					throw new ResourceException("要更新的等级不存在");
				}
				associationId = rawId;
				cardPackageTriggerGradeBindService.deleteTriggersForGrades(companyId, List.of(associationId), "vip_grade");
				applyVipGradeFromMap(entity, normalized, false, now);
				vipGradeMapper.updateById(entity);
			} else {
				VipGrade entity = new VipGrade();
				applyVipGradeFromMap(entity, normalized, true, now);
				vipGradeMapper.insert(entity);
				Long generatedId = entity.getVipGradeId();
				if (generatedId == null || generatedId <= 0L) {
					throw new ResourceException("等级保存失败");
				}
				associationId = generatedId;
			}

			if (associationId != null && !CollectionUtils.isEmpty(voucherPackage)) {
				cardPackageTriggerGradeBindService.setTriggersByPackageSet(companyId, voucherPackage, associationId,
						"vip_grade");
				cardPackageTriggerGradeBindService.clearReceiveRecords(companyId, associationId, "vip_grade");
			}
		}
	}

	private void applyVipGradeFromMap(VipGrade entity, Map<String, Object> n, boolean isNew, int now) {
		Object cid = n.get("company_id");
		if (cid instanceof Number num) {
			entity.setCompanyId(num.intValue());
		} else if (cid != null) {
			entity.setCompanyId(Integer.parseInt(String.valueOf(cid)));
		}

		if (n.containsKey("grade_name")) {
			entity.setGradeName(toNullableString(n.get("grade_name")));
		}
		if (n.containsKey("lv_type")) {
			entity.setLvType(toNullableString(n.get("lv_type")));
		}
		if (n.containsKey("is_disabled")) {
			entity.setIsDisabled(toBoolean(n.get("is_disabled")));
		}
		if (n.containsKey("background_pic_url")) {
			entity.setBackgroundPicUrl(toNullableString(n.get("background_pic_url")));
		}
		if (n.containsKey("guide_title")) {
			entity.setGuideTitle(toNullableString(n.get("guide_title")));
		}
		if (n.containsKey("is_default")) {
			entity.setIsDefault(toBoolean(n.get("is_default")));
		}
		if (n.containsKey("description")) {
			entity.setDescription(toNullableString(n.get("description")));
		}
		if (n.containsKey("external_id")) {
			entity.setExternalId(toStringOrEmpty(n.get("external_id")));
		} else if (isNew) {
			entity.setExternalId("");
		}
		if (n.containsKey("price_list")) {
			entity.setPriceList(toDbJsonString(n.get("price_list")));
		}
		if (n.containsKey("privileges")) {
			entity.setPrivileges(toDbJsonString(n.get("privileges")));
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
		row.put("company_id", (int) companyId);
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

	private static Long parseVipGradeId(Map<String, Object> g) {
		if (!g.containsKey("vip_grade_id")) {
			return null;
		}
		Object v = g.get("vip_grade_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long lv = n.longValue();
			return lv == 0L ? null : lv;
		}
		if (v instanceof String s) {
			try {
				long lv = Long.parseLong(s.trim());
				return lv == 0L ? null : lv;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static List<?> extractVoucherPackage(Map<String, Object> g) {
		Object vp = g.get("voucher_package");
		if (vp instanceof List<?> l) {
			return l.isEmpty() ? List.of() : l;
		}
		if (vp instanceof Collection<?> c && !c.isEmpty()) {
			return new ArrayList<>(c);
		}
		return List.of();
	}
}
