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
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantListQueryService {

	private static final List<String> DECRYPT_KEYS = List.of("legal_cert_id", "bank_mobile");

	private final MerchantRepository merchantRepository;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MerchantOutsideLangReadService merchantOutsideLangReadService;

	public MerchantListQueryService(
			MerchantRepository merchantRepository,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MerchantOutsideLangReadService merchantOutsideLangReadService) {
		this.merchantRepository = merchantRepository;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.merchantOutsideLangReadService = merchantOutsideLangReadService;
	}

	public Map<String, Object> query(long companyId, Map<String, Object> validatedParams, String langTag) {
		int page = MerchantListParamValidator.pageFromValidatedMap(validatedParams);
		int pageSize = MerchantListParamValidator.pageSizeFromValidatedMap(validatedParams);

		String merchantName = MerchantListParamValidator.stringFilterFromMap(validatedParams, "merchant_name");
		String contactNamePlain = MerchantListParamValidator.stringFilterFromMap(validatedParams, "contact_name");
		String contactMobile = MerchantListParamValidator.stringFilterFromMap(validatedParams, "contact_mobile");
		Integer gte = MerchantListParamValidator.createdGteFromMap(validatedParams);
		Integer lte = MerchantListParamValidator.createdLteFromMap(validatedParams);

		MerchantListFilter filter =
				new MerchantListFilter(merchantName, contactNamePlain, contactMobile, gte, lte);

		long total = merchantRepository.countForList(companyId, filter);
		long offset = (long) (page - 1) * pageSize;
		List<Merchant> entities =
				total == 0 ? List.of() : merchantRepository.selectPageForList(companyId, filter, offset, pageSize);

		Map<Long, String> contactMobileByApplyId = loadContactMobiles(entities);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Merchant m : entities) {
			Map<String, Object> row = toListRow(m);
			if (m.getSettlementApplyId() != null) {
				String contactMobileValue = contactMobileByApplyId.get(m.getSettlementApplyId());
				if (contactMobileValue != null) {
					row.put("contact_mobile", contactMobileValue);
				}
			}
			for (String k : DECRYPT_KEYS) {
				Object v = row.get(k);
				if (v instanceof String str && !str.isEmpty()) {
					row.put(k, sensitiveFieldEncryptor.decrypt(str));
				}
			}
			merchantOutsideLangReadService.applyMerchantRow(m.getCompanyId(), m.getId(), langTag, row);
			list.add(row);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", total);
		body.put("list", list);
		return body;
	}

	private Map<Long, String> loadContactMobiles(List<Merchant> entities) {
		LinkedHashSet<Long> applyIds = new LinkedHashSet<>();
		for (Merchant m : entities) {
			if (m.getSettlementApplyId() != null) {
				applyIds.add(m.getSettlementApplyId());
			}
		}
		if (applyIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> result = new LinkedHashMap<>();
		for (MerchantSettlementApply apply : merchantSettlementApplyMapper.selectBatchIds(applyIds)) {
			if (StringUtils.hasText(apply.getMobile())) {
				result.put(apply.getId(), apply.getMobile());
			}
		}
		return result;
	}

	private static Map<String, Object> toListRow(Merchant m) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", m.getId());
		row.put("company_id", m.getCompanyId());
		row.put("settlement_apply_id", m.getSettlementApplyId());
		row.put("merchant_name", m.getMerchantName());
		row.put("settled_type", m.getSettledType());
		row.put("social_credit_code_id", m.getSocialCreditCodeId());
		row.put("contact_name", m.getContactName());
		row.put("license_registered_address", m.getLicenseRegisteredAddress());
		row.put("license_legal_representative", m.getLicenseLegalRepresentative());
		row.put("legal_cert_id", m.getLegalCertId());
		row.put("email", m.getEmail());
		row.put("bank_acct_type", m.getBankAcctType());
		row.put("card_id_mask", m.getCardIdMask());
		row.put("bank_name", m.getBankName());
		row.put("bank_mobile", m.getBankMobile());
		row.put("license_url", m.getLicenseUrl());
		row.put("legal_certid_front_url", m.getLegalCertidFrontUrl());
		row.put("legal_cert_id_back_url", m.getLegalCertIdBackUrl());
		row.put("bank_card_front_url", m.getBankCardFrontUrl());
		row.put("contract_url", m.getContractUrl());
		row.put("settled_succ_sendsms", m.getSettledSuccSendsms());
		row.put("audit_goods", m.isAuditGoods());
		row.put("source", m.getSource());
		row.put("disabled", m.isDisabled());
		row.put("created", m.getCreated());
		row.put("updated", m.getUpdated());
		return row;
	}
}
