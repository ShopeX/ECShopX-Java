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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.MerchantSettlementMainCategoryPort;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorLoginMobileOccupancyPort;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MerchantUpdateMerchantService {

	private static final List<String> LANG_FIELD_KEYS = List.of("merchant_name");

	private final MerchantUpdateMerchantParamValidator merchantUpdateMerchantParamValidator;
	private final MerchantRepository merchantRepository;
	private final MerchantMapper merchantMapper;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MerchantOutsideLangWriteService merchantOutsideLangWriteService;
	private final MerchantSettlementMainCategoryPort mainCategoryPort;
	private final MerchantSettlementApplyDuplicateCheckService duplicateCheckService;
	private final MerchantOperatorLoginMobileOccupancyPort operatorLoginMobileOccupancyPort;

	public MerchantUpdateMerchantService(
			MerchantUpdateMerchantParamValidator merchantUpdateMerchantParamValidator,
			MerchantRepository merchantRepository,
			MerchantMapper merchantMapper,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MerchantOutsideLangWriteService merchantOutsideLangWriteService,
			MerchantSettlementMainCategoryPort mainCategoryPort,
			MerchantSettlementApplyDuplicateCheckService duplicateCheckService,
			MerchantOperatorLoginMobileOccupancyPort operatorLoginMobileOccupancyPort) {
		this.merchantUpdateMerchantParamValidator = merchantUpdateMerchantParamValidator;
		this.merchantRepository = merchantRepository;
		this.merchantMapper = merchantMapper;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.merchantOutsideLangWriteService = merchantOutsideLangWriteService;
		this.mainCategoryPort = mainCategoryPort;
		this.duplicateCheckService = duplicateCheckService;
		this.operatorLoginMobileOccupancyPort = operatorLoginMobileOccupancyPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void update(long merchantId, long companyId, Map<String, Object> params, String acceptLanguageHeader) {
		merchantUpdateMerchantParamValidator.validateAndNormalizeForUpdate(params);
		params.put("company_id", companyId);

		Merchant existing = merchantRepository.findById(merchantId);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}

		if (isSet(params, "main_category_selection")) {
			String json = mainCategoryPort.validateNormalizeAndSerialize(companyId, params.get("main_category_selection"));
			params.put("main_category_selection", json);
		}

		if (isSet(params, "contact_mobile")) {
			String mobilePlain = str(params.get("contact_mobile")).trim();
			Long applyId = existing.getSettlementApplyId();
			if (applyId != null) {
				duplicateCheckService.assertContactMobileNotDuplicateForWxappSave(companyId, applyId, mobilePlain);
			}
			operatorLoginMobileOccupancyPort.assertNotOccupiedByOtherMerchant(mobilePlain, merchantId);
			syncApplyMobile(companyId, applyId, mobilePlain);
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Merchant> w = new LambdaUpdateWrapper<>();
		w.eq(Merchant::getId, merchantId);

		setIfPresent(w, params, "settled_type", Merchant::getSettledType);
		setIfPresent(w, params, "merchant_name", Merchant::getMerchantName);
		setIfPresent(w, params, "contact_name", Merchant::getContactName);
		setIfPresent(w, params, "main_category_selection", Merchant::getMainCategorySelection);
		setIfPresent(w, params, "social_credit_code_id", Merchant::getSocialCreditCodeId);
		setIfPresent(w, params, "license_registered_address", Merchant::getLicenseRegisteredAddress);
		setIfPresent(w, params, "license_legal_representative", Merchant::getLicenseLegalRepresentative);
		if (isSet(params, "legal_cert_id")) {
			w.set(Merchant::getLegalCertId, sensitiveFieldEncryptor.encrypt(str(params.get("legal_cert_id"))));
		}
		setIfPresent(w, params, "email", Merchant::getEmail);
		setIfPresent(w, params, "bank_acct_type", Merchant::getBankAcctType);
		if (isSet(params, "card_id_mask")) {
			w.set(Merchant::getCardIdMask, sensitiveFieldEncryptor.encrypt(str(params.get("card_id_mask"))));
		}
		setIfPresent(w, params, "bank_name", Merchant::getBankName);
		if (isSet(params, "bank_mobile")) {
			w.set(Merchant::getBankMobile, sensitiveFieldEncryptor.encrypt(str(params.get("bank_mobile"))));
		}
		setIfPresent(w, params, "license_url", Merchant::getLicenseUrl);
		setIfPresent(w, params, "legal_certid_front_url", Merchant::getLegalCertidFrontUrl);
		setIfPresent(w, params, "legal_cert_id_back_url", Merchant::getLegalCertIdBackUrl);
		setIfPresent(w, params, "bank_card_front_url", Merchant::getBankCardFrontUrl);
		setIfPresent(w, params, "contract_url", Merchant::getContractUrl);
		if (isSet(params, "audit_goods")) {
			w.set(Merchant::isAuditGoods, booleanFrom(params.get("audit_goods")));
		}
		w.set(Merchant::getUpdated, now);

		int rows = merchantMapper.update(null, w);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> langData = new LinkedHashMap<>();
		for (String k : LANG_FIELD_KEYS) {
			if (params.containsKey(k) && params.get(k) != null) {
				langData.put(k, str(params.get(k)));
			}
		}
		if (!langData.isEmpty()) {
			merchantOutsideLangWriteService.applyAfterUpdate(merchantId, companyId, langData, acceptLanguageHeader);
		}
	}

	private void syncApplyMobile(long companyId, Long applyId, String mobilePlain) {
		if (applyId == null) {
			return;
		}
		LambdaUpdateWrapper<MerchantSettlementApply> uw = new LambdaUpdateWrapper<>();
		uw.eq(MerchantSettlementApply::getId, applyId)
				.eq(MerchantSettlementApply::getCompanyId, companyId)
				.set(MerchantSettlementApply::getMobile, mobilePlain);
		merchantSettlementApplyMapper.update(null, uw);
	}

	private static <T> void setIfPresent(
			LambdaUpdateWrapper<Merchant> w,
			Map<String, Object> params,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<Merchant, T> col) {
		if (isSet(params, key)) {
			w.set(col, str(params.get(key)));
		}
	}

	private static boolean isSet(Map<String, Object> params, String key) {
		return params.containsKey(key) && params.get(key) != null;
	}

	private static String str(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o);
	}

	private static boolean booleanFrom(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}
}
