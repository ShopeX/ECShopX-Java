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

import cn.shopex.ecshopx.common.dispatch.MerchantEnterSuccessNoticeDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.port.MerchantMainOperatorProvisioner;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantCreateService {

	private final MerchantCreateParamValidator merchantCreateParamValidator;
	private final MerchantTypeCheckService merchantTypeCheckService;
	private final MerchantSettlementApplyDuplicateCheckService merchantSettlementApplyDuplicateCheckService;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantMapper merchantMapper;
	private final MerchantOutsideLangWriteService merchantOutsideLangWriteService;
	private final MerchantMainOperatorProvisioner merchantMainOperatorProvisioner;
	private final MerchantEnterSuccessNoticeDispatchPublisher merchantEnterSuccessNoticePublisher;

	public MerchantCreateService(
			MerchantCreateParamValidator merchantCreateParamValidator,
			MerchantTypeCheckService merchantTypeCheckService,
			MerchantSettlementApplyDuplicateCheckService merchantSettlementApplyDuplicateCheckService,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantMapper merchantMapper,
			MerchantOutsideLangWriteService merchantOutsideLangWriteService,
			MerchantMainOperatorProvisioner merchantMainOperatorProvisioner,
			MerchantEnterSuccessNoticeDispatchPublisher merchantEnterSuccessNoticePublisher) {
		this.merchantCreateParamValidator = merchantCreateParamValidator;
		this.merchantTypeCheckService = merchantTypeCheckService;
		this.merchantSettlementApplyDuplicateCheckService = merchantSettlementApplyDuplicateCheckService;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantMapper = merchantMapper;
		this.merchantOutsideLangWriteService = merchantOutsideLangWriteService;
		this.merchantMainOperatorProvisioner = merchantMainOperatorProvisioner;
		this.merchantEnterSuccessNoticePublisher = merchantEnterSuccessNoticePublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createMerchant(Map<String, Object> params, String acceptLanguageHeader) {
		try {
			merchantCreateParamValidator.validateAndNormalizeRegions(params);
			normalizeAuditGoodsBoolean(params);
			checkServiceLayerParams(params);

			long companyId = toLong(params.get("company_id"));
			long merchantTypeId = toLong(params.get("merchant_type_id"));
			merchantTypeCheckService.checkMerchantType(companyId, merchantTypeId);

			String mobilePlain = String.valueOf(params.get("mobile")).trim();
			merchantSettlementApplyDuplicateCheckService.assertMobileNotUsedInApply(mobilePlain);

			Map<String, Object> applyRow = buildApplyRow(params);
			MerchantSettlementApply apply = toSettlementApplyEntity(applyRow);
			merchantSettlementApplyMapper.insert(apply);
			Long settlementApplyId = apply.getId();
			if (settlementApplyId == null) {
				throw new ResourceException("提交失败，请重试");
			}
			params.put("settlement_apply_id", settlementApplyId);

			Merchant merchant = toMerchantEntity(params, companyId, settlementApplyId);
			merchantMapper.insert(merchant);
			Long merchantId = merchant.getId();
			if (merchantId == null) {
				throw new ResourceException("提交失败，请重试");
			}

			Map<String, Object> langSource = new LinkedHashMap<>(params);
			merchantOutsideLangWriteService.applyAfterInsert(merchantId, companyId, langSource, acceptLanguageHeader);

			String operatorPassword = null;
			if ("1".equals(MerchantCreateParamNormalizer.normalizeScalarToString(params.get("settled_succ_sendsms")))) {
				operatorPassword = merchantMainOperatorProvisioner.createMainMerchantOperator(
						companyId, mobilePlain, mobilePlain, merchantId);
				merchantEnterSuccessNoticePublisher.publish(companyId, mobilePlain, operatorPassword);
			}

			Map<String, Object> body = new HashMap<>(2);
			body.put("mobile", mobilePlain);
			if ("1".equals(MerchantCreateParamNormalizer.normalizeScalarToString(params.get("settled_succ_sendsms")))) {
				body.put("password", operatorPassword);
			} else {
				body.put("password", "确认协议后显示");
			}
			return body;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage();
			throw new ResourceException(msg != null && !msg.isEmpty() ? msg : "操作失败");
		}
	}

	private static void normalizeAuditGoodsBoolean(Map<String, Object> params) {
		String n = MerchantCreateParamNormalizer.normalizeScalarToString(params.get("audit_goods"));
		params.put("audit_goods", !"false".equals(n));
	}

	private void checkServiceLayerParams(Map<String, Object> params) {
		String legalMobile = String.valueOf(params.get("legal_mobile")).trim();
		if (!MerchantCreateParamValidator.isCnMobile(legalMobile)) {
			throw new ResourceException("手机号格式不正确，请确认后再重试");
		}
		String mobile = String.valueOf(params.get("mobile")).trim();
		if (!MerchantCreateParamValidator.isCnMobile(mobile)) {
			throw new ResourceException("生成账号的手机号格式不正确，请确认后再重试");
		}
	}

	private static Map<String, Object> buildApplyRow(Map<String, Object> params) {
		Map<String, Object> row = new LinkedHashMap<>(params);
		row.remove("settled_succ_sendsms");
		row.remove("email");
		row.put(
				"is_agree_agreement",
				"1".equals(MerchantCreateParamNormalizer.normalizeScalarToString(params.get("settled_succ_sendsms"))));
		row.put("audit_status", "2");
		return row;
	}

	private static MerchantSettlementApply toSettlementApplyEntity(Map<String, Object> row) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		MerchantSettlementApply a = new MerchantSettlementApply();
		a.setCompanyId(toLong(row.get("company_id")));
		a.setMobile(str(row.get("mobile")));
		a.setAgreeAgreement(booleanFromRow(row.get("is_agree_agreement")));
		a.setMerchantTypeId(toLong(row.get("merchant_type_id")));
		a.setSettledType(str(row.get("settled_type")));
		a.setMerchantName(str(row.get("merchant_name")));
		a.setSocialCreditCodeId(str(row.get("social_credit_code_id")));
		a.setProvince(str(row.get("province")));
		a.setCity(str(row.get("city")));
		a.setArea(str(row.get("area")));
		a.setRegionsId(str(row.get("regions_id")));
		a.setAddress(str(row.get("address")));
		a.setLegalName(str(row.get("legal_name")));
		a.setLegalCertId(str(row.get("legal_cert_id")));
		a.setLegalMobile(str(row.get("legal_mobile")));
		a.setBankAcctType(str(row.get("bank_acct_type")));
		a.setCardIdMask(str(row.get("card_id_mask")));
		a.setBankName(str(row.get("bank_name")));
		a.setBankMobile(str(row.get("bank_mobile")));
		a.setLicenseUrl(str(row.get("license_url")));
		a.setLegalCertidFrontUrl(str(row.get("legal_certid_front_url")));
		a.setLegalCertIdBackUrl(str(row.get("legal_cert_id_back_url")));
		a.setBankCardFrontUrl(str(row.get("bank_card_front_url")));
		a.setAuditStatus(str(row.get("audit_status")));
		a.setSource(str(row.get("source")));
		a.setAuditGoods(booleanFromRow(row.get("audit_goods")));
		a.setDisabled(booleanFromRow(row.get("disabled")));
		a.setCreated(now);
		a.setUpdated(now);
		return a;
	}

	private static Merchant toMerchantEntity(Map<String, Object> params, long companyId, long settlementApplyId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		Merchant m = new Merchant();
		m.setCompanyId(companyId);
		m.setSettlementApplyId(settlementApplyId);
		m.setMerchantName(str(params.get("merchant_name")));
		m.setMerchantTypeId(toLong(params.get("merchant_type_id")));
		m.setSettledType(str(params.get("settled_type")));
		m.setSocialCreditCodeId(str(params.get("social_credit_code_id")));
		m.setProvince(str(params.get("province")));
		m.setCity(str(params.get("city")));
		m.setArea(str(params.get("area")));
		m.setRegionsId(str(params.get("regions_id")));
		m.setAddress(str(params.get("address")));
		m.setLegalName(str(params.get("legal_name")));
		m.setLegalCertId(str(params.get("legal_cert_id")));
		m.setLegalMobile(str(params.get("legal_mobile")));
		m.setEmail(str(params.get("email")));
		m.setBankAcctType(str(params.get("bank_acct_type")));
		m.setCardIdMask(str(params.get("card_id_mask")));
		m.setBankName(str(params.get("bank_name")));
		m.setBankMobile(str(params.get("bank_mobile")));
		m.setLicenseUrl(str(params.get("license_url")));
		m.setLegalCertidFrontUrl(str(params.get("legal_certid_front_url")));
		m.setLegalCertIdBackUrl(str(params.get("legal_cert_id_back_url")));
		m.setBankCardFrontUrl(str(params.get("bank_card_front_url")));
		m.setContractUrl(str(params.get("contract_url")));
		m.setSettledSuccSendsms(
				MerchantCreateParamNormalizer.normalizeScalarToString(params.get("settled_succ_sendsms")));
		m.setAuditGoods(booleanFromRow(params.get("audit_goods")));
		m.setSource(str(params.get("source")));
		m.setDisabled(booleanFromRow(params.get("disabled")));
		m.setCreated(now);
		m.setUpdated(now);
		return m;
	}

	private static String str(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o).trim());
	}

	private static boolean booleanFromRow(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}
}
