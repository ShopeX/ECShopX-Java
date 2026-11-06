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

package cn.shopex.ecshopx.merchant.service.wxapp;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.MerchantSettlementMainCategoryPort;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorLoginMobileOccupancyPort;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import cn.shopex.ecshopx.merchant.service.MerchantCreateParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyDuplicateCheckService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantWxappSettlementApplySaveService {

	private static final String AUDIT_ONGOING = "1";
	private static final String AUDIT_SUCC = "2";
	private static final String AUDIT_FAIL = "3";

	private final MerchantWxappSettlementApplySaveParamValidator validator;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final MerchantSettlementApplyProgressService progressService;
	private final MerchantSettlementMainCategoryPort mainCategoryPort;
	private final MerchantSettlementApplyDuplicateCheckService duplicateCheck;
	private final MerchantOperatorLoginMobileOccupancyPort operatorLoginMobileOccupancyPort;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public MerchantWxappSettlementApplySaveService(
			MerchantWxappSettlementApplySaveParamValidator validator,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			MerchantSettlementApplyProgressService progressService,
			MerchantSettlementMainCategoryPort mainCategoryPort,
			MerchantSettlementApplyDuplicateCheckService duplicateCheck,
			MerchantOperatorLoginMobileOccupancyPort operatorLoginMobileOccupancyPort,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.validator = validator;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.progressService = progressService;
		this.mainCategoryPort = mainCategoryPort;
		this.duplicateCheck = duplicateCheck;
		this.operatorLoginMobileOccupancyPort = operatorLoginMobileOccupancyPort;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public void save(MerchantWxappAuthAttributes auth, String step, Map<String, Object> mergedParams) {
		validator.validate(step, mergedParams);
		long companyId = auth.companyId();
		long accountId = auth.accountId();
		Map<String, Object> params = new LinkedHashMap<>(mergedParams);
		params.put("company_id", companyId);

		MerchantSettlementApply info = merchantSettlementApplyMapper.selectById(accountId);
		if (info == null) {
			throw new ResourceException("未查询到相关数据");
		}
		if (AUDIT_SUCC.equals(info.getAuditStatus())) {
			throw new ResourceException("入驻申请已经审核通过，不能再修改");
		}
		Map<String, Object> setting = merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
		if (Boolean.FALSE.equals(setting.get("status"))) {
			throw new ResourceException("该平台现不支持商户入驻，请核实后再试");
		}
		int cur = progressService.resolveProgressStepWithSideEffects(companyId, accountId);
		int stepInt = Integer.parseInt(step.trim());
		if (stepInt > cur) {
			throw new ResourceException("入驻信息填写步骤错误");
		}

		String mainCategoryJson = null;
		switch (step) {
			case "1" -> {
				String settledType = str(params.get("settled_type")).trim();
				@SuppressWarnings("unchecked")
				java.util.List<String> allowed = (java.util.List<String>) setting.get("settled_type");
				if (allowed == null || !allowed.contains(settledType)) {
					throw new ResourceException("入驻类型错误，请确认后重新提交");
				}
				mainCategoryJson =
						mainCategoryPort.validateNormalizeAndSerialize(companyId, params.get("main_category_selection"));
			}
			case "2" -> {
				String contactMobile = str(params.get("contact_mobile")).trim();
				if (!MerchantCreateParamValidator.isCnMobile(contactMobile)) {
					throw new ResourceException("请填写正确的手机号码");
				}
				String social = str(params.get("social_credit_code_id")).trim();
				duplicateCheck.assertSocialCreditNotDuplicateForWxappSave(companyId, accountId, social);
				duplicateCheck.assertContactMobileNotDuplicateForWxappSave(companyId, accountId, contactMobile);
				operatorLoginMobileOccupancyPort.assertNotOccupiedByOtherMerchant(contactMobile, 0L);
			}
			case "3" -> {
				params.put("disabled", false);
				if (AUDIT_FAIL.equals(info.getAuditStatus())) {
					params.put("audit_status", AUDIT_ONGOING);
				}
			}
			default -> throw new ResourceException("入驻信息填写步骤错误");
		}

		if (AUDIT_ONGOING.equals(info.getAuditStatus()) && ("1".equals(step) || "2".equals(step))) {
			params.put("disabled", true);
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<MerchantSettlementApply> uw = new LambdaUpdateWrapper<>();
		uw.eq(MerchantSettlementApply::getId, accountId).eq(MerchantSettlementApply::getCompanyId, companyId);

		switch (step) {
			case "1" -> {
				uw.set(MerchantSettlementApply::getSettledType, str(params.get("settled_type")).trim())
						.set(MerchantSettlementApply::getMainCategorySelection, mainCategoryJson);
			}
			case "2" -> {
				setPlain(uw, MerchantSettlementApply::getLicenseRegisteredAddress, params.get("license_registered_address"));
				setPlain(uw, MerchantSettlementApply::getLicenseLegalRepresentative, params.get("license_legal_representative"));
				setPlain(uw, MerchantSettlementApply::getMerchantName, params.get("merchant_name"));
				setPlain(uw, MerchantSettlementApply::getSocialCreditCodeId, params.get("social_credit_code_id"));
				setPlain(uw, MerchantSettlementApply::getContactName, params.get("contact_name"));
				uw.set(MerchantSettlementApply::getMobile, str(params.get("contact_mobile")).trim());
				setEncryptedIfPresent(uw, MerchantSettlementApply::getLegalCertId, params.get("legal_cert_id"));
				setPlain(uw, MerchantSettlementApply::getLicenseUrl, params.get("license_url"));
				setPlain(uw, MerchantSettlementApply::getLegalCertidFrontUrl, params.get("legal_certid_front_url"));
				setPlain(uw, MerchantSettlementApply::getLegalCertIdBackUrl, params.get("legal_cert_id_back_url"));
				setPlain(uw, MerchantSettlementApply::getBankAcctType, params.get("bank_acct_type"));
				setEncryptedIfPresent(uw, MerchantSettlementApply::getCardIdMask, params.get("card_id_mask"));
				setPlain(uw, MerchantSettlementApply::getBankName, params.get("bank_name"));
				setEncryptedIfPresent(uw, MerchantSettlementApply::getBankMobile, params.get("bank_mobile"));
				if (params.containsKey("bank_card_front_url")) {
					setPlain(uw, MerchantSettlementApply::getBankCardFrontUrl, params.get("bank_card_front_url"));
				}
			}
			case "3" -> {
				uw.set(MerchantSettlementApply::isDisabled, false);
				if (params.containsKey("audit_status")) {
					uw.set(MerchantSettlementApply::getAuditStatus, str(params.get("audit_status")));
				}
			}
			default -> throw new ResourceException("入驻信息填写步骤错误");
		}

		if (params.containsKey("disabled")) {
			Object d = params.get("disabled");
			boolean dis = d instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(d));
			uw.set(MerchantSettlementApply::isDisabled, dis);
		}
		uw.set(MerchantSettlementApply::getUpdated, now);

		int rows = merchantSettlementApplyMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到相关数据");
		}
	}

	private void setPlain(
			LambdaUpdateWrapper<MerchantSettlementApply> uw,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<MerchantSettlementApply, ?> col,
			Object v) {
		uw.set(col, str(v));
	}

	private void setEncryptedIfPresent(
			LambdaUpdateWrapper<MerchantSettlementApply> uw,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<MerchantSettlementApply, ?> col,
			Object v) {
		String plain = str(v).trim();
		if (!StringUtils.hasText(plain)) {
			uw.set(col, plain);
			return;
		}
		uw.set(col, sensitiveFieldEncryptor.encrypt(plain));
	}

	private static String str(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof JsonNode n && n.isTextual()) {
			return n.asText();
		}
		return String.valueOf(o);
	}
}
