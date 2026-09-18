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

import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMemberUpdateLog;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.repository.AdapayMemberDetailRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayDistributorMemberInfoService {

	private final AdapayMemberDetailRepository adapayMemberDetailRepository;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public AdapayDistributorMemberInfoService(
			AdapayMemberDetailRepository adapayMemberDetailRepository,
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper) {
		this.adapayMemberDetailRepository = adapayMemberDetailRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> buildMemberInfoSection(
			long companyId, long distributorId, String adapayMemberOperatorType) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("operator_type", adapayMemberOperatorType);
		filter.put("operator_id", distributorId);
		filter.put("company_id", companyId);

		return adapayMemberDetailRepository
				.selectMemberByOperatorTypeAndOperatorIdAndCompany(
						adapayMemberOperatorType, distributorId, companyId)
				.map(m -> buildWithMember(m, filter))
				.orElseGet(() -> {
					Map<String, Object> out = new LinkedHashMap<>();
					out.put("basicInfo", loadBasicInfoForDistributor(companyId, distributorId, null));
					return out;
				});
	}

	private Map<String, Object> buildWithMember(AdapayMember member, Map<String, Object> filter) {
		Map<String, Object> result = memberToSnakeMap(member);

		adapayMemberDetailRepository
				.selectSettleAccountByMemberIdAndCompany(member.getId(), member.getCompanyId())
				.ifPresent(
						rs -> {
							result.put("settle_account_id", nullToEmpty(rs.getSettleAccountId()));
							result.put("bank_card_id", nullToEmpty(rs.getCardId()));
							result.put("bank_card_name", nullToEmpty(rs.getCardName()));
							result.put("bank_cert_id", nullToEmpty(rs.getCertId()));
							result.put("bank_tel_no", nullToEmpty(rs.getTelNo()));
							result.put("bank_name", nullToEmpty(rs.getBankName()));
						});

		if ("corp".equals(member.getMemberType())) {
			Map<String, Object> memberOnly = new LinkedHashMap<>(result);
			var corpOpt =
					adapayMemberDetailRepository.selectCorpMemberByMemberIdAndCompany(
							member.getId(), member.getCompanyId());
			if (corpOpt.isPresent()) {
				AdapayCorpMember corp = corpOpt.get();
				Map<String, Object> corpMap = corpToSnakeMap(corp);
				corpMap.remove("audit_state");
				corpMap.remove("audit_desc");
				String attach = corp.getAttachFile() != null ? corp.getAttachFile() : "";
				corpMap.put("attach_file", attach);
				String prov = corp.getProvCode() != null ? corp.getProvCode() : "";
				String ac = corp.getAreaCode() != null ? corp.getAreaCode() : "";
				String areaStr =
						adapayMemberDetailRepository
								.selectConcatAreaNamesByProvAndAreaCode(prov, ac)
								.orElse("");
				Map<String, Object> merged = new LinkedHashMap<>(corpMap);
				merged.putAll(memberOnly);
				merged.put("area", areaStr);
				result.clear();
				result.putAll(merged);
			} else {
				result.put("member_id", member.getId());
			}
		} else {
			result.put("member_id", member.getId());
		}

		String hfAudit = member.getAuditState() != null ? member.getAuditState() : "";
		result.put("hf_audit_state", hfAudit);
		result.put("audit_state", changeStatus(hfAudit));

		Map<String, Object> basic = loadBasicInfoForDistributor(member.getCompanyId(), filterOperatorId(filter), result);
		result.put("basicInfo", basic);

		if (truthyIsUpdate(result.get("is_update"))) {
			applyWaitDataTransfer(result);
		}

		splitAuditDesc(result);
		return result;
	}

	private static void splitAuditDesc(Map<String, Object> result) {
		Object ad = result.get("audit_desc");
		String auditDesc = ad != null ? ad.toString() : "";
		if (!StringUtils.hasText(auditDesc)) {
			return;
		}
		String[] parts = auditDesc.split("###", 2);
		result.put("audit_desc_1", parts.length > 0 ? parts[0] : "");
		result.put("audit_desc_2", parts.length > 1 ? parts[1] : "");
	}

	private void applyWaitDataTransfer(Map<String, Object> adapayMemberInfo) {
		if (!"corp".equals(String.valueOf(adapayMemberInfo.get("member_type")))) {
			return;
		}
		Object mid = adapayMemberInfo.get("member_id");
		long memberId = mid instanceof Number n ? n.longValue() : parseLong(mid, 0L);
		if (memberId <= 0) {
			return;
		}
		AdapayMemberUpdateLog updateData =
				adapayMemberDetailRepository
						.selectLatestUpdateLogByMemberIdForWaitDataTranf(memberId)
						.orElse(null);
		if (updateData == null) {
			return;
		}
		Map<String, Object> data = parseJsonObject(updateData.getData());
		String auditState = updateData.getAuditState() != null ? updateData.getAuditState() : "";
		if ("A".equals(auditState)) {
			putIfPresent(adapayMemberInfo, "name", data.get("name"));
			putIfPresent(adapayMemberInfo, "prov_code", data.get("prov_code"));
			putIfPresent(adapayMemberInfo, "area_code", data.get("area_code"));
			putIfPresent(adapayMemberInfo, "social_credit_code", data.get("social_credit_code"));
			putIfPresent(adapayMemberInfo, "social_credit_code_expires", data.get("social_credit_code_expires"));
			putIfPresent(adapayMemberInfo, "business_scope", data.get("business_scope"));
			putIfPresent(adapayMemberInfo, "legal_person", data.get("legal_person"));
			putIfPresent(adapayMemberInfo, "legal_cert_id", data.get("legal_cert_id"));
			putIfPresent(adapayMemberInfo, "legal_cert_id_expires", data.get("legal_cert_id_expires"));
			putIfPresent(adapayMemberInfo, "legal_mp", data.get("legal_mp"));
			putIfPresent(adapayMemberInfo, "address", data.get("address"));
			putIfPresent(adapayMemberInfo, "zip_code", data.get("zip_code"));
			putIfPresent(adapayMemberInfo, "telphone", data.get("telphone"));
			putIfPresent(adapayMemberInfo, "email", data.get("email"));
			putIfPresent(adapayMemberInfo, "attach_file", data.get("attach_file"));
			putIfPresent(adapayMemberInfo, "attach_file_name", data.get("attach_file_name"));
			putIfPresent(adapayMemberInfo, "bank_code", data.get("bank_code"));
			putIfPresent(adapayMemberInfo, "bank_acct_type", data.get("bank_acct_type"));
			putIfPresent(adapayMemberInfo, "card_no", data.get("card_no"));
			putIfPresent(adapayMemberInfo, "card_name", data.get("card_name"));
			adapayMemberInfo.put("audit_state", auditState);
		} else if ("B".equals(auditState) || "C".equals(auditState) || "D".equals(auditState)) {
			adapayMemberInfo.put("audit_state", auditState);
			if (updateData.getAuditDesc() != null) {
				adapayMemberInfo.put("audit_desc", updateData.getAuditDesc());
			}
		}
	}

	private Map<String, Object> parseJsonObject(String json) {
		if (!StringUtils.hasText(json)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static void putIfPresent(Map<String, Object> target, String key, Object v) {
		if (v != null) {
			target.put(key, v);
		}
	}

	private static boolean truthyIsUpdate(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private Map<String, Object> loadBasicInfoForDistributor(
			long companyId, long operatorId, Map<String, Object> memberRow) {
		Map<String, Object> basic = new LinkedHashMap<>();
		if (operatorId <= 0) {
			return basic;
		}
		String sql =
				"SELECT name, contact, hour, is_ziti, auto_sync_goods, is_delivery, is_dada, province, city, split_ledger_info "
						+ "FROM distribution_distributor WHERE company_id = ? AND distributor_id = ? LIMIT 1";
		List<Map<String, Object>> rows =
				jdbcTemplate.query(
						sql,
						(rs, i) -> {
							Map<String, Object> m = new LinkedHashMap<>();
							m.put("name", rs.getString("name"));
							m.put("contact", rs.getString("contact"));
							m.put("hour", rs.getString("hour"));
							m.put("is_ziti", rs.getObject("is_ziti"));
							m.put("auto_sync_goods", rs.getObject("auto_sync_goods"));
							m.put("is_delivery", rs.getObject("is_delivery"));
							m.put("is_dada", rs.getObject("is_dada"));
							m.put("province", rs.getString("province"));
							m.put("city", rs.getString("city"));
							m.put("split_ledger_info", rs.getString("split_ledger_info"));
							return m;
						},
						companyId,
						operatorId);
		if (rows.isEmpty()) {
			return basic;
		}
		Map<String, Object> d = rows.get(0);
		String prov = str(d.get("province"));
		String city = str(d.get("city"));
		basic.put("name", d.get("name"));
		basic.put("contact", d.get("contact"));
		basic.put("hour", d.get("hour"));
		basic.put("is_ziti", d.get("is_ziti"));
		basic.put("auto_sync_goods", d.get("auto_sync_goods"));
		basic.put("is_delivery", d.get("is_delivery"));
		basic.put("is_dada", d.get("is_dada"));
		basic.put("area", prov + " - " + city);
		Object split = d.get("split_ledger_info");
		if (split != null && StringUtils.hasText(split.toString())) {
			try {
				basic.put("split_ledger_info", objectMapper.readValue(split.toString(), Object.class));
			} catch (Exception e) {
				basic.put("split_ledger_info", split.toString());
			}
		} else {
			basic.put("split_ledger_info", null);
		}
		if (memberRow != null) {
			basic.put("email", memberRow.get("email") != null ? memberRow.get("email") : "");
			basic.put("tel_no", memberRow.get("tel_no") != null ? memberRow.get("tel_no") : "");
		} else {
			basic.put("email", "");
			basic.put("tel_no", "");
		}
		return basic;
	}

	private static long filterOperatorId(Map<String, Object> filter) {
		Object o = filter.get("operator_id");
		return parseLong(o, 0L);
	}

	private static long parseLong(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String changeStatus(String state) {
		if (state == null || state.isEmpty()) {
			return "A";
		}
		return switch (state) {
			case "0", "A" -> "A";
			case "B", "C", "D" -> "B";
			case "E" -> "C";
			default -> "A";
		};
	}

	private static Map<String, Object> memberToSnakeMap(AdapayMember m) {
		Map<String, Object> map = new LinkedHashMap<>();
		put(map, "id", m.getId());
		put(map, "app_id", m.getAppId());
		put(map, "location", m.getLocation());
		put(map, "company_id", m.getCompanyId());
		put(map, "pid", m.getPid());
		put(map, "is_update", m.getIsUpdate());
		put(map, "operator_id", m.getOperatorId());
		put(map, "operator_type", m.getOperatorType());
		put(map, "email", m.getEmail());
		put(map, "member_type", m.getMemberType());
		put(map, "gender", m.getGender());
		put(map, "nickname", m.getNickname());
		put(map, "tel_no", m.getTelNo());
		put(map, "user_name", m.getUserName());
		put(map, "cert_type", m.getCertType());
		put(map, "cert_id", m.getCertId());
		put(map, "is_sms", m.getIsSms());
		put(map, "audit_state", m.getAuditState());
		put(map, "audit_desc", m.getAuditDesc());
		put(map, "status", m.getStatus());
		put(map, "error_info", m.getErrorInfo());
		put(map, "create_time", m.getCreateTime());
		put(map, "update_time", m.getUpdateTime());
		put(map, "valid", m.getValid());
		put(map, "is_created", m.getIsCreated());
		return map;
	}

	private static Map<String, Object> corpToSnakeMap(AdapayCorpMember c) {
		Map<String, Object> map = new LinkedHashMap<>();
		put(map, "id", c.getId());
		put(map, "app_id", c.getAppId());
		put(map, "order_no", c.getOrderNo());
		put(map, "member_id", c.getMemberId());
		put(map, "company_id", c.getCompanyId());
		put(map, "dealer_id", c.getDealerId());
		put(map, "distributor_id", c.getDistributorId());
		put(map, "operator_id", c.getOperatorId());
		put(map, "name", c.getName());
		put(map, "prov_code", c.getProvCode());
		put(map, "area_code", c.getAreaCode());
		put(map, "social_credit_code", c.getSocialCreditCode());
		put(map, "social_credit_code_expires", c.getSocialCreditCodeExpires());
		put(map, "business_scope", c.getBusinessScope());
		put(map, "legal_person", c.getLegalPerson());
		put(map, "legal_cert_id", c.getLegalCertId());
		put(map, "legal_cert_id_expires", c.getLegalCertIdExpires());
		put(map, "legal_mp", c.getLegalMp());
		put(map, "address", c.getAddress());
		put(map, "zip_code", c.getZipCode());
		put(map, "telphone", c.getTelphone());
		put(map, "email", c.getEmail());
		put(map, "attach_file", c.getAttachFile());
		put(map, "attach_file_name", c.getAttachFileName());
		put(map, "confirm_letter_file", c.getConfirmLetterFile());
		put(map, "confirm_letter_file_name", c.getConfirmLetterFileName());
		put(map, "bank_code", c.getBankCode());
		put(map, "bank_acct_type", c.getBankAcctType());
		put(map, "card_no", c.getCardNo());
		put(map, "card_name", c.getCardName());
		put(map, "audit_state", c.getAuditState());
		put(map, "audit_desc", c.getAuditDesc());
		put(map, "status", c.getStatus());
		put(map, "error_info", c.getErrorInfo());
		put(map, "create_time", c.getCreateTime());
		put(map, "update_time", c.getUpdateTime());
		return map;
	}

	private static void put(Map<String, Object> m, String k, Object v) {
		m.put(k, v);
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}
}
