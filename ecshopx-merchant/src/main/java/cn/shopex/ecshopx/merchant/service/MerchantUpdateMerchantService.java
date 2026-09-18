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
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantUpdateMerchantService {

	private static final List<String> LANG_FIELD_KEYS =
			List.of("province", "city", "area", "address", "legal_name");

	private final MerchantUpdateMerchantParamValidator merchantUpdateMerchantParamValidator;
	private final MerchantRepository merchantRepository;
	private final MerchantMapper merchantMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MerchantOutsideLangWriteService merchantOutsideLangWriteService;
	private final ObjectMapper objectMapper;

	public MerchantUpdateMerchantService(
			MerchantUpdateMerchantParamValidator merchantUpdateMerchantParamValidator,
			MerchantRepository merchantRepository,
			MerchantMapper merchantMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MerchantOutsideLangWriteService merchantOutsideLangWriteService,
			ObjectMapper objectMapper) {
		this.merchantUpdateMerchantParamValidator = merchantUpdateMerchantParamValidator;
		this.merchantRepository = merchantRepository;
		this.merchantMapper = merchantMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.merchantOutsideLangWriteService = merchantOutsideLangWriteService;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void update(long merchantId, long companyId, Map<String, Object> params, String acceptLanguageHeader) {
		merchantUpdateMerchantParamValidator.validateAndNormalizeForUpdate(params);
		processRegions(params);
		params.put("company_id", companyId);

		Merchant existing = merchantRepository.findById(merchantId);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Merchant> w = new LambdaUpdateWrapper<>();
		w.eq(Merchant::getId, merchantId);

		if (isSet(params, "company_id")) {
			w.set(Merchant::getCompanyId, toLong(params.get("company_id")));
		}
		if (isSet(params, "merchant_type_id")) {
			w.set(Merchant::getMerchantTypeId, toLong(params.get("merchant_type_id")));
		}
		if (isSet(params, "province")) {
			w.set(Merchant::getProvince, str(params.get("province")));
		}
		if (isSet(params, "city")) {
			w.set(Merchant::getCity, str(params.get("city")));
		}
		if (isSet(params, "area")) {
			w.set(Merchant::getArea, str(params.get("area")));
		}
		if (isSet(params, "regions_id")) {
			w.set(Merchant::getRegionsId, str(params.get("regions_id")));
		}
		if (isSet(params, "address")) {
			w.set(Merchant::getAddress, str(params.get("address")));
		}
		if (isSet(params, "legal_name")) {
			w.set(Merchant::getLegalName, sensitiveFieldEncryptor.encrypt(str(params.get("legal_name"))));
		}
		if (isSet(params, "legal_cert_id")) {
			w.set(Merchant::getLegalCertId, sensitiveFieldEncryptor.encrypt(str(params.get("legal_cert_id"))));
		}
		if (isSet(params, "legal_mobile")) {
			w.set(Merchant::getLegalMobile, sensitiveFieldEncryptor.encrypt(str(params.get("legal_mobile"))));
		}
		if (isSet(params, "email")) {
			w.set(Merchant::getEmail, str(params.get("email")));
		}
		if (isSet(params, "bank_acct_type")) {
			w.set(Merchant::getBankAcctType, str(params.get("bank_acct_type")));
		}
		if (isSet(params, "card_id_mask")) {
			w.set(Merchant::getCardIdMask, str(params.get("card_id_mask")));
		}
		if (isSet(params, "bank_name")) {
			w.set(Merchant::getBankName, str(params.get("bank_name")));
		}
		if (isSet(params, "bank_mobile")) {
			w.set(Merchant::getBankMobile, sensitiveFieldEncryptor.encrypt(str(params.get("bank_mobile"))));
		}
		if (isSet(params, "license_url")) {
			w.set(Merchant::getLicenseUrl, str(params.get("license_url")));
		}
		if (isSet(params, "legal_certid_front_url")) {
			w.set(Merchant::getLegalCertidFrontUrl, str(params.get("legal_certid_front_url")));
		}
		if (isSet(params, "legal_cert_id_back_url")) {
			w.set(Merchant::getLegalCertIdBackUrl, str(params.get("legal_cert_id_back_url")));
		}
		if (isSet(params, "bank_card_front_url")) {
			w.set(Merchant::getBankCardFrontUrl, str(params.get("bank_card_front_url")));
		}
		if (isSet(params, "contract_url")) {
			w.set(Merchant::getContractUrl, str(params.get("contract_url")));
		}
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

	private void processRegions(Map<String, Object> params) {
		if (params.containsKey("regions_id") && params.containsKey("regions")) {
			List<String> regionNames = normalizeRegionNameList(params.get("regions"));
			for (int i = 0; i < regionNames.size() && i < 3; i++) {
				String v = regionNames.get(i);
				if (i == 0) {
					params.put("province", v);
				} else if (i == 1) {
					params.put("city", v);
				} else {
					params.put("area", v);
				}
			}
		}

		Object rawRegionsId = params.get("regions_id");
		if (rawRegionsId == null) {
			throw new ResourceException("区域必填");
		}
		List<Object> regionIds = normalizeRegionIdList(rawRegionsId);
		try {
			params.put("regions_id", objectMapper.writeValueAsString(regionIds));
		} catch (Exception e) {
			throw new ResourceException("区域必填");
		}
		params.remove("regions");
	}

	private List<String> normalizeRegionNameList(Object raw) {
		if (raw instanceof JsonNode node) {
			if (node.isArray()) {
				List<String> out = new ArrayList<>();
				for (JsonNode n : node) {
					out.add(n == null || n.isNull() ? "" : String.valueOf(n.asText()));
				}
				return out;
			}
			if (node.isObject()) {
				return mapObjectToOrderedStringList(node);
			}
			throw new ResourceException("区域必填");
		}
		if (raw instanceof Collection<?> c) {
			List<String> out = new ArrayList<>();
			for (Object o : c) {
				out.add(o == null ? "" : String.valueOf(o));
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			List<String> out = new ArrayList<>();
			for (Object o : arr) {
				out.add(o == null ? "" : String.valueOf(o));
			}
			return out;
		}
		throw new ResourceException("区域必填");
	}

	private List<String> mapObjectToOrderedStringList(JsonNode objectNode) {
		Map<Integer, String> sorted = new TreeMap<>();
		var it = objectNode.fields();
		while (it.hasNext()) {
			var e = it.next();
			int k;
			try {
				k = Integer.parseInt(e.getKey().trim());
			} catch (NumberFormatException ex) {
				throw new ResourceException("区域必填");
			}
			JsonNode val = e.getValue();
			sorted.put(k, val == null || val.isNull() ? "" : val.asText());
		}
		return new ArrayList<>(sorted.values());
	}

	private List<Object> normalizeRegionIdList(Object raw) {
		if (raw instanceof JsonNode node) {
			if (!node.isArray()) {
				throw new ResourceException("区域必填");
			}
			List<Object> out = new ArrayList<>();
			for (JsonNode n : node) {
				if (n == null || n.isNull()) {
					out.add(null);
				} else if (n.isNumber()) {
					out.add(n.numberValue());
				} else {
					out.add(n.asText());
				}
			}
			return out;
		}
		if (raw instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (raw instanceof Object[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (Object o : arr) {
				out.add(o);
			}
			return out;
		}
		throw new ResourceException("区域必填");
	}

	private static boolean isSet(Map<String, Object> params, String key) {
		if (!params.containsKey(key)) {
			return false;
		}
		return params.get(key) != null;
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

	private static boolean booleanFrom(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}
}
