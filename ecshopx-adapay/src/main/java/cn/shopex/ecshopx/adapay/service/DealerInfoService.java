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
import cn.shopex.ecshopx.adapay.domain.AdapayRegions;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayRegionsMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DealerInfoService {

	private final OperatorsCommandService operatorsCommandService;
	private final OperatorsQueryService operatorsQueryService;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayCorpMemberMapper adapayCorpMemberMapper;
	private final AdapayRegionsMapper adapayRegionsMapper;
	private final FileStorageService fileStorageService;
	private final OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;
	private final ObjectMapper objectMapper;

	public DealerInfoService(
			OperatorsCommandService operatorsCommandService,
			OperatorsQueryService operatorsQueryService,
			AdapayMemberMapper adapayMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayCorpMemberMapper adapayCorpMemberMapper,
			AdapayRegionsMapper adapayRegionsMapper,
			FileStorageService fileStorageService,
			OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService,
			ObjectMapper objectMapper) {
		this.operatorsCommandService = operatorsCommandService;
		this.operatorsQueryService = operatorsQueryService;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
		this.adapayRegionsMapper = adapayRegionsMapper;
		this.fileStorageService = fileStorageService;
		this.operatorDeliveryStaffRoleDataService = operatorDeliveryStaffRoleDataService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> dealerInfo(long companyId, Map<String, Object> jwtMap, long targetOperatorId) {
		ensureOperatorContext(companyId, jwtMap);

		AdapayMember member = adapayMemberMapper.selectOne(
				new LambdaQueryWrapper<AdapayMember>()
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getOperatorId, (int) targetOperatorId)
						.eq(AdapayMember::getOperatorType, "dealer")
						.orderByDesc(AdapayMember::getId)
						.last("LIMIT 1"));

		if (member == null) {
			Map<String, Object> out = new LinkedHashMap<>();
			appendBasicInfoForDealer(companyId, targetOperatorId, out);
			return out;
		}

		Map<String, Object> memberMap = memberEntityToSnakeMap(member);

		AdapaySettleAccount settle = adapaySettleAccountMapper.selectOne(
				new LambdaQueryWrapper<AdapaySettleAccount>()
						.eq(AdapaySettleAccount::getCompanyId, companyId)
						.eq(AdapaySettleAccount::getMemberId, member.getId())
						.orderByDesc(AdapaySettleAccount::getId)
						.last("LIMIT 1"));
		if (settle != null) {
			memberMap.put("settle_account_id", nz(settle.getSettleAccountId()));
			memberMap.put("bank_card_id", nz(settle.getCardId()));
			memberMap.put("bank_card_name", nz(settle.getCardName()));
			memberMap.put("bank_cert_id", nz(settle.getCertId()));
			memberMap.put("bank_tel_no", nz(settle.getTelNo()));
			memberMap.put("bank_name", nz(settle.getBankName()));
		}

		Map<String, Object> result;
		String memberTypeRaw = member.getMemberType() == null ? "" : member.getMemberType().trim();
		if ("corp".equals(memberTypeRaw)) {
			AdapayCorpMember corp = adapayCorpMemberMapper.selectOne(
					new LambdaQueryWrapper<AdapayCorpMember>()
							.eq(AdapayCorpMember::getCompanyId, companyId)
							.eq(AdapayCorpMember::getMemberId, member.getId())
							.orderByDesc(AdapayCorpMember::getId)
							.last("LIMIT 1"));
			if (corp == null) {
				result = new LinkedHashMap<>(memberMap);
			} else {
				Map<String, Object> corpMap = corpEntityToSnakeMap(corp);
				if (StringUtils.hasText(corp.getAttachFile())) {
					String path = corp.getAttachFile().trim();
					try {
						corpMap.put("attach_file", fileStorageService.privateDownloadUrl("file", path, 3600));
					} catch (RuntimeException e) {
						throw new ResourceException("附件地址生成失败");
					}
				}
				String provName = requireAreaName(corp.getProvCode());
				String areaName = requireAreaName(corp.getAreaCode());
				memberMap.put("area", provName + "-" + areaName);
				corpMap.remove("audit_state");
				corpMap.remove("audit_desc");
				result = new LinkedHashMap<>();
				result.putAll(corpMap);
				result.putAll(memberMap);
			}
		} else {
			result = new LinkedHashMap<>(memberMap);
			result.put("member_id", result.get("id"));
		}

		String rawAudit = String.valueOf(result.getOrDefault("audit_state", "")).trim();
		result.put("hf_audit_state", rawAudit);
		result.put("audit_state", mapAuditStateForDisplay(rawAudit));

		appendBasicInfoForDealer(companyId, targetOperatorId, result);
		return result;
	}

	private void ensureOperatorContext(long companyId, Map<String, Object> jwtMap) {
		String operatorTypeTrimmed =
				jwtMap.get("operator_type") == null ? "" : jwtMap.get("operator_type").toString().trim();
		if ("distributor".equals(operatorTypeTrimmed)) {
			return;
		}
		if (!"dealer".equals(operatorTypeTrimmed)) {
			return;
		}
		long jwtOperatorId = parseLongClaim(jwtMap.get("operator_id"));
		operatorsCommandService.initDealerParentIdForCurrentDealer(jwtOperatorId);
		Map<String, Object> filter = new HashMap<>(4);
		filter.put("operator_id", jwtOperatorId);
		filter.put("company_id", companyId);
		Map<String, Object> op = operatorsQueryService.getInfo(filter);
		if (op == null || op.isEmpty()) {
			throw new ResourceException("没有账号信息");
		}
		if (isDealerSubAccount(op)) {
			long parentId = parseLongClaim(op.get("dealer_parent_id"));
			if (parentId <= 0L) {
				throw new ResourceException("没有账号信息");
			}
		}
	}

	private void appendBasicInfoForDealer(long companyId, long targetOperatorId, Map<String, Object> result) {
		Map<String, Object> staffFilter = new HashMap<>(4);
		staffFilter.put("operator_id", targetOperatorId);
		staffFilter.put("company_id", companyId);
		Map<String, Object> staff = operatorsQueryService.getInfo(staffFilter);
		if (staff == null || staff.isEmpty()) {
			result.put("basicInfo", Collections.emptyList());
			return;
		}
		staff.put("role_data", operatorDeliveryStaffRoleDataService.getRoleDataList(companyId, targetOperatorId));
		Object splitLedger = staff.get("split_ledger_info");
		if (splitLedger instanceof String str) {
			String trimmed = str.trim();
			if (!trimmed.isEmpty()) {
				try {
					result.put(
							"split_ledger_info",
							objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {}));
				} catch (Exception e) {
					result.put("split_ledger_info", null);
				}
			}
		} else if (splitLedger instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> asMap = (Map<String, Object>) m;
			result.put("split_ledger_info", new LinkedHashMap<>(asMap));
		}
		Map<String, Object> basic = new LinkedHashMap<>();
		basic.put("name", nullToEmpty(staff.get("username")));
		basic.put("contact", nullToEmpty(staff.get("contact")));
		basic.put("email", result.containsKey("email") ? nullToEmpty(result.get("email")) : "");
		basic.put("tel_no", result.containsKey("tel_no") ? nullToEmpty(result.get("tel_no")) : "");
		basic.put("area", result.get("area") != null ? String.valueOf(result.get("area")) : "");
		result.put("basicInfo", basic);
	}

	private static String nullToEmpty(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private String requireAreaName(String code) {
		if (code == null || code.isBlank()) {
			return "";
		}
		String trimmed = code.trim();
		AdapayRegions r = adapayRegionsMapper.selectOne(
				new LambdaQueryWrapper<AdapayRegions>().eq(AdapayRegions::getAreaCode, trimmed).last("LIMIT 1"));
		if (r == null) {
			throw new ResourceException("地区编号不存在");
		}
		return r.getAreaName() != null ? r.getAreaName() : "";
	}

	private static String mapAuditStateForDisplay(String raw) {
		if ("0".equals(raw) || "A".equals(raw)) {
			return "A";
		}
		if ("B".equals(raw) || "C".equals(raw) || "D".equals(raw)) {
			return "B";
		}
		if ("E".equals(raw)) {
			return "C";
		}
		return "A";
	}

	private static Map<String, Object> memberEntityToSnakeMap(AdapayMember m) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", m.getId());
		map.put("app_id", nz(m.getAppId()));
		map.put("company_id", m.getCompanyId());
		map.put("operator_id", m.getOperatorId() == null ? 0L : m.getOperatorId().longValue());
		map.put("operator_type", nz(m.getOperatorType()));
		map.put("member_type", nz(m.getMemberType()));
		map.put("email", nz(m.getEmail()));
		map.put("tel_no", nz(m.getTelNo()));
		map.put("user_name", nz(m.getUserName()));
		map.put("cert_id", nz(m.getCertId()));
		map.put("audit_state", nz(m.getAuditState()));
		map.put("audit_desc", nz(m.getAuditDesc()));
		map.put("gender", nz(m.getGender()));
		map.put("nickname", nz(m.getNickname()));
		map.put("location", nz(m.getLocation()));
		map.put("valid", Boolean.TRUE.equals(m.getValid()));
		map.put("is_created", Boolean.TRUE.equals(m.getIsCreated()));
		map.put("is_update", m.getIsUpdate() == null ? 0 : m.getIsUpdate());
		map.put("create_time", m.getCreateTime());
		map.put("update_time", m.getUpdateTime());
		map.put("status", nz(m.getStatus()));
		map.put("error_info", nz(m.getErrorInfo()));
		map.put("is_sms", m.getIsSms() == null ? "" : m.getIsSms());
		map.put("cert_type", nz(m.getCertType()));
		map.put("pid", m.getPid() == null ? 0L : m.getPid());
		return map;
	}

	private static Map<String, Object> corpEntityToSnakeMap(AdapayCorpMember c) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", c.getId());
		map.put("app_id", nz(c.getAppId()));
		map.put("order_no", nz(c.getOrderNo()));
		map.put("member_id", c.getMemberId());
		map.put("company_id", c.getCompanyId());
		map.put("dealer_id", c.getDealerId() == null ? 0L : c.getDealerId());
		map.put("distributor_id", c.getDistributorId() == null ? 0L : c.getDistributorId());
		map.put("operator_id", c.getOperatorId() == null ? 0L : c.getOperatorId().longValue());
		map.put("name", nz(c.getName()));
		map.put("prov_code", nz(c.getProvCode()));
		map.put("area_code", nz(c.getAreaCode()));
		map.put("social_credit_code", nz(c.getSocialCreditCode()));
		map.put("social_credit_code_expires", nz(c.getSocialCreditCodeExpires()));
		map.put("business_scope", nz(c.getBusinessScope()));
		map.put("legal_person", nz(c.getLegalPerson()));
		map.put("legal_cert_id", nz(c.getLegalCertId()));
		map.put("legal_cert_id_expires", nz(c.getLegalCertIdExpires()));
		map.put("legal_mp", nz(c.getLegalMp()));
		map.put("address", nz(c.getAddress()));
		map.put("zip_code", nz(c.getZipCode()));
		map.put("telphone", nz(c.getTelphone()));
		map.put("email", nz(c.getEmail()));
		map.put("attach_file", nz(c.getAttachFile()));
		map.put("attach_file_name", nz(c.getAttachFileName()));
		map.put("confirm_letter_file", nz(c.getConfirmLetterFile()));
		map.put("confirm_letter_file_name", nz(c.getConfirmLetterFileName()));
		map.put("bank_code", nz(c.getBankCode()));
		map.put("bank_acct_type", nz(c.getBankAcctType()));
		map.put("card_no", nz(c.getCardNo()));
		map.put("card_name", nz(c.getCardName()));
		map.put("audit_state", nz(c.getAuditState()));
		map.put("audit_desc", nz(c.getAuditDesc()));
		map.put("status", nz(c.getStatus()));
		map.put("error_info", nz(c.getErrorInfo()));
		map.put("create_time", c.getCreateTime());
		map.put("update_time", c.getUpdateTime());
		return map;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static boolean isDealerSubAccount(Map<String, Object> operatorInfo) {
		if (!operatorInfo.containsKey("is_dealer_main")) {
			return false;
		}
		Object v = operatorInfo.get("is_dealer_main");
		if (v == null) {
			return false;
		}
		return !isTruthyDealerMain(v);
	}

	private static boolean isTruthyDealerMain(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim().toLowerCase(Locale.ROOT);
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s) || "false".equals(s)) {
			return false;
		}
		return true;
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
}
