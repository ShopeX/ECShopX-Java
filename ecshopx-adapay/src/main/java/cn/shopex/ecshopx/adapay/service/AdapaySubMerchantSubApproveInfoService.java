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
import cn.shopex.ecshopx.adapay.domain.AdapayEntryApply;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayRegions;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayEntryApplyMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayRegionsMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.LocalDeliveryDistributorInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapaySubMerchantSubApproveInfoService {

	private final AdapayEntryApplyMapper adapayEntryApplyMapper;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayCorpMemberMapper adapayCorpMemberMapper;
	private final AdapayRegionsMapper adapayRegionsMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorInfoResolveService distributorInfoResolveService;
	private final LocalDeliveryDistributorInfoService localDeliveryDistributorInfoService;
	private final AdapaySubMerchantLastIsSmsRedisWriter adapaySubMerchantLastIsSmsRedisWriter;
	private final String qqmapKey;

	public AdapaySubMerchantSubApproveInfoService(
			AdapayEntryApplyMapper adapayEntryApplyMapper,
			AdapayMemberMapper adapayMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayCorpMemberMapper adapayCorpMemberMapper,
			AdapayRegionsMapper adapayRegionsMapper,
			OperatorsQueryService operatorsQueryService,
			DistributorInfoResolveService distributorInfoResolveService,
			LocalDeliveryDistributorInfoService localDeliveryDistributorInfoService,
			AdapaySubMerchantLastIsSmsRedisWriter adapaySubMerchantLastIsSmsRedisWriter,
			ObjectMapper objectMapper,
			@Value("${common.qqmap-key:}") String qqmapKey) {
		this.adapayEntryApplyMapper = adapayEntryApplyMapper;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
		this.adapayRegionsMapper = adapayRegionsMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorInfoResolveService = distributorInfoResolveService;
		this.localDeliveryDistributorInfoService = localDeliveryDistributorInfoService;
		this.adapaySubMerchantLastIsSmsRedisWriter = adapaySubMerchantLastIsSmsRedisWriter;
		Objects.requireNonNull(objectMapper, "objectMapper");
		this.qqmapKey = qqmapKey != null ? qqmapKey : "";
	}

	public Map<String, Object> subApproveInfo(long companyId, String applyIdRaw, String requestLang) {
		String trimmedApply = applyIdRaw == null ? "" : applyIdRaw.trim();
		if (trimmedApply.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		long applyId;
		try {
			applyId = Long.parseLong(trimmedApply);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		AdapayEntryApply apply =
				adapayEntryApplyMapper.selectOne(
						new LambdaQueryWrapper<AdapayEntryApply>()
								.eq(AdapayEntryApply::getId, applyId)
								.eq(AdapayEntryApply::getCompanyId, String.valueOf(companyId)));
		if (apply == null) {
			throw new ResourceException("申请记录不存在");
		}

		Map<String, Object> entryApplyInfo = entryApplyToSnakeMap(apply);

		String entryIdStr = apply.getEntryId();
		if (entryIdStr == null || entryIdStr.trim().isEmpty()) {
			throw new BadRequestException("申请关联的 entry_id 格式非法");
		}
		long entryMemberPk;
		try {
			entryMemberPk = Long.parseLong(entryIdStr.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("申请关联的 entry_id 格式非法");
		}

		AdapayMember member =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getId, entryMemberPk));
		if (member == null) {
			throw new ResourceException("没有开户详情");
		}

		long savedOperatorId = member.getOperatorId() == null ? 0L : member.getOperatorId().longValue();

		Map<String, Object> entryInfo = new LinkedHashMap<>(memberEntityToSnakeMap(member));

		AdapaySettleAccount settle =
				adapaySettleAccountMapper.selectOne(
						new LambdaQueryWrapper<AdapaySettleAccount>()
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.eq(AdapaySettleAccount::getMemberId, member.getId())
								.orderByDesc(AdapaySettleAccount::getId)
								.last("LIMIT 1"));
		if (settle != null) {
			entryInfo.put("bank_card_id", nz(settle.getCardId()));
			entryInfo.put("bank_card_name", nz(settle.getCardName()));
			entryInfo.put("bank_cert_id", nz(settle.getCertId()));
			entryInfo.put("bank_tel_no", nz(settle.getTelNo()));
			entryInfo.put("bank_name", nz(settle.getBankName()));
		}

		if ("corp".equals(String.valueOf(entryInfo.getOrDefault("member_type", "")).trim())) {
			AdapayCorpMember corp =
					adapayCorpMemberMapper.selectOne(
							new LambdaQueryWrapper<AdapayCorpMember>()
									.eq(AdapayCorpMember::getMemberId, member.getId())
									.orderByDesc(AdapayCorpMember::getId)
									.last("LIMIT 1"));
			if (corp != null) {
				String provName = requireAreaName(corp.getProvCode());
				String areaName = requireAreaName(corp.getAreaCode());
				Map<String, Object> corpMap = corpEntityToSnakeMap(corp);
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>(entryInfo);
				merged.putAll(corpMap);
				merged.put("area", provName + "-" + areaName);
				merged.put("operator_id", savedOperatorId);
				entryInfo = merged;
			}
		}

		Map<String, Object> distributorInfo = null;
		Map<String, Object> dealerInfo = null;
		boolean isRelDealer = false;

		String applyType = apply.getApplyType() == null ? "" : apply.getApplyType().trim();
		if ("dealer".equals(applyType)) {
			Map<String, Object> opFilter = new LinkedHashMap<>();
			opFilter.put("company_id", companyId);
			opFilter.put("operator_id", entryInfo.get("operator_id"));
			Map<String, Object> operatorsInfo = operatorsQueryService.getInfo(opFilter);
			if (operatorsInfo == null || operatorsInfo.isEmpty()) {
				throw new ResourceException("没有账号信息");
			}
			dealerInfo = new LinkedHashMap<>();
			dealerInfo.put("operator_id", operatorsInfo.get("operator_id"));
			dealerInfo.put("mobile", operatorsInfo.get("mobile"));
			dealerInfo.put("username", operatorsInfo.get("username"));
			dealerInfo.put("head_portrait", operatorsInfo.get("head_portrait"));
			dealerInfo.put("split_ledger_info", operatorsInfo.get("split_ledger_info"));
		} else if ("distributor".equals(applyType)) {
			boolean isLocalDelivery = localDeliveryDistributorInfoService.isLocalDeliveryOpen(companyId);
			long distributorId = savedOperatorId;
			Map<String, Object> storeRow =
					distributorInfoResolveService
							.resolveStoreDetail(companyId, distributorId, requestLang)
							.orElse(null);
			if (storeRow == null) {
				throw new ResourceException("请选择店铺");
			}
			distributorInfo = new LinkedHashMap<>(storeRow);
			distributorInfo.put("business_list", localDeliveryDistributorInfoService.businessList());
			distributorInfo.put("is_local_delivery", isLocalDelivery);
			Object ra = distributorInfo.get("regionauth_id");
			if (isUnsetRegionauthId(ra)) {
				distributorInfo.put("regionauth_id", "");
			}
			distributorInfo.put("qqmapimg", buildQqMapImgUrl(distributorInfo.get("lat"), distributorInfo.get("lng")));
			long dealerId = longOf(distributorInfo.get("dealer_id"));
			if (dealerId != 0L) {
				isRelDealer = true;
				LinkedHashMap<String, Object> opFilter2 = new LinkedHashMap<>();
				opFilter2.put("company_id", companyId);
				opFilter2.put("operator_id", dealerId);
				Map<String, Object> op2 = operatorsQueryService.getInfo(opFilter2);
				if (op2 == null || op2.isEmpty()) {
					throw new ResourceException("没有账号信息");
				}
				Map<String, Object> innerDealer = buildDealerSummary(op2);
				distributorInfo.put("dealer_info", innerDealer);
				dealerInfo = innerDealer;
			} else {
				distributorInfo.put("dealer_info", null);
				dealerInfo = null;
			}
		}

		LinkedHashMap<String, Object> rs = new LinkedHashMap<>();
		rs.put("entry_apply_info", entryApplyInfo);
		rs.put("entry_info", entryInfo);
		rs.put("distributor_info", distributorInfo);
		rs.put("is_rel_dealer", isRelDealer);
		rs.put("dealer_info", dealerInfo);
		rs.put("headquarters_adapay_fee_mode", "to do..");
		rs.put("last_is_sms", adapaySubMerchantLastIsSmsRedisWriter.getLastIsSms(companyId));
		return rs;
	}

	public Map<String, Object> toSnakeMapForSubApproveEntryApply(AdapayEntryApply apply) {
		return entryApplyToSnakeMap(apply);
	}

	private Map<String, Object> entryApplyToSnakeMap(AdapayEntryApply apply) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (apply.getId() != null) {
			map.put("id", apply.getId());
		}
		if (apply.getUserName() != null) {
			map.put("user_name", apply.getUserName());
		}
		if (apply.getCompanyId() != null) {
			map.put("company_id", apply.getCompanyId());
		}
		if (apply.getEntryId() != null) {
			map.put("entry_id", apply.getEntryId());
		}
		if (apply.getApplyType() != null) {
			map.put("apply_type", apply.getApplyType());
		}
		map.put("address", apply.getAddress());
		map.put("comments", apply.getComments());
		map.put("is_sms", apply.getIsSms());
		if (apply.getCreateTime() != null) {
			map.put("create_time", apply.getCreateTime());
		}
		if (apply.getStatus() != null) {
			map.put("status", apply.getStatus());
		}
		if (apply.getUpdateTime() != null) {
			map.put("update_time", apply.getUpdateTime());
		}
		return map;
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

	private String buildQqMapImgUrl(Object latObj, Object lngObj) {
		String lat =
				latObj != null && StringUtils.hasText(latObj.toString())
						? latObj.toString().trim()
						: "39.908739";
		String lng =
				lngObj != null && StringUtils.hasText(lngObj.toString())
						? lngObj.toString().trim()
						: "116.397513";
		String latlng = lat + "," + lng;
		return "http://apis.map.qq.com/ws/staticmap/v2/?"
				+ "key="
				+ qqmapKey
				+ "&size=500x249"
				+ "&zoom=16"
				+ "&center="
				+ latlng
				+ "&markers=color:blue|label:A|"
				+ latlng;
	}

	private static boolean isUnsetRegionauthId(Object ra) {
		if (ra == null) {
			return true;
		}
		if (ra instanceof BigDecimal bd) {
			return bd.signum() == 0;
		}
		if (ra instanceof Number n) {
			if (n instanceof Double d) {
				return d.doubleValue() == 0.0;
			}
			if (n instanceof Float f) {
				return f.floatValue() == 0.0f;
			}
			return n.longValue() == 0L;
		}
		String s = ra.toString().trim();
		return !StringUtils.hasText(s) || "0".equals(s);
	}

	private static long longOf(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> buildDealerSummary(Map<String, Object> op) {
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("operator_id", op.get("operator_id"));
		d.put("mobile", op.get("mobile"));
		d.put("username", op.get("username"));
		d.put("head_portrait", op.get("head_portrait"));
		d.put("split_ledger_info", op.get("split_ledger_info"));
		return d;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
