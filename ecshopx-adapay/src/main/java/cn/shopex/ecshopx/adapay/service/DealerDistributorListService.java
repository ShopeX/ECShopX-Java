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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantEntryMapper;
import cn.shopex.ecshopx.adapay.mapper.DealerDistributorListMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerDistributorListService {

	private static final String AUDIT_WAIT = "A";
	private static final String AUDIT_WAIT_MAIN = "0";
	private static final String AUDIT_FAIL = "B";
	private static final String AUDIT_MEMBER_FAIL = "C";
	private static final String AUDIT_SUCCESS = "E";

	private final DealerDistributorListMapper dealerDistributorListMapper;
	private final DealerParentIdService dealerParentIdService;
	private final AdapayMerchantEntryMapper adapayMerchantEntryMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public DealerDistributorListService(
			DealerDistributorListMapper dealerDistributorListMapper,
			DealerParentIdService dealerParentIdService,
			AdapayMerchantEntryMapper adapayMerchantEntryMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.dealerDistributorListMapper = dealerDistributorListMapper;
		this.dealerParentIdService = dealerParentIdService;
		this.adapayMerchantEntryMapper = adapayMerchantEntryMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> distributorList(
			long companyId,
			Map<String, Object> jwtMap,
			boolean dealerIdParamPresent,
			String dealerIdParam,
			String name,
			String contact,
			String mobile,
			String auditStateParam,
			String province,
			String city,
			String area,
			String adapayFeeMode,
			String timeStart,
			String timeEnd,
			int page,
			int pageSize) {
		if (page < 1) {
			throw new BadRequestException("page 须为正整数", 400);
		}

		String nameLike = null;
		if (ValuePresence.hasEffectiveValue((Object) name)) {
			nameLike = "%" + escapeSqlLike(name.trim()) + "%";
		}
		String contactEq = ValuePresence.hasEffectiveValue((Object) contact) ? contact : null;
		String mobileEq = ValuePresence.hasEffectiveValue((Object) mobile) ? mobile : null;
		String provinceEq = ValuePresence.hasEffectiveValue((Object) province) ? province : null;
		String cityEq = ValuePresence.hasEffectiveValue((Object) city) ? city : null;
		String areaEq = ValuePresence.hasEffectiveValue((Object) area) ? area : null;
		String adapayFeeEq = ValuePresence.hasEffectiveValue((Object) adapayFeeMode) ? adapayFeeMode : null;

		boolean bothTime =
				ValuePresence.hasEffectiveValue((Object) timeStart) && ValuePresence.hasEffectiveValue((Object) timeEnd);
		String timeStartParam = bothTime ? timeStart : null;
		String timeEndParam = bothTime ? timeEnd : null;

		String auditTrimmed = auditStateParam == null ? "" : auditStateParam.trim();
		String auditStateGroup = null;
		if (!auditTrimmed.isEmpty()) {
			if ("1".equals(auditTrimmed) || "2".equals(auditTrimmed) || "3".equals(auditTrimmed)) {
				auditStateGroup = auditTrimmed;
			} else {
				auditStateGroup = "3";
			}
		}

		AdapayMerchantEntry entry = adapayMerchantEntryMapper.selectOne(
				new LambdaQueryWrapper<AdapayMerchantEntry>()
						.eq(AdapayMerchantEntry::getCompanyId, companyId)
						.select(AdapayMerchantEntry::getMerName)
						.last("LIMIT 1"));
		String merName = entry != null && entry.getMerName() != null ? entry.getMerName() : "-";

		Integer dealerId = null;
		boolean dealerIdSet = false;
		Object operatorTypeObj = jwtMap.get("operator_type");
		boolean isDealer = operatorTypeObj != null
				&& operatorTypeObj.toString().trim().equalsIgnoreCase("dealer");
		if (isDealer) {
			long jwtOpId = parseLongClaim(jwtMap.get("operator_id"));
			long principalDealerId = dealerParentIdService.resolveDealerPrincipalOperatorId(companyId, jwtOpId);
			dealerId = (int) principalDealerId;
			dealerIdSet = true;
		} else if (dealerIdParamPresent) {
			String raw = dealerIdParam == null ? "" : dealerIdParam.trim();
			try {
				dealerId = Integer.parseInt(raw);
				dealerIdSet = true;
			} catch (NumberFormatException e) {
				throw new BadRequestException("dealer_id 格式不正确");
			}
		}

		Integer limit = pageSize > 0 ? pageSize : null;
		int offset = pageSize > 0 ? (page - 1) * pageSize : 0;

		long count = dealerDistributorListMapper.countDistributors(
				companyId,
				dealerId,
				dealerIdSet,
				nameLike,
				contactEq,
				mobileEq,
				provinceEq,
				cityEq,
				areaEq,
				auditStateGroup,
				adapayFeeEq,
				timeStartParam,
				timeEndParam);
		List<Map<String, Object>> rows = dealerDistributorListMapper.selectDistributors(
				companyId,
				dealerId,
				dealerIdSet,
				nameLike,
				contactEq,
				mobileEq,
				provinceEq,
				cityEq,
				areaEq,
				auditStateGroup,
				adapayFeeEq,
				timeStartParam,
				timeEndParam,
				offset,
				limit);

		for (Map<String, Object> row : rows) {
			// Nullable columns or LEFT JOIN misses may leave keys out of the row map; ensure stable list fields with explicit null.
			putCanonicalNullIfAbsent(row, "username");
			putCanonicalNullIfAbsent(row, "operator_id");
			putCanonicalNullIfAbsent(row, "address");

			Object rawAudit = getCi(row, "audit_state");
			String letter = rawAudit == null ? "" : String.valueOf(rawAudit);
			String displayCode = handleAuditState(letter);
			String displayName = handleAuditStateName(displayCode);
			row.put("audit_state", displayCode);
			row.put("audit_state_name", displayName);

			Object splitRaw = getCi(row, "split_ledger_info");
			String json = splitRaw == null ? null : String.valueOf(splitRaw);
			if (json == null || json.isEmpty()) {
				row.put("split_ledger_info", null);
			} else {
				try {
					row.put(
							"split_ledger_info",
							objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}));
				} catch (Exception e) {
					row.put("split_ledger_info", null);
				}
			}

			row.put("mer_name", merName);

			String m = getCi(row, "mobile") == null ? "" : String.valueOf(getCi(row, "mobile"));
			row.put("mobile", sensitiveFieldEncryptor.decrypt(sensitiveFieldEncryptor.decrypt(m)));
			String c = getCi(row, "contact") == null ? "" : String.valueOf(getCi(row, "contact"));
			row.put("contact", sensitiveFieldEncryptor.decrypt(sensitiveFieldEncryptor.decrypt(c)));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", rows);
		data.put("count", count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
		return data;
	}

	private static String handleAuditState(String auditStateLetter) {
		if (AUDIT_WAIT.equals(auditStateLetter) || AUDIT_WAIT_MAIN.equals(auditStateLetter)) {
			return "2";
		}
		if (AUDIT_SUCCESS.equals(auditStateLetter)) {
			return "3";
		}
		return "1";
	}

	private static String handleAuditStateName(String displayCode) {
		switch (displayCode) {
			case AUDIT_WAIT:
			case AUDIT_WAIT_MAIN:
				return "审核中";
			case AUDIT_SUCCESS:
				return "入网成功";
			case AUDIT_MEMBER_FAIL:
			case AUDIT_FAIL:
				return "入网失败";
			default:
				return "未入网";
		}
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static long parseLongClaim(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Object getCi(Map<String, Object> row, String key) {
		if (row.containsKey(key)) {
			return row.get(key);
		}
		for (Map.Entry<String, Object> e : row.entrySet()) {
			String k = e.getKey();
			if (k != null && k.equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
	}

	/**
	 * Ensures {@code canonicalKey} exists on the row map even when the value is null, so list rows expose consistent field names.
	 * Drops case-variant duplicate keys so serialization emits a single canonical property name.
	 */
	private static void putCanonicalNullIfAbsent(Map<String, Object> row, String canonicalKey) {
		Object v = getCi(row, canonicalKey);
		String foundKey = null;
		for (String k : row.keySet()) {
			if (k != null && k.equalsIgnoreCase(canonicalKey)) {
				foundKey = k;
				break;
			}
		}
		if (foundKey != null && !foundKey.equals(canonicalKey)) {
			row.remove(foundKey);
		}
		row.put(canonicalKey, v);
	}
}
