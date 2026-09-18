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
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OpenapiThirdApiV2MemberCardGradeUpdateService {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MemberCardGradeMultiLangWriteService memberCardGradeMultiLangWriteService;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV2MemberCardGradeUpdateService(
			MemberCardGradeMapper memberCardGradeMapper,
			MemberCardGradeMultiLangWriteService memberCardGradeMultiLangWriteService,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.memberCardGradeMultiLangWriteService = memberCardGradeMultiLangWriteService;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public Map<String, Object> executeOpenapiUpdate(
			long companyId,
			String gradeIdRaw,
			Optional<String> gradeNamePresent,
			Optional<String> discountPresent,
			Optional<String> totalConsumptionPresent,
			Optional<String> backgroundPicUrlPresent,
			Optional<String> externalIdPresent) {
		validateParams(gradeIdRaw, discountPresent, totalConsumptionPresent);

		String companyIdStr = String.valueOf(companyId);
		MemberCardGrade existing = memberCardGradeMapper.selectOne(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr)
						.eq(MemberCardGrade::getGradeId, gradeIdRaw));
		if (existing == null) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_GRADE_NOT_FOUND, "会员等级找不到");
		}

		PartialUpdateParams params;
		try {
			params = buildPartialUpdateParams(
					gradeNamePresent,
					discountPresent,
					totalConsumptionPresent,
					backgroundPicUrlPresent,
					externalIdPresent);
		} catch (JsonProcessingException e) {
			throw new ResourceException("等级数据序列化失败");
		}
		if (params.isEmpty()) {
			return null;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		MemberCardGrade entity = existing;

		if (params.gradeName != null) {
			entity.setGradeName(params.gradeName);
		}
		if (params.backgroundPicUrl != null) {
			entity.setBackgroundPicUrl(params.backgroundPicUrl);
		}
		if (params.externalId != null) {
			entity.setExternalId(params.externalId);
		}
		if (params.privilegesJson != null) {
			entity.setPrivileges(params.privilegesJson);
		}
		if (params.promotionConditionJson != null) {
			entity.setPromotionCondition(params.promotionConditionJson);
		}
		entity.setUpdated(now);

		MemberCardGrade[] holder = new MemberCardGrade[1];
		transactionTemplate.executeWithoutResult(status -> {
			memberCardGradeMapper.updateById(entity);
			holder[0] = entity;
			Map<String, Object> tempItem = buildTempItem(companyId, entity);
			memberCardGradeMultiLangWriteService.addOrUpdateDefaultLang(
					entity.getGradeId(), tempItem, companyId);
		});

		Map<String, Object> row = OpenapiMemberCardGradeOpenApiFormatSupport.toOverlayRow(holder[0]);
		return OpenapiMemberCardGradeOpenApiFormatSupport.formatOpenApiGradeRow(row, objectMapper);
	}

	private void validateParams(
			String gradeIdRaw,
			Optional<String> discountPresent,
			Optional<String> totalConsumptionPresent) {
		if (gradeIdRaw == null || gradeIdRaw.isEmpty()) {
			throw paramError();
		}
		if (discountPresent.isPresent()) {
			String raw = discountPresent.get();
			if (raw != null && !raw.isEmpty() && !isNumericBetween1And10Inclusive(raw)) {
				throw paramError();
			}
		}
		if (totalConsumptionPresent.isPresent()) {
			String raw = totalConsumptionPresent.get();
			if (raw != null && !raw.isEmpty() && !isNumericNonNegative(raw)) {
				throw paramError();
			}
		}
	}

	private PartialUpdateParams buildPartialUpdateParams(
			Optional<String> gradeNamePresent,
			Optional<String> discountPresent,
			Optional<String> totalConsumptionPresent,
			Optional<String> backgroundPicUrlPresent,
			Optional<String> externalIdPresent) throws JsonProcessingException {
		PartialUpdateParams out = new PartialUpdateParams();

		if (gradeNamePresent.isPresent() && gradeNamePresent.get() != null) {
			out.gradeName = gradeNamePresent.get();
		}
		if (backgroundPicUrlPresent.isPresent() && backgroundPicUrlPresent.get() != null) {
			out.backgroundPicUrl = backgroundPicUrlPresent.get();
		}
		if (externalIdPresent.isPresent() && externalIdPresent.get() != null) {
			out.externalId = externalIdPresent.get();
		}
		if (discountPresent.isPresent()) {
			String raw = discountPresent.get();
			if (raw != null && isNumeric(raw)) {
				Map<String, Object> privileges = new LinkedHashMap<>();
				privileges.put("discount_desc", raw);
				privileges.put("discount", 100 - (int) (Double.parseDouble(raw.trim()) * 10));
				out.privilegesJson = objectMapper.writeValueAsString(privileges);
			}
		}
		if (totalConsumptionPresent.isPresent()) {
			String raw = totalConsumptionPresent.get();
			if (raw != null && isNumeric(raw)) {
				Map<String, Object> promotionCondition = new LinkedHashMap<>();
				promotionCondition.put("total_consumption", raw);
				out.promotionConditionJson = objectMapper.writeValueAsString(promotionCondition);
			}
		}
		return out;
	}

	private static boolean isNumeric(String raw) {
		try {
			Double.parseDouble(raw.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
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

	private static Map<String, Object> buildTempItem(long companyId, MemberCardGrade e) {
		Map<String, Object> tempItem = new LinkedHashMap<>();
		tempItem.put("company_id", companyId);
		tempItem.put("grade_id", e.getGradeId());
		tempItem.put("grade_name", e.getGradeName());
		tempItem.put("default_grade", e.getDefaultGrade());
		tempItem.put("background_pic_url", e.getBackgroundPicUrl());
		tempItem.put("grade_background", e.getGradeBackground());
		tempItem.put("description", e.getDescription());
		tempItem.put("privileges", e.getPrivileges());
		tempItem.put("promotion_condition", e.getPromotionCondition());
		tempItem.put("created", e.getCreated());
		tempItem.put("updated", e.getUpdated());
		tempItem.put("third_data", e.getThirdData());
		tempItem.put("external_id", e.getExternalId());
		tempItem.put("dm_grade_code", e.getDmGradeCode());
		return tempItem;
	}

	private static OpenapiMemberV2FailException paramError() {
		return new OpenapiMemberV2FailException(
				OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误");
	}

	private static final class PartialUpdateParams {
		String gradeName;
		String backgroundPicUrl;
		String externalId;
		String privilegesJson;
		String promotionConditionJson;

		boolean isEmpty() {
			return gradeName == null
					&& backgroundPicUrl == null
					&& externalId == null
					&& privilegesJson == null
					&& promotionConditionJson == null;
		}
	}
}
