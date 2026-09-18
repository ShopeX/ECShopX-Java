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
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberCardVipGradeUpdateService {

	private final VipGradeMapper vipGradeMapper;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardVipGradeUpdateService(
			VipGradeMapper vipGradeMapper, ObjectMapper objectMapper) {
		this.vipGradeMapper = vipGradeMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiUpdate(
			long companyId,
			String vipGradeIdRaw,
			Optional<String> gradeNamePresent,
			Optional<String> monthlyFeePresent,
			Optional<String> quarterFeePresent,
			Optional<String> yearFeePresent,
			Optional<String> discountPresent,
			Optional<String> guideTitlePresent,
			Optional<String> descriptionPresent,
			Optional<String> isDefaultPresent,
			Optional<String> isDisabledPresent,
			Optional<String> externalIdPresent) {
		validateParams(vipGradeIdRaw, monthlyFeePresent, quarterFeePresent, yearFeePresent, discountPresent);

		Long vipGradeId = tryParseLong(vipGradeIdRaw.trim());
		VipGrade existing = vipGradeMapper.selectOne(
				new LambdaQueryWrapper<VipGrade>()
						.eq(VipGrade::getCompanyId, (int) companyId)
						.eq(VipGrade::getVipGradeId, vipGradeId != null ? vipGradeId : -1L));
		if (existing == null) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_VIP_GRADE_NOT_FOUND, "会员付费等级找不到");
		}

		VipGrade entity = existing;

		if (gradeNamePresent.isPresent()
				&& gradeNamePresent.get() != null
				&& !gradeNamePresent.get().isEmpty()) {
			entity.setGradeName(gradeNamePresent.get());
		}
		if (guideTitlePresent.isPresent()
				&& guideTitlePresent.get() != null
				&& !guideTitlePresent.get().isEmpty()) {
			entity.setGuideTitle(guideTitlePresent.get());
		}
		if (descriptionPresent.isPresent()
				&& descriptionPresent.get() != null
				&& !descriptionPresent.get().isEmpty()) {
			entity.setDescription(descriptionPresent.get());
		}
		if (isDefaultPresent.isPresent() && isDefaultPresent.get() != null) {
			entity.setIsDefault(parseWeakBoolean(isDefaultPresent.get(), 0));
		}
		if (isDisabledPresent.isPresent() && isDisabledPresent.get() != null) {
			entity.setIsDisabled(parseWeakBoolean(isDisabledPresent.get(), 0));
		}

		String mergedPriceListJson;
		try {
			mergedPriceListJson = mergePriceListJson(
					entity.getPriceList(), monthlyFeePresent, quarterFeePresent, yearFeePresent);
		} catch (JsonProcessingException e) {
			throw new ResourceException("付费等级数据序列化失败");
		}
		if (mergedPriceListJson != null
				&& !mergedPriceListJson.isEmpty()
				&& !"[]".equals(mergedPriceListJson)) {
			entity.setPriceList(mergedPriceListJson);
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setUpdated(now);
		vipGradeMapper.updateById(entity);

		return OpenapiMemberCardVipGradeOpenApiFormatSupport.formatOpenApiVipGradeRow(entity, objectMapper);
	}

	private void validateParams(
			String vipGradeIdRaw,
			Optional<String> monthlyFeePresent,
			Optional<String> quarterFeePresent,
			Optional<String> yearFeePresent,
			Optional<String> discountPresent) {
		if (vipGradeIdRaw == null || vipGradeIdRaw.isEmpty()) {
			throw paramError();
		}
		validatePresentFee(monthlyFeePresent);
		validatePresentFee(quarterFeePresent);
		validatePresentFee(yearFeePresent);
		if (discountPresent.isPresent()) {
			String raw = discountPresent.get();
			if (raw != null && !raw.isEmpty() && !isNumericBetween1And10Inclusive(raw)) {
				throw paramError();
			}
		}
	}

	private static void validatePresentFee(Optional<String> feePresent) {
		if (!feePresent.isPresent()) {
			return;
		}
		String raw = feePresent.get();
		if (raw == null || raw.isEmpty()) {
			return;
		}
		if (!isNumericNonNegative(raw)) {
			throw paramError();
		}
	}

	private String mergePriceListJson(
			String existingPriceListJson,
			Optional<String> monthlyFeePresent,
			Optional<String> quarterFeePresent,
			Optional<String> yearFeePresent) throws JsonProcessingException {
		Map<String, Map<String, Object>> priceListByName = indexPriceListByName(
				OpenapiMemberCardVipGradeOpenApiFormatSupport.parseJsonArray(
						existingPriceListJson, objectMapper));

		applyFeeUpdate(priceListByName, "monthly", monthlyFeePresent, "30", "30天");
		applyFeeUpdate(priceListByName, "quarter", quarterFeePresent, "90", "90天");
		applyFeeUpdate(priceListByName, "year", yearFeePresent, "365", "365天");

		List<Map<String, Object>> values = new ArrayList<>(priceListByName.values());
		if (values.isEmpty()) {
			return existingPriceListJson;
		}
		return objectMapper.writeValueAsString(values);
	}

	private static void applyFeeUpdate(
			Map<String, Map<String, Object>> priceListByName,
			String name,
			Optional<String> feePresent,
			String day,
			String desc) {
		if (!feePresent.isPresent()) {
			return;
		}
		String raw = feePresent.get();
		if (raw == null || raw.isEmpty()) {
			return;
		}
		if (priceListByName.containsKey(name)) {
			priceListByName.get(name).put("price", raw);
		} else {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("name", name);
			item.put("price", raw);
			item.put("day", day);
			item.put("desc", desc);
			priceListByName.put(name, item);
		}
	}

	private static Map<String, Map<String, Object>> indexPriceListByName(
			List<Map<String, Object>> items) {
		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> item : items) {
			Object name = item.get("name");
			if (name != null) {
				out.put(String.valueOf(name), new LinkedHashMap<>(item));
			}
		}
		return out;
	}

	private static Long tryParseLong(String raw) {
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			try {
				double d = Double.parseDouble(raw);
				if (d == Math.floor(d)) {
					return (long) d;
				}
			} catch (NumberFormatException ignored) {
				// fall through
			}
			return null;
		}
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

	private static OpenapiMemberV2FailException paramError() {
		return new OpenapiMemberV2FailException(
				OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误");
	}
}
