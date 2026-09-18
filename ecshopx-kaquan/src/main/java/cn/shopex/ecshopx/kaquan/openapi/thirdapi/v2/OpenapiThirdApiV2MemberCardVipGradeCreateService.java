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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberCardVipGradeCreateService {

	private static final String LV_TYPE_VIP = "vip";
	private static final String LV_TYPE_SVIP = "svip";

	private final VipGradeMapper vipGradeMapper;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardVipGradeCreateService(
			VipGradeMapper vipGradeMapper, ObjectMapper objectMapper) {
		this.vipGradeMapper = vipGradeMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiCreate(
			long companyId,
			String gradeNameRaw,
			String monthlyFeeRaw,
			String quarterFeeRaw,
			String yearFeeRaw,
			String discountRaw,
			String guideTitleRaw,
			String descriptionRaw,
			String isDefaultRaw,
			String isDisabledRaw,
			String externalIdRaw) {
		validateParams(gradeNameRaw, monthlyFeeRaw, quarterFeeRaw, yearFeeRaw, discountRaw);

		String lvType = resolveLvType(companyId);
		String gradeName = gradeNameRaw;
		String guideTitle = guideTitleRaw == null ? "" : guideTitleRaw;
		String description = descriptionRaw == null ? "" : descriptionRaw;

		boolean isDefault = parseWeakBoolean(isDefaultRaw, 0);
		boolean isDisabled = parseWeakBoolean(isDisabledRaw, 0);

		List<Map<String, Object>> priceList =
				buildPriceList(monthlyFeeRaw, quarterFeeRaw, yearFeeRaw);
		Map<String, Object> privileges = new LinkedHashMap<>();
		privileges.put("discount_desc", discountRaw);
		privileges.put("discount", 100 - (int) (Double.parseDouble(discountRaw.trim()) * 10));

		String priceListJson;
		String privilegesJson;
		try {
			priceListJson = objectMapper.writeValueAsString(priceList);
			privilegesJson = objectMapper.writeValueAsString(privileges);
		} catch (JsonProcessingException e) {
			throw new ResourceException("付费等级数据序列化失败");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		VipGrade entity = new VipGrade();
		entity.setCompanyId((int) companyId);
		entity.setGradeName(gradeName);
		entity.setLvType(lvType);
		entity.setGuideTitle(guideTitle.isEmpty() ? null : guideTitle);
		entity.setDescription(description.isEmpty() ? null : description);
		entity.setBackgroundPicUrl("");
		entity.setIsDefault(isDefault);
		entity.setIsDisabled(isDisabled);
		entity.setPriceList(priceListJson);
		entity.setPrivileges(privilegesJson);
		entity.setExternalId("");
		entity.setCreated(now);
		entity.setUpdated(now);

		vipGradeMapper.insert(entity);

		return OpenapiMemberCardVipGradeOpenApiFormatSupport.formatOpenApiVipGradeRow(entity, objectMapper);
	}

	private void validateParams(
			String gradeNameRaw,
			String monthlyFeeRaw,
			String quarterFeeRaw,
			String yearFeeRaw,
			String discountRaw) {
		if (gradeNameRaw == null || gradeNameRaw.isEmpty()) {
			throw missingParams("未填写付费等级名称");
		}
		if (discountRaw == null || discountRaw.isEmpty()) {
			throw missingParams("未填写会员折扣");
		}
		if (isFeeAbsent(monthlyFeeRaw) && isFeeAbsent(quarterFeeRaw) && isFeeAbsent(yearFeeRaw)) {
			throw missingParams("购买金额必填一项");
		}
		validatePresentFee(monthlyFeeRaw);
		validatePresentFee(quarterFeeRaw);
		validatePresentFee(yearFeeRaw);
		if (!isNumericBetween1And10Inclusive(discountRaw)) {
			throw missingParams("请求参数错误");
		}
	}

	private static boolean isFeeAbsent(String raw) {
		return raw == null || raw.isEmpty();
	}

	private static void validatePresentFee(String feeRaw) {
		if (feeRaw == null) {
			return;
		}
		if (feeRaw.isEmpty() || !isNumericNonNegative(feeRaw)) {
			throw missingParams("请求参数错误");
		}
	}

	private String resolveLvType(long companyId) {
		boolean hasVip = false;
		boolean hasSvip = false;

		List<VipGrade> existing = vipGradeMapper.selectList(
				new LambdaQueryWrapper<VipGrade>()
						.eq(VipGrade::getCompanyId, (int) companyId)
						.select(VipGrade::getVipGradeId, VipGrade::getLvType)
						.last("LIMIT 100"));

		for (VipGrade row : existing) {
			String lvType = row.getLvType() == null ? "" : row.getLvType();
			switch (lvType) {
				case LV_TYPE_VIP -> {
					if (hasVip) {
						throw vipGradeExist();
					}
					hasVip = true;
				}
				case LV_TYPE_SVIP -> {
					if (hasSvip) {
						throw vipGradeExist();
					}
					hasSvip = true;
				}
				default -> { /* 忽略未知 lv_type */ }
			}
		}
		if (hasVip && hasSvip) {
			throw vipGradeLimit();
		}
		if (hasVip && !hasSvip) {
			return LV_TYPE_SVIP;
		}
		return LV_TYPE_VIP;
	}

	private static List<Map<String, Object>> buildPriceList(
			String monthlyFeeRaw, String quarterFeeRaw, String yearFeeRaw) {
		List<Map<String, Object>> list = new ArrayList<>(3);
		list.add(priceItem("monthly", feePriceOrNull(monthlyFeeRaw), "30", "30天"));
		list.add(priceItem("quarter", feePriceOrNull(quarterFeeRaw), "90", "90天"));
		list.add(priceItem("year", feePriceOrNull(yearFeeRaw), "365", "365天"));
		return list;
	}

	private static Object feePriceOrNull(String feeRaw) {
		if (feeRaw == null || feeRaw.isEmpty()) {
			return null;
		}
		return feeRaw;
	}

	private static Map<String, Object> priceItem(String name, Object price, String day, String desc) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("name", name);
		item.put("price", price);
		item.put("day", day);
		item.put("desc", desc);
		return item;
	}

	private static boolean parseWeakBoolean(String raw, int defaultWhenNull) {
		if (raw == null) {
			return defaultWhenNull != 0;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty() || "0".equals(trimmed) || "false".equalsIgnoreCase(trimmed)) {
			return false;
		}
		return true;
	}

	private static boolean isNumericBetween1And10Inclusive(String raw) {
		try {
			double v = Double.parseDouble(raw.trim());
			return v >= 1.0 && v <= 10.0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isNumericNonNegative(String raw) {
		try {
			double v = Double.parseDouble(raw.trim());
			return v >= 0.0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberV2FailException vipGradeLimit() {
		return new OpenapiMemberV2FailException(
				OpenapiErrorCode.MEMBER_VIP_GRADE_ERROR, "最多只能创建2个会员付费等级");
	}

	private static OpenapiMemberV2FailException vipGradeExist() {
		return new OpenapiMemberV2FailException(
				OpenapiErrorCode.MEMBER_VIP_GRADE_EXIST, "该类型下会员付费等级已存在");
	}
}
