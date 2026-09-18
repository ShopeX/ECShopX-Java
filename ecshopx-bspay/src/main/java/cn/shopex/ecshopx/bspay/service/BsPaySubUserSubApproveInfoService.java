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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.domain.RegionsThird;
import cn.shopex.ecshopx.bspay.domain.UserCard;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.RegionsThirdMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.LocalDeliveryDistributorInfoService;
import cn.shopex.ecshopx.orders.domain.CompanyRelDada;
import cn.shopex.ecshopx.orders.mapper.CompanyRelDadaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BsPaySubUserSubApproveInfoService {

	private static final Map<String, String> ENT_TYPE_LABEL = new LinkedHashMap<>();

	static {
		ENT_TYPE_LABEL.put("1", "政府机构");
		ENT_TYPE_LABEL.put("2", "国营企业");
		ENT_TYPE_LABEL.put("3", "私营企业");
		ENT_TYPE_LABEL.put("4", "外资企业");
		ENT_TYPE_LABEL.put("5", "个体工商户");
		ENT_TYPE_LABEL.put("6", "其它组织");
		ENT_TYPE_LABEL.put("7", "事业单位");
		ENT_TYPE_LABEL.put("8", "集体经济");
	}

	private final EntryApplyMapper entryApplyMapper;
	private final UserEntMapper userEntMapper;
	private final UserIndvMapper userIndvMapper;
	private final UserCardMapper userCardMapper;
	private final RegionsThirdMapper regionsThirdMapper;
	private final OperatorsMapper operatorsMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorInfoResolveService distributorInfoResolveService;
	private final LocalDeliveryDistributorInfoService localDeliveryDistributorInfoService;
	private final CompanyRelDadaMapper companyRelDadaMapper;
	private final String qqmapKey;

	public BsPaySubUserSubApproveInfoService(
			EntryApplyMapper entryApplyMapper,
			UserEntMapper userEntMapper,
			UserIndvMapper userIndvMapper,
			UserCardMapper userCardMapper,
			RegionsThirdMapper regionsThirdMapper,
			OperatorsMapper operatorsMapper,
			OperatorsQueryService operatorsQueryService,
			DistributorInfoResolveService distributorInfoResolveService,
			LocalDeliveryDistributorInfoService localDeliveryDistributorInfoService,
			CompanyRelDadaMapper companyRelDadaMapper,
			@Value("${common.qqmap-key:}") String qqmapKey) {
		this.entryApplyMapper = entryApplyMapper;
		this.userEntMapper = userEntMapper;
		this.userIndvMapper = userIndvMapper;
		this.userCardMapper = userCardMapper;
		this.regionsThirdMapper = regionsThirdMapper;
		this.operatorsMapper = operatorsMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorInfoResolveService = distributorInfoResolveService;
		this.localDeliveryDistributorInfoService = localDeliveryDistributorInfoService;
		this.companyRelDadaMapper = companyRelDadaMapper;
		this.qqmapKey = qqmapKey != null ? qqmapKey : "";
	}

	public Map<String, Object> toSnakeMapForSubApproveEntryApply(EntryApply apply) {
		if (apply == null) {
			return new LinkedHashMap<>();
		}
		return entryApplyToSnakeMap(apply);
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

		EntryApply apply = entryApplyMapper.selectById(applyId);
		if (apply == null) {
			throw new ResourceException("没有开户详情");
		}

		Map<String, Object> entryApplyInfo = entryApplyToSnakeMap(apply);

		String userType = apply.getUserType() == null ? "" : apply.getUserType().trim();
		Map<String, Object> entryInfo;
		long userSubjectPk;

		if ("ent".equalsIgnoreCase(userType)) {
			UserEnt ent = userEntMapper.selectById(parseUserId(apply.getUserId()));
			if (ent == null) {
				throw new ResourceException("没有开户详情");
			}
			userSubjectPk = ent.getId() != null ? ent.getId() : 0L;
			entryInfo = new LinkedHashMap<>(entToSnakeMap(ent));
			entryInfo.put("user_type", userType);
			entryInfo.put("approved_time", apply.getUpdated());
			String entTypeKey = ent.getEntType() == null ? "" : Integer.toString(ent.getEntType());
			entryInfo.put("ent_type_value", ENT_TYPE_LABEL.getOrDefault(entTypeKey, ""));
			String regArea =
					areaName(ent.getRegProvId())
							+ "-"
							+ areaName(ent.getRegAreaId())
							+ "-"
							+ areaName(ent.getRegDistrictId());
			entryInfo.put("reg_area", regArea);
		} else if ("indv".equalsIgnoreCase(userType)) {
			UserIndv indv = userIndvMapper.selectById(parseUserId(apply.getUserId()));
			if (indv == null) {
				throw new ResourceException("没有开户详情");
			}
			userSubjectPk = indv.getId() != null ? indv.getId() : 0L;
			entryInfo = new LinkedHashMap<>(indvToSnakeMap(indv));
			entryInfo.put("user_type", userType);
			entryInfo.put("approved_time", apply.getUpdated());
		} else {
			throw new ResourceException("获取用户进件信息失败");
		}

		UserCard card =
				userCardMapper.selectOne(
						new LambdaQueryWrapper<UserCard>()
								.eq(UserCard::getCompanyId, companyId)
								.eq(UserCard::getUserId, userSubjectPk)
								.eq(UserCard::getUserType, userType)
								.last("LIMIT 1"));
		if (card != null) {
			entryInfo.put("card_no", nz(card.getCardNo()));
			entryInfo.put("card_name", nz(card.getCardName()));
			entryInfo.put("bank_cert_no", nz(card.getCertNo()));
			entryInfo.put("mp", nz(card.getMp()));
			entryInfo.put("card_type", nz(card.getCardType()));
			entryInfo.put("bank_code", nz(card.getBankCode()));
			entryInfo.put("branch_name", nz(card.getBranchName()));
			String cardArea = areaName(card.getProvId()) + "-" + areaName(card.getAreaId());
			entryInfo.put("card_area", cardArea);
			if ("indv".equalsIgnoreCase(userType)) {
				entryInfo.put("bank_card_id", nz(card.getCardNo()));
				entryInfo.put("bank_card_name", nz(card.getCardName()));
				entryInfo.put("bank_cert_id", nz(card.getCertNo()));
				entryInfo.put("bank_tel_no", nz(card.getMp()));
			}
		}

		String memberType = "ent".equalsIgnoreCase(userType) ? "corp" : "person";
		entryInfo.put("member_type", memberType);
		if ("ent".equalsIgnoreCase(userType)) {
			entryInfo.put("legal_person", nz(str(entryInfo.get("legal_name"))));
			entryInfo.put("legal_cert_id", nz(str(entryInfo.get("legal_cert_no"))));
			entryInfo.put("tel_no", nz(str(entryInfo.get("contact_mobile"))));
		} else {
			entryInfo.put("user_name", nz(str(entryInfo.get("name"))));
			entryInfo.put("cert_id", nz(str(entryInfo.get("cert_no"))));
			entryInfo.put("tel_no", nz(str(entryInfo.get("mobile_no"))));
		}

		Map<String, Object> distributorInfo = null;
		Map<String, Object> dealerInfo = null;
		Map<String, Object> merchantInfo = null;
		boolean isRelDealer = false;
		boolean isRelMerchant = false;

		String ot = apply.getOperatorType() == null ? "" : apply.getOperatorType().trim().toLowerCase(Locale.ROOT);
		if ("merchant".equals(ot)) {
			Operators op =
					operatorsMapper.selectOne(
							new LambdaQueryWrapper<Operators>()
									.eq(Operators::getCompanyId, companyId)
									.eq(Operators::getMerchantId, longOrZero(apply.getOperatorId()))
									.eq(Operators::getOperatorType, "merchant")
									.eq(Operators::getIsMerchantMain, Boolean.TRUE)
									.last("LIMIT 1"));
			if (op == null) {
				throw new ResourceException("没有账号信息");
			}
			Map<String, Object> opRow = operatorsEntityToSummarySource(op);
			merchantInfo = operatorSummaryFromEntity(opRow);
		} else if ("dealer".equals(ot) || "supplier".equals(ot)) {
			Map<String, Object> f = new LinkedHashMap<>();
			f.put("company_id", companyId);
			f.put("operator_id", apply.getOperatorId());
			Map<String, Object> dealerRow = operatorsQueryService.getInfo(f);
			if (dealerRow == null || dealerRow.isEmpty()) {
				throw new ResourceException("没有账号信息");
			}
			dealerInfo = operatorSummaryFromEntity(dealerRow);
		} else if ("distributor".equals(ot)) {
			long distributorId = apply.getOperatorId() == null ? 0L : apply.getOperatorId().longValue();
			Map<String, Object> storeRow =
					distributorInfoResolveService
							.resolveStoreDetail(companyId, distributorId, requestLang)
							.orElse(null);
			if (storeRow == null) {
				throw new ResourceException("请选择店铺");
			}
			distributorInfo = new LinkedHashMap<>(storeRow);
			distributorInfo.put("business_list", localDeliveryDistributorInfoService.businessList());
			CompanyRelDada dadaRow =
					companyRelDadaMapper.selectOne(
							new LambdaQueryWrapper<CompanyRelDada>()
									.eq(CompanyRelDada::getCompanyId, companyId)
									.last("LIMIT 1"));
			distributorInfo.put(
					"company_dada_open", dadaRow != null && Boolean.TRUE.equals(dadaRow.getIsOpen()));
			Object ra = distributorInfo.get("regionauth_id");
			if (isUnsetRegionauthId(ra)) {
				distributorInfo.put("regionauth_id", "");
			}
			distributorInfo.put(
					"qqmapimg", buildQqMapImgUrl(distributorInfo.get("lat"), distributorInfo.get("lng")));
			long merchantId = longOf(distributorInfo.get("merchant_id"));
			if (merchantId != 0L) {
				isRelMerchant = true;
				Operators op =
						operatorsMapper.selectOne(
								new LambdaQueryWrapper<Operators>()
										.eq(Operators::getCompanyId, companyId)
										.eq(Operators::getMerchantId, merchantId)
										.eq(Operators::getOperatorType, "merchant")
										.eq(Operators::getIsMerchantMain, Boolean.TRUE)
										.last("LIMIT 1"));
				if (op == null) {
					throw new ResourceException("没有账号信息");
				}
				Map<String, Object> opRow = operatorsEntityToSummarySource(op);
				Map<String, Object> relMerchantSummary = operatorSummaryFromEntity(opRow);
				distributorInfo.put("merchant_info", relMerchantSummary);
				merchantInfo = relMerchantSummary;
			} else {
				long dealerId = longOf(distributorInfo.get("dealer_id"));
				if (dealerId != 0L) {
					isRelDealer = true;
					Map<String, Object> opFilter2 = new LinkedHashMap<>();
					opFilter2.put("company_id", companyId);
					opFilter2.put("operator_id", dealerId);
					Map<String, Object> op2 = operatorsQueryService.getInfo(opFilter2);
					if (op2 == null || op2.isEmpty()) {
						throw new ResourceException("没有账号信息");
					}
					Map<String, Object> relDealerSummary = operatorSummaryFromEntity(op2);
					distributorInfo.put("dealer_info", relDealerSummary);
					dealerInfo = relDealerSummary;
				} else {
					distributorInfo.put("dealer_info", null);
				}
			}
		}

		LinkedHashMap<String, Object> rs = new LinkedHashMap<>();
		rs.put("entry_apply_info", entryApplyInfo);
		rs.put("entry_info", entryInfo);
		rs.put("distributor_info", distributorInfo);
		rs.put("is_rel_dealer", isRelDealer);
		rs.put("is_rel_merchant", isRelMerchant);
		rs.put("dealer_info", dealerInfo);
		rs.put("merchant_info", merchantInfo);
		return rs;
	}

	private Map<String, Object> entryApplyToSnakeMap(EntryApply apply) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("id", apply.getId());
		map.put("user_name", nz(apply.getUserName()));
		map.put("company_id", apply.getCompanyId());
		map.put("user_id", nz(apply.getUserId()));
		map.put("operator_id", apply.getOperatorId());
		map.put("operator_type", nz(apply.getOperatorType()));
		map.put("user_type", nz(apply.getUserType()));
		map.put("address", nz(apply.getAddress()));
		map.put("comments", nz(apply.getComments()));
		map.put("status", nz(apply.getStatus()));
		map.put("created", apply.getCreated());
		map.put("updated", apply.getUpdated());
		return map;
	}

	private String areaName(String code) {
		if (code == null || code.isBlank()) {
			return "";
		}
		String trimmed = code.trim();
		RegionsThird r =
				regionsThirdMapper.selectOne(
						new LambdaQueryWrapper<RegionsThird>()
								.eq(RegionsThird::getAreaCode, trimmed)
								.last("LIMIT 1"));
		if (r == null || r.getAreaName() == null) {
			return "";
		}
		return r.getAreaName();
	}

	private static long parseUserId(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (t.isEmpty()) {
			throw new ResourceException("没有开户详情");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("没有开户详情");
		}
	}

	private static Map<String, Object> entToSnakeMap(UserEnt entity) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", entity.getId());
		map.put("req_seq_id", nz(entity.getReqSeqId()));
		map.put("sys_id", nz(entity.getSysId()));
		map.put("huifu_id", nz(entity.getHuifuId()));
		map.put("company_id", entity.getCompanyId());
		map.put("is_update", entity.getIsUpdate());
		map.put("reg_name", nz(entity.getRegName()));
		map.put("license_code", nz(entity.getLicenseCode()));
		map.put("license_validity_type", entity.getLicenseValidityType());
		map.put("license_begin_date", nz(entity.getLicenseBeginDate()));
		map.put("license_end_date", nz(entity.getLicenseEndDate()));
		map.put("reg_prov_id", nz(entity.getRegProvId()));
		map.put("reg_area_id", nz(entity.getRegAreaId()));
		map.put("reg_district_id", nz(entity.getRegDistrictId()));
		map.put("reg_detail", nz(entity.getRegDetail()));
		map.put("legal_name", nz(entity.getLegalName()));
		map.put("legal_cert_no", nz(entity.getLegalCertNo()));
		map.put("legal_cert_validity_type", entity.getLegalCertValidityType());
		map.put("legal_cert_begin_date", nz(entity.getLegalCertBeginDate()));
		map.put("legal_cert_end_date", nz(entity.getLegalCertEndDate()));
		map.put("contact_name", nz(entity.getContactName()));
		map.put("contact_mobile", nz(entity.getContactMobile()));
		map.put("ent_type", entity.getEntType());
		map.put("audit_state", nz(entity.getAuditState()));
		map.put("audit_desc", nz(entity.getAuditDesc()));
		map.put("error_info", nz(entity.getErrorInfo()));
		map.put("created", entity.getCreated());
		map.put("updated", entity.getUpdated());
		return map;
	}

	private static Map<String, Object> indvToSnakeMap(UserIndv entity) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", entity.getId());
		map.put("req_seq_id", nz(entity.getReqSeqId()));
		map.put("sys_id", nz(entity.getSysId()));
		map.put("huifu_id", nz(entity.getHuifuId()));
		map.put("company_id", entity.getCompanyId());
		map.put("is_update", entity.getIsUpdate());
		map.put("name", nz(entity.getName()));
		map.put("cert_no", nz(entity.getCertNo()));
		map.put("cert_validity_type", entity.getCertValidityType());
		map.put("cert_begin_date", nz(entity.getCertBeginDate()));
		map.put("cert_end_date", nz(entity.getCertEndDate()));
		map.put("mobile_no", nz(entity.getMobileNo()));
		map.put("audit_state", nz(entity.getAuditState()));
		map.put("audit_desc", nz(entity.getAuditDesc()));
		map.put("error_info", nz(entity.getErrorInfo()));
		map.put("created", entity.getCreated());
		map.put("updated", entity.getUpdated());
		return map;
	}

	private static long longOrZero(Integer i) {
		return i == null ? 0L : i.longValue();
	}

	private static Map<String, Object> operatorsEntityToSummarySource(Operators op) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("operator_id", op.getOperatorId());
		m.put("mobile", op.getMobile());
		m.put("username", op.getUsername());
		m.put("head_portrait", op.getHeadPortrait());
		return m;
	}

	private static Map<String, Object> operatorSummaryFromEntity(Map<String, Object> source) {
		Map<String, Object> d = new LinkedHashMap<>();
		if (source == null) {
			return d;
		}
		d.put("operator_id", source.get("operator_id"));
		d.put("mobile", source.get("mobile"));
		d.put("username", source.get("username"));
		d.put("head_portrait", source.get("head_portrait"));
		return d;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String nz(String s) {
		return s == null ? "" : s;
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
}
