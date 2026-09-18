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
import cn.shopex.ecshopx.adapay.domain.AdapayRegions;
import cn.shopex.ecshopx.adapay.mapper.AdapayRegionsMapper;
import cn.shopex.ecshopx.adapay.repository.AdapayMemberDetailRepository;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayMemberGetService {

	private final MemberOperatorContextService memberOperatorContextService;
	private final DealerInfoService dealerInfoService;
	private final AdapayMemberDetailRepository adapayMemberDetailRepository;
	private final AdapayRegionsMapper adapayRegionsMapper;
	private final FileStorageService fileStorageService;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public AdapayMemberGetService(
			MemberOperatorContextService memberOperatorContextService,
			DealerInfoService dealerInfoService,
			AdapayMemberDetailRepository adapayMemberDetailRepository,
			AdapayRegionsMapper adapayRegionsMapper,
			FileStorageService fileStorageService,
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper) {
		this.memberOperatorContextService = memberOperatorContextService;
		this.dealerInfoService = dealerInfoService;
		this.adapayMemberDetailRepository = adapayMemberDetailRepository;
		this.adapayRegionsMapper = adapayRegionsMapper;
		this.fileStorageService = fileStorageService;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> get(long companyId, Map<String, Object> jwtMap) {
		long jwtOperatorId = toLong(jwtMap.get("operator_id"));
		String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
		Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
		Map<String, Object> ctx =
				memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);

		int effectiveOperatorId = toInt(ctx.get("operator_id"));
		String effectiveOperatorType = stringVal(ctx.get("operator_type")).trim();

		if ("dealer".equals(effectiveOperatorType)) {
			Map<String, Object> result = dealerInfoService.dealerInfo(companyId, jwtMap, effectiveOperatorId);
			postProcess(result);
			return result;
		}

		if ("distributor".equals(effectiveOperatorType)) {
			return getForDistributorOrOther(companyId, effectiveOperatorId, "distributor", true);
		}

		return getForDistributorOrOther(companyId, effectiveOperatorId, effectiveOperatorType, false);
	}

	private Map<String, Object> getForDistributorOrOther(
			long companyId, int effectiveOperatorId, String dbOperatorType, boolean distributorFlow) {
		var memberOpt =
				adapayMemberDetailRepository.selectMemberByOperatorTypeAndOperatorIdAndCompany(
						dbOperatorType, effectiveOperatorId, companyId);

		if (memberOpt.isEmpty()) {
			Map<String, Object> out = new LinkedHashMap<>(2);
			if (distributorFlow) {
				out.put("basicInfo", buildBasicInfoForDistributorNoMember(companyId, effectiveOperatorId));
			} else {
				out.put("basicInfo", new ArrayList<Object>());
			}
			return out;
		}

		AdapayMember member = memberOpt.get();
		Map<String, Object> result = buildMemberDetailMap(companyId, member);
		if (distributorFlow) {
			result.put("basicInfo", buildBasicInfoForDistributorWithMember(companyId, effectiveOperatorId, result));
		} else {
			result.put("basicInfo", new ArrayList<Object>());
		}
		postProcess(result);
		return result;
	}

	private void postProcess(Map<String, Object> result) {
		if (truthyIsUpdate(result.get("is_update"))) {
			applyWaitDataTransferEquivalent(result);
		}
		splitAuditDescEquivalent(result);
	}

	private Map<String, Object> buildMemberDetailMap(long companyId, AdapayMember member) {
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

		String memberTypeRaw = member.getMemberType() == null ? "" : member.getMemberType().trim();
		if ("corp".equals(memberTypeRaw)) {
			Map<String, Object> memberOnly = new LinkedHashMap<>(result);
			var corpOpt =
					adapayMemberDetailRepository.selectCorpMemberByMemberIdAndCompany(
							member.getId(), member.getCompanyId());
			if (corpOpt.isPresent()) {
				AdapayCorpMember corp = corpOpt.get();
				Map<String, Object> corpMap = corpToSnakeMap(corp);
				corpMap.remove("audit_state");
				corpMap.remove("audit_desc");
				if (StringUtils.hasText(corp.getAttachFile())) {
					String path = corp.getAttachFile().trim();
					try {
						corpMap.put("attach_file", fileStorageService.privateDownloadUrl("file", path, 3600));
					} catch (RuntimeException e) {
						throw new ResourceException("附件地址生成失败");
					}
				} else {
					corpMap.put("attach_file", corp.getAttachFile() != null ? corp.getAttachFile() : "");
				}
				String prov = corp.getProvCode() != null ? corp.getProvCode() : "";
				String ac = corp.getAreaCode() != null ? corp.getAreaCode() : "";
				String areaStr = requireAreaName(prov) + "-" + requireAreaName(ac);
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
		result.put("audit_state", mapAuditStateForDisplay(hfAudit));
		return result;
	}

	private String requireAreaName(String code) {
		if (code == null || code.isBlank()) {
			return "";
		}
		String trimmed = code.trim();
		AdapayRegions r =
				adapayRegionsMapper.selectOne(
						new LambdaQueryWrapper<AdapayRegions>()
								.eq(AdapayRegions::getAreaCode, trimmed)
								.last("LIMIT 1"));
		if (r == null) {
			throw new ResourceException("地区编号不存在");
		}
		return r.getAreaName() != null ? r.getAreaName() : "";
	}

	private Map<String, Object> buildBasicInfoForDistributorNoMember(long companyId, int operatorId) {
		return loadDistributionBasic(companyId, operatorId, null);
	}

	private Map<String, Object> buildBasicInfoForDistributorWithMember(
			long companyId, int operatorId, Map<String, Object> memberRow) {
		return loadDistributionBasic(companyId, operatorId, memberRow);
	}

	private Map<String, Object> loadDistributionBasic(long companyId, int operatorId, Map<String, Object> memberRow) {
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

	private void applyWaitDataTransferEquivalent(Map<String, Object> result) {
		if (!"corp".equals(String.valueOf(result.get("member_type")))) {
			return;
		}
		Object mid = result.get("member_id");
		if (mid == null || parseLong(mid, 0L) <= 0L) {
			mid = result.get("id");
		}
		long memberId = parseLong(mid, 0L);
		if (memberId <= 0L) {
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
			putIfPresent(result, "name", data.get("name"));
			putIfPresent(result, "prov_code", data.get("prov_code"));
			putIfPresent(result, "area_code", data.get("area_code"));
			putIfPresent(result, "social_credit_code", data.get("social_credit_code"));
			putIfPresent(result, "social_credit_code_expires", data.get("social_credit_code_expires"));
			putIfPresent(result, "business_scope", data.get("business_scope"));
			putIfPresent(result, "legal_person", data.get("legal_person"));
			putIfPresent(result, "legal_cert_id", data.get("legal_cert_id"));
			putIfPresent(result, "legal_cert_id_expires", data.get("legal_cert_id_expires"));
			putIfPresent(result, "legal_mp", data.get("legal_mp"));
			putIfPresent(result, "address", data.get("address"));
			putIfPresent(result, "zip_code", data.get("zip_code"));
			putIfPresent(result, "telphone", data.get("telphone"));
			putIfPresent(result, "email", data.get("email"));
			putIfPresent(result, "attach_file", data.get("attach_file"));
			putIfPresent(result, "attach_file_name", data.get("attach_file_name"));
			putIfPresent(result, "bank_code", data.get("bank_code"));
			putIfPresent(result, "bank_acct_type", data.get("bank_acct_type"));
			putIfPresent(result, "card_no", data.get("card_no"));
			putIfPresent(result, "card_name", data.get("card_name"));
			result.put("audit_state", auditState);
		} else if ("B".equals(auditState) || "C".equals(auditState) || "D".equals(auditState)) {
			result.put("audit_state", auditState);
			if (updateData.getAuditDesc() != null) {
				result.put("audit_desc", updateData.getAuditDesc());
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

	private static void splitAuditDescEquivalent(Map<String, Object> result) {
		Object ad = result.get("audit_desc");
		String auditDesc = ad != null ? ad.toString() : "";
		if (!StringUtils.hasText(auditDesc)) {
			return;
		}
		String[] parts = auditDesc.split("###", 2);
		result.put("audit_desc_1", parts.length > 0 ? parts[0] : "");
		result.put("audit_desc_2", parts.length > 1 ? parts[1] : "");
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

	private static String mapAuditStateForDisplay(String state) {
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

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long toLongObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int toInt(Object o) {
		long v = toLong(o);
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}
}
