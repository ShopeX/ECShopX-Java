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
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantSettlementApplyListQueryService {

	private static final List<String> DECRYPT_KEYS =
			List.of("legal_cert_id", "legal_mobile", "card_id_mask", "bank_mobile");

	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public MerchantSettlementApplyListQueryService(
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> query(long companyId, MerchantSettlementApplyListQueryInput input) {
		LambdaQueryWrapper<MerchantSettlementApply> w = new LambdaQueryWrapper<>();
		w.eq(MerchantSettlementApply::getCompanyId, companyId);
		if (StringUtils.hasText(input.auditStatus())) {
			w.eq(MerchantSettlementApply::getAuditStatus, input.auditStatus());
		}
		if (StringUtils.hasText(input.merchantName())) {
			w.like(MerchantSettlementApply::getMerchantName, "%" + input.merchantName() + "%");
		}
		if (StringUtils.hasText(input.province())) {
			w.eq(MerchantSettlementApply::getProvince, input.province());
		}
		if (StringUtils.hasText(input.city())) {
			w.eq(MerchantSettlementApply::getCity, input.city());
		}
		if (StringUtils.hasText(input.area())) {
			w.eq(MerchantSettlementApply::getArea, input.area());
		}
		if (StringUtils.hasText(input.settledType())) {
			w.eq(MerchantSettlementApply::getSettledType, input.settledType());
		}
		if (input.createdGte() != null) {
			w.ge(MerchantSettlementApply::getCreated, input.createdGte());
		}
		if (input.createdLte() != null) {
			w.le(MerchantSettlementApply::getCreated, input.createdLte());
		}
		w.orderByDesc(MerchantSettlementApply::getCreated);

		long total = merchantSettlementApplyMapper.selectCount(w);
		List<MerchantSettlementApply> records;
		if (total == 0) {
			records = Collections.emptyList();
		} else {
			Page<MerchantSettlementApply> pageReq = new Page<>(input.page(), input.pageSize(), false);
			merchantSettlementApplyMapper.selectPage(pageReq, w);
			records = pageReq.getRecords();
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (MerchantSettlementApply e : records) {
			Map<String, Object> row = toListRow(e);
			for (String k : DECRYPT_KEYS) {
				Object v = row.get(k);
				if (v instanceof String str && !str.isEmpty()) {
					row.put(k, sensitiveFieldEncryptor.decrypt(str));
				}
			}
			list.add(row);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("list", list);
		body.put("count", total);
		return body;
	}

	private static Map<String, Object> toListRow(MerchantSettlementApply m) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", m.getId());
		row.put("company_id", m.getCompanyId());
		row.put("mobile", m.getMobile());
		row.put("is_agree_agreement", m.isAgreeAgreement());
		row.put("merchant_type_id", m.getMerchantTypeId());
		row.put("settled_type", m.getSettledType());
		row.put("merchant_name", m.getMerchantName());
		row.put("social_credit_code_id", m.getSocialCreditCodeId());
		row.put("province", m.getProvince());
		row.put("city", m.getCity());
		row.put("area", m.getArea());
		row.put("regions_id", m.getRegionsId());
		row.put("address", m.getAddress());
		row.put("legal_name", m.getLegalName());
		row.put("legal_cert_id", m.getLegalCertId());
		row.put("legal_mobile", m.getLegalMobile());
		row.put("bank_acct_type", m.getBankAcctType());
		row.put("card_id_mask", m.getCardIdMask());
		row.put("bank_name", m.getBankName());
		row.put("bank_mobile", m.getBankMobile());
		row.put("license_url", m.getLicenseUrl());
		row.put("legal_certid_front_url", m.getLegalCertidFrontUrl());
		row.put("legal_cert_id_back_url", m.getLegalCertIdBackUrl());
		row.put("bank_card_front_url", m.getBankCardFrontUrl());
		row.put("audit_status", m.getAuditStatus());
		row.put("audit_memo", m.getAuditMemo());
		row.put("source", m.getSource());
		row.put("audit_goods", m.isAuditGoods());
		row.put("disabled", m.isDisabled());
		row.put("created", m.getCreated());
		row.put("updated", m.getUpdated());
		return row;
	}
}
