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
import cn.shopex.ecshopx.merchant.port.MerchantOperatorListQueryPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantOperatorListQueryService {

	private final MerchantOperatorListQueryPort merchantOperatorListQueryPort;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MerchantOutsideLangReadService merchantOutsideLangReadService;

	public MerchantOperatorListQueryService(
			MerchantOperatorListQueryPort merchantOperatorListQueryPort,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MerchantOutsideLangReadService merchantOutsideLangReadService) {
		this.merchantOperatorListQueryPort = merchantOperatorListQueryPort;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.merchantOutsideLangReadService = merchantOutsideLangReadService;
	}

	public Map<String, Object> query(
			long companyId, Map<String, Object> user, Map<String, Object> validatedParams, String langTag) {
		int page = MerchantListParamValidator.pageFromValidatedMap(validatedParams);
		int pageSize = MerchantListParamValidator.pageSizeFromValidatedMap(validatedParams);

		String operatorType = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
		Long merchantIdOrNull = null;
		if ("merchant".equals(operatorType)) {
			long mid = toLong(user.get("merchant_id"));
			if (mid > 0) {
				merchantIdOrNull = mid;
			}
		}
		boolean onlyMerchantMain = "admin".equals(operatorType);

		String mobilePlain = MerchantListParamValidator.stringFilterFromMap(validatedParams, "mobile");
		String operatorsMobileEncryptedOrNull =
				StringUtils.hasText(mobilePlain) ? sensitiveFieldEncryptor.encrypt(mobilePlain) : null;
		String merchantNameContainsOrNull =
				MerchantListParamValidator.stringFilterFromMap(validatedParams, "merchant_name");

		MerchantOperatorListFilter filter = new MerchantOperatorListFilter(
				companyId,
				merchantIdOrNull,
				onlyMerchantMain,
				operatorsMobileEncryptedOrNull,
				merchantNameContainsOrNull);

		MerchantOperatorListPage pageResult = merchantOperatorListQueryPort.queryPage(filter, page, pageSize);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Map<String, Object> row : pageResult.rows()) {
			if (row == null) {
				continue;
			}
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>(row);
			Object mobileVal = copy.get("mobile");
			if (mobileVal instanceof String str && !str.isEmpty()) {
				copy.put("mobile", sensitiveFieldEncryptor.decrypt(str));
			}
			long merchantTableId = toLong(copy.get("merchant_table_id"));
			merchantOutsideLangReadService.applyMerchantRow(companyId, merchantTableId, langTag, copy);
			copy.remove("merchant_table_id");

			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("operator_id", copy.get("operator_id"));
			out.put("mobile", copy.get("mobile"));
			out.put("password", copy.get("password"));
			out.put("merchant_name", copy.get("merchant_name"));
			out.put("settled_type", copy.get("settled_type"));
			out.put("is_merchant_main", copy.get("is_merchant_main"));
			list.add(out);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", pageResult.totalCount());
		body.put("list", list);
		return body;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
