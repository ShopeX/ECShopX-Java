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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OpenapiThirdApiV2MemberCardGradeCreateService {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MemberCardGradeMultiLangWriteService memberCardGradeMultiLangWriteService;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV2MemberCardGradeCreateService(
			MemberCardGradeMapper memberCardGradeMapper,
			MemberCardGradeMultiLangWriteService memberCardGradeMultiLangWriteService,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.memberCardGradeMultiLangWriteService = memberCardGradeMultiLangWriteService;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public Map<String, Object> executeOpenapiCreate(
			long companyId,
			String gradeNameRaw,
			String discountRaw,
			String totalConsumptionRaw,
			String backgroundPicUrlRaw,
			String externalIdRaw) {
		validateParams(gradeNameRaw, discountRaw, totalConsumptionRaw);
		checkUpperLimit(companyId);

		String gradeName = gradeNameRaw;
		String backgroundPicUrl = backgroundPicUrlRaw == null ? "" : backgroundPicUrlRaw;
		String externalId = externalIdRaw == null ? "" : externalIdRaw;

		Map<String, Object> privileges = new LinkedHashMap<>();
		privileges.put("discount_desc", discountRaw);
		privileges.put("discount", 100 - (int) (Double.parseDouble(discountRaw.trim()) * 10));

		Map<String, Object> promotionCondition = new LinkedHashMap<>();
		promotionCondition.put("total_consumption", totalConsumptionRaw);

		String privilegesJson;
		String promotionConditionJson;
		try {
			privilegesJson = objectMapper.writeValueAsString(privileges);
			promotionConditionJson = objectMapper.writeValueAsString(promotionCondition);
		} catch (JsonProcessingException e) {
			throw new ResourceException("等级数据序列化失败");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		MemberCardGrade entity = new MemberCardGrade();
		entity.setCompanyId(String.valueOf(companyId));
		entity.setGradeName(gradeName);
		entity.setDefaultGrade(Boolean.FALSE);
		entity.setBackgroundPicUrl(backgroundPicUrl);
		entity.setExternalId(externalId);
		entity.setPrivileges(privilegesJson);
		entity.setPromotionCondition(promotionConditionJson);
		entity.setCreated(now);
		entity.setUpdated(now);

		MemberCardGrade[] holder = new MemberCardGrade[1];
		transactionTemplate.executeWithoutResult(status -> {
			memberCardGradeMapper.insert(entity);
			holder[0] = entity;
			Map<String, Object> tempItem = buildTempItem(companyId, entity);
			memberCardGradeMultiLangWriteService.addOrUpdateDefaultLang(
					entity.getGradeId(), tempItem, companyId);
		});

		Map<String, Object> row = OpenapiMemberCardGradeOpenApiFormatSupport.toOverlayRow(holder[0]);
		return OpenapiMemberCardGradeOpenApiFormatSupport.formatOpenApiGradeRow(row, objectMapper);
	}

	private void validateParams(String gradeNameRaw, String discountRaw, String totalConsumptionRaw) {
		if (gradeNameRaw == null || gradeNameRaw.isEmpty()) {
			throw missingParams("未填写等级名称");
		}
		if (discountRaw == null || discountRaw.isEmpty()) {
			throw missingParams("未填写会员折扣");
		}
		if (totalConsumptionRaw == null || totalConsumptionRaw.isEmpty()) {
			throw missingParams("未填写升级条件");
		}
		if (!isNumericBetween1And10Inclusive(discountRaw)) {
			throw missingParams("请求参数错误");
		}
		if (!isNumericNonNegative(totalConsumptionRaw)) {
			throw missingParams("请求参数错误");
		}
	}

	private void checkUpperLimit(long companyId) {
		String companyIdStr = String.valueOf(companyId);
		long count = memberCardGradeMapper.selectCount(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr));
		if (count >= 6) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_GRADE_ERROR, "最多只能创建5个会员等级");
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

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}
}
