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
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import cn.shopex.ecshopx.merchant.service.MerchantCreateParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyDuplicateCheckService;
import cn.shopex.ecshopx.merchant.service.MerchantTypeCheckService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
	private final MerchantTypeCheckService merchantTypeCheckService;
	private final MerchantSettlementApplyDuplicateCheckService duplicateCheck;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public MerchantWxappSettlementApplySaveService(
			MerchantWxappSettlementApplySaveParamValidator validator,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			MerchantSettlementApplyProgressService progressService,
			MerchantTypeCheckService merchantTypeCheckService,
			MerchantSettlementApplyDuplicateCheckService duplicateCheck,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.validator = validator;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.progressService = progressService;
		this.merchantTypeCheckService = merchantTypeCheckService;
		this.duplicateCheck = duplicateCheck;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public void save(MerchantWxappAuthAttributes auth, String step, Map<String, Object> mergedParams) {
		validator.validate(step, mergedParams);
		long companyId = auth.companyId();
		long accountId = auth.accountId();
		Map<String, Object> params = new LinkedHashMap<>(mergedParams);
		params.put("company_id", companyId);

		if ("2".equals(step)) {
			if (params.containsKey("regions_id") && params.containsKey("regions")) {
				expandRegionsToProvinceCityArea(params);
			}
			normalizeRegionsIdForDb(params);
		}

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

		switch (step) {
			case "1" -> {
				String settledType = str(params.get("settled_type")).trim();
				@SuppressWarnings("unchecked")
				List<String> allowed = (List<String>) setting.get("settled_type");
				if (allowed == null || !allowed.contains(settledType)) {
					throw new ResourceException("入驻类型错误，请确认后重新提交");
				}
				long merchantTypeId = toPositiveLong(params.get("merchant_type_id"));
				merchantTypeCheckService.checkMerchantType(companyId, merchantTypeId);
			}
			case "2" -> {
				String legalMobile = str(params.get("legal_mobile")).trim();
				if (!MerchantCreateParamValidator.isCnMobile(legalMobile)) {
					throw new ResourceException("请填写正确的手机号码");
				}
				String social = str(params.get("social_credit_code_id")).trim();
				duplicateCheck.assertSocialCreditNotDuplicateForWxappSave(companyId, accountId, social);
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
				uw.set(MerchantSettlementApply::getMerchantTypeId, toPositiveLong(params.get("merchant_type_id")))
						.set(MerchantSettlementApply::getSettledType, str(params.get("settled_type")).trim());
			}
			case "2" -> {
				setPlain(uw, MerchantSettlementApply::getMerchantName, params.get("merchant_name"));
				setPlain(uw, MerchantSettlementApply::getSocialCreditCodeId, params.get("social_credit_code_id"));
				setPlain(uw, MerchantSettlementApply::getRegionsId, params.get("regions_id"));
				setPlain(uw, MerchantSettlementApply::getProvince, params.get("province"));
				setPlain(uw, MerchantSettlementApply::getCity, params.get("city"));
				setPlain(uw, MerchantSettlementApply::getArea, params.get("area"));
				setPlain(uw, MerchantSettlementApply::getAddress, params.get("address"));
				setPlain(uw, MerchantSettlementApply::getLegalName, params.get("legal_name"));
				setEncryptedIfPresent(uw, MerchantSettlementApply::getLegalCertId, params.get("legal_cert_id"));
				setEncryptedIfPresent(uw, MerchantSettlementApply::getLegalMobile, params.get("legal_mobile"));
				if (params.containsKey("bank_acct_type")) {
					setPlain(uw, MerchantSettlementApply::getBankAcctType, params.get("bank_acct_type"));
				}
				if (params.containsKey("card_id_mask")) {
					setEncryptedIfPresent(uw, MerchantSettlementApply::getCardIdMask, params.get("card_id_mask"));
				}
				if (params.containsKey("bank_name")) {
					setPlain(uw, MerchantSettlementApply::getBankName, params.get("bank_name"));
				}
				if (params.containsKey("bank_mobile")) {
					setEncryptedIfPresent(uw, MerchantSettlementApply::getBankMobile, params.get("bank_mobile"));
				}
			}
			case "3" -> {
				setPlain(uw, MerchantSettlementApply::getLicenseUrl, params.get("license_url"));
				setPlain(uw, MerchantSettlementApply::getLegalCertidFrontUrl, params.get("legal_certid_front_url"));
				setPlain(uw, MerchantSettlementApply::getLegalCertIdBackUrl, params.get("legal_cert_id_back_url"));
				if (params.containsKey("bank_card_front_url")) {
					setPlain(uw, MerchantSettlementApply::getBankCardFrontUrl, params.get("bank_card_front_url"));
				}
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

	private void expandRegionsToProvinceCityArea(Map<String, Object> params) {
		List<?> list = normalizeRegionsToList(params.get("regions"));
		if (list == null) {
			return;
		}
		String[] keys = {"province", "city", "area"};
		for (int i = 0; i < keys.length && i < list.size(); i++) {
			Object item = list.get(i);
			if (item != null) {
				String t = String.valueOf(item).trim();
				params.put(keys[i], t);
			}
		}
	}

	private List<?> normalizeRegionsToList(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof JsonNode n) {
			if (n.isArray()) {
				List<Object> out = new ArrayList<>();
				for (JsonNode x : n) {
					out.add(x.isValueNode() ? x.asText() : x.toString());
				}
				return out;
			}
			return null;
		}
		if (raw instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (raw instanceof Object[] a) {
			return Arrays.asList(a);
		}
		return List.of(raw);
	}

	private void normalizeRegionsIdForDb(Map<String, Object> params) {
		Object rid = params.get("regions_id");
		if (rid == null) {
			return;
		}
		if (rid instanceof String s) {
			params.put("regions_id", s.trim());
			return;
		}
		try {
			params.put("regions_id", objectMapper.writeValueAsString(rid));
		} catch (JsonProcessingException e) {
			throw new ResourceException("区域必填");
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

	private static long toPositiveLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
