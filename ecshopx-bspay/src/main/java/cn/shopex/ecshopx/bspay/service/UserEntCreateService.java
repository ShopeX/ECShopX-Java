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
import cn.shopex.ecshopx.bspay.domain.UserCard;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.support.UnicodeStringLimits;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class UserEntCreateService {

	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private final BsPayPaymentSettingService bsPayPaymentSettingService;
	private final BsPayOperatorResolveService bsPayOperatorResolveService;
	private final UserEntMapper userEntMapper;
	private final UserCardMapper userCardMapper;
	private final EntryApplyMapper entryApplyMapper;

	public UserEntCreateService(
			BsPayPaymentSettingService bsPayPaymentSettingService,
			BsPayOperatorResolveService bsPayOperatorResolveService,
			UserEntMapper userEntMapper,
			UserCardMapper userCardMapper,
			EntryApplyMapper entryApplyMapper) {
		this.bsPayPaymentSettingService = bsPayPaymentSettingService;
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
		this.userEntMapper = userEntMapper;
		this.userCardMapper = userCardMapper;
		this.entryApplyMapper = entryApplyMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void create(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, Object> params = new LinkedHashMap<>(body);
		params.put("company_id", companyId);
		String legalCertNo = str(params.get("legal_cert_no"));
		String cardNo = str(params.get("card_no"));
		params.put("legal_cert_no", legalCertNo);
		params.put("card_no", cardNo);

		validateEntBodyCommon(params);
		normalizeEntBodyParams(params);

		Map<String, Object> setting = bsPayPaymentSettingService.requireSettingMap(companyId);
		String sysId = Objects.toString(setting.get("sys_id"), "").trim();
		if (!StringUtils.hasText(sysId)) {
			throw new ResourceException("请先配置支付信息");
		}
		params.put("sys_id", sysId);

		int now = (int) (System.currentTimeMillis() / 1000);

		log.info("data====>{}", params);

		UserEnt userEnt = new UserEnt();
		userEnt.setCompanyId(companyId);
		userEnt.setSysId(sysId);
		userEnt.setRegName(str(params.get("reg_name")));
		userEnt.setLicenseCode(str(params.get("license_code")));
		userEnt.setLicenseValidityType(parseLicenseValidityTypeForPersistG(params.get("license_validity_type")));
		userEnt.setLicenseBeginDate(str(params.get("license_begin_date")));
		userEnt.setLicenseEndDate(str(params.get("license_end_date")));
		userEnt.setRegProvId(str(params.get("reg_prov_id")));
		userEnt.setRegAreaId(str(params.get("reg_area_id")));
		userEnt.setRegDistrictId(str(params.get("reg_district_id")));
		userEnt.setRegDetail(str(params.get("reg_detail")));
		userEnt.setLegalName(str(params.get("legal_name")));
		userEnt.setLegalCertNo(legalCertNo);
		userEnt.setLegalCertValidityType(parseLegalCertValidityTypeForPersistH(params.get("legal_cert_validity_type")));
		userEnt.setLegalCertBeginDate(str(params.get("legal_cert_begin_date")));
		userEnt.setLegalCertEndDate(str(params.get("legal_cert_end_date")));
		userEnt.setContactName(str(params.get("contact_name")));
		userEnt.setContactMobile(str(params.get("contact_mobile")));
		userEnt.setEntType(parseEntTypeForPersistF(params.get("ent_type")));
		userEnt.setCreated(now);
		userEnt.setUpdated(now);

		int entRows = userEntMapper.insert(userEnt);
		if (entRows <= 0 || userEnt.getId() == null) {
			throw new ResourceException("企业用户信息保存失败");
		}

		log.info("userRes====>{}", userEnt.getId());

		int cardTypeInt = parseCardTypeForPersistE(params.get("card_type"));
		Map<String, Object> cardInfoMap = new LinkedHashMap<>();
		cardInfoMap.put("sys_id", sysId);
		cardInfoMap.put("user_id", userEnt.getId());
		cardInfoMap.put("company_id", companyId);
		cardInfoMap.put("user_type", "ent");
		cardInfoMap.put("card_type", String.valueOf(cardTypeInt));
		cardInfoMap.put("card_name", str(params.get("legal_name")));
		cardInfoMap.put("card_no", str(params.get("card_no")));
		cardInfoMap.put("prov_id", str(params.get("prov_id")));
		cardInfoMap.put("area_id", str(params.get("area_id")));
		cardInfoMap.put("bank_code", str(params.get("bank_code")));
		cardInfoMap.put("branch_name", str(params.get("branch_name")));
		cardInfoMap.put("cert_no", legalCertNo);
		cardInfoMap.put("cert_validity_type", params.get("legal_cert_validity_type"));
		cardInfoMap.put("cert_begin_date", str(params.get("legal_cert_begin_date")));
		cardInfoMap.put("cert_end_date", str(params.get("legal_cert_end_date")));
		cardInfoMap.put("mp", str(params.get("mp")));

		log.info("cardInfo====>{}", cardInfoMap);

		UserCard userCard = new UserCard();
		userCard.setSysId(sysId);
		userCard.setUserId(userEnt.getId());
		userCard.setCompanyId(companyId);
		userCard.setUserType("ent");
		userCard.setCardType(String.valueOf(cardTypeInt));
		userCard.setCardName(str(params.get("legal_name")));
		userCard.setCardNo(str(params.get("card_no")));
		userCard.setProvId(str(params.get("prov_id")));
		userCard.setAreaId(str(params.get("area_id")));
		userCard.setBankCode(str(params.get("bank_code")));
		userCard.setBranchName(str(params.get("branch_name")));
		userCard.setCertNo(legalCertNo);
		userCard.setCertValidityType(parseLegalCertValidityTypeForPersistH(params.get("legal_cert_validity_type")));
		userCard.setCertBeginDate(str(params.get("legal_cert_begin_date")));
		userCard.setCertEndDate(str(params.get("legal_cert_end_date")));
		userCard.setMp(str(params.get("mp")));
		userCard.setCreated(now);
		userCard.setUpdated(now);

		int cardInsertRows = userCardMapper.insert(userCard);
		if (cardInsertRows <= 0) {
			throw new ResourceException("结算卡创建失败");
		}
		log.info("cardRes====>{}", cardInsertRows);

		BsPayOperatorResolveService.OperatorContext opCtx = bsPayOperatorResolveService.resolve(jwtMap);

		Map<String, Object> applyMap = new LinkedHashMap<>();
		applyMap.put("user_type", "ent");
		applyMap.put("user_name", str(params.get("reg_name")));
		applyMap.put("company_id", companyId);
		applyMap.put("user_id", String.valueOf(userEnt.getId()));
		applyMap.put("operator_id", opCtx.operatorId());
		applyMap.put("operator_type", opCtx.operatorType());
		applyMap.put("address", "");
		applyMap.put("status", "WAIT_APPROVE");

		log.info("apply====>{}", applyMap);

		EntryApply entryApply = new EntryApply();
		entryApply.setUserType("ent");
		entryApply.setUserName(str(params.get("reg_name")));
		entryApply.setCompanyId(companyId);
		entryApply.setUserId(String.valueOf(userEnt.getId()));
		entryApply.setOperatorId(opCtx.operatorId());
		entryApply.setOperatorType(opCtx.operatorType());
		entryApply.setAddress("");
		entryApply.setStatus("WAIT_APPROVE");
		entryApply.setCreated(now);
		entryApply.setUpdated(now);

		int applyInsertRows = entryApplyMapper.insert(entryApply);
		if (applyInsertRows <= 0) {
			throw new ResourceException("开户申请创建失败");
		}
		log.info("applyRes====>{}", applyInsertRows);
	}

	void parseRequiredEntBodyEntId(Map<String, Object> params) {
		Object idRaw = params.get("id");
		if (idRaw == null || !StringUtils.hasText(str(idRaw))) {
			throw new BadRequestException("Id必填");
		}
		long parsedId;
		try {
			if (idRaw instanceof Number n) {
				parsedId = n.longValue();
			} else if (idRaw instanceof String s) {
				String t = s.trim();
				if (!StringUtils.hasText(t)) {
					throw new BadRequestException("Id必填");
				}
				parsedId = Long.parseLong(t);
			} else {
				throw new BadRequestException("Id必填");
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("Id必填");
		}
		if (parsedId <= 0L) {
			throw new BadRequestException("Id必填");
		}
		params.put("id", parsedId);
	}

	public void validateEntModifyBody(Map<String, Object> params) {
		parseRequiredEntBodyEntId(params);
		validateEntBodyCommon(params);
	}

	public void validateEntUpdateBody(Map<String, Object> params) {
		parseRequiredEntBodyEntId(params);
		if (!StringUtils.hasText(str(params.get("reg_name")))) {
			throw new BadRequestException("企业名称必填");
		}
		if (!StringUtils.hasText(str(params.get("license_code")))) {
			throw new BadRequestException("营业执照号必填");
		}
		int lvt = parseLicenseValidityTypeForUpdateRule(params.get("license_validity_type"));
		if (!StringUtils.hasText(str(params.get("license_begin_date")))) {
			throw new BadRequestException("营业执照有效期起始日期必填");
		}
		if (lvt == 0 && !StringUtils.hasText(str(params.get("license_end_date")))) {
			throw new BadRequestException("营业执照有效期结束日期必填");
		}
		if (isMissingLicenseRegionsId(params.get("license_regions_id"))) {
			throw new BadRequestException("注册地区必填");
		}
		if (!StringUtils.hasText(str(params.get("reg_detail")))) {
			throw new BadRequestException("注册地址必填");
		}
		if (!StringUtils.hasText(str(params.get("legal_name")))) {
			throw new BadRequestException("法人姓名必填");
		}
		String legalCertNo = str(params.get("legal_cert_no"));
		if (legalCertNo.length() != 18) {
			throw new BadRequestException("法人身份证号码必须是18位");
		}
		int lcvt = parseLegalCertValidityTypeForUpdateRule(params.get("legal_cert_validity_type"));
		if (!StringUtils.hasText(str(params.get("legal_cert_begin_date")))) {
			throw new BadRequestException("法人身份证有效期起始日期必填");
		}
		if (lcvt == 0 && !StringUtils.hasText(str(params.get("legal_cert_end_date")))) {
			throw new BadRequestException("法人身份证有效期截止日期必填");
		}
		String contactMobile = str(params.get("contact_mobile"));
		if (contactMobile.length() != 11) {
			throw new BadRequestException("联系人手机必须是11位");
		}
		if (!StringUtils.hasText(str(params.get("ent_type")))) {
			throw new BadRequestException("公司类型必填");
		}
		int cardTypeInt = parseCardTypeForRuleUpdate(params.get("card_type"));
		String cardName = str(params.get("card_name"));
		if (!StringUtils.hasText(cardName)) {
			throw new BadRequestException("持卡人姓名必填");
		}
		if (cardName.codePointCount(0, cardName.length()) > 20) {
			throw new BadRequestException("持卡人姓名不能超过20个字符");
		}
		if (!StringUtils.hasText(str(params.get("card_no")))) {
			throw new BadRequestException("银行卡号必填");
		}
		if (isMissingCardRegionsId(params.get("card_regions_id"))) {
			throw new BadRequestException("银行卡开户地区必填");
		}
		if (cardTypeInt == 0) {
			if (!StringUtils.hasText(str(params.get("bank_code")))) {
				throw new BadRequestException("银行号必填");
			}
		}
		if (cardTypeInt == 1) {
			if (!StringUtils.hasText(str(params.get("cert_no")))) {
				throw new BadRequestException("持卡人身份证号必填");
			}
		}
		int holderCertVt = parseCertValidityTypeForUpdateRule(params.get("cert_validity_type"));
		if (!StringUtils.hasText(str(params.get("cert_begin_date")))) {
			throw new BadRequestException("持卡人身份证有效期起始日期必填");
		}
		if (holderCertVt == 0 && !StringUtils.hasText(str(params.get("cert_end_date")))) {
			throw new BadRequestException("持卡人身份证有效期截止日期必填");
		}
		String mp = str(params.get("mp"));
		if (mp.length() != 11) {
			throw new BadRequestException("银行卡绑定手机号必须是11位");
		}
	}

	public Map<String, Object> normalizeEntBodyParams(Map<String, Object> params) {
		normalizeLicenseRegions(params);
		String regDetail = str(params.get("reg_detail"));
		if (StringUtils.hasText(regDetail)) {
			params.put("reg_detail", UnicodeStringLimits.limitCodePoints(regDetail, 60));
		}
		String legalName = str(params.get("legal_name"));
		if (StringUtils.hasText(legalName)) {
			params.put("legal_name", UnicodeStringLimits.limitCodePoints(legalName, 20));
		}
		params.put("contact_name", params.get("legal_name"));

		int ct = parseCardTypeForRuleD(params.get("card_type"));
		if (ct == 1) {
			params.put("card_name", str(params.get("legal_name")));
		} else {
			params.put("card_name", str(params.get("reg_name")));
		}

		String legalCertNo = str(params.get("legal_cert_no"));
		if (StringUtils.hasText(legalCertNo) && !LEGAL_CERT_PATTERN.matcher(legalCertNo).matches()) {
			throw new BadRequestException("法人身份证号码格式错误");
		}

		normalizeCardRegions(params);
		return params;
	}

	public Map<String, Object> normalizeEntBodyParamsForUpdate(Map<String, Object> params) {
		normalizeLicenseRegions(params);
		String regDetail = str(params.get("reg_detail"));
		if (StringUtils.hasText(regDetail)) {
			params.put("reg_detail", UnicodeStringLimits.limitCodePoints(regDetail, 60));
		}
		String legalName = str(params.get("legal_name"));
		if (StringUtils.hasText(legalName)) {
			params.put("legal_name", UnicodeStringLimits.limitCodePoints(legalName, 20));
		}
		params.put("contact_name", params.get("legal_name"));

		int ct = parseCardTypeForRuleUpdate(params.get("card_type"));
		if (ct == 1) {
			params.put("card_name", str(params.get("legal_name")));
		} else {
			params.put("card_name", str(params.get("reg_name")));
		}

		String legalCertNoNorm = str(params.get("legal_cert_no"));
		if (StringUtils.hasText(legalCertNoNorm) && !LEGAL_CERT_PATTERN.matcher(legalCertNoNorm).matches()) {
			throw new BadRequestException("法人身份证号码格式错误");
		}

		normalizeCardRegions(params);
		return params;
	}

	public int persistLicenseValidityType(Object raw) {
		return parseLicenseValidityTypeForPersistG(raw);
	}

	public int persistLegalCertValidityType(Object raw) {
		return parseLegalCertValidityTypeForPersistH(raw);
	}

	public int persistEntType(Object raw) {
		return parseEntTypeForPersistF(raw);
	}

	public int persistCardType(Object raw) {
		return parseCardTypeForPersistE(raw);
	}

	private void validateEntBodyCommon(Map<String, Object> params) {
		if (!StringUtils.hasText(str(params.get("reg_name")))) {
			throw new BadRequestException("企业名称必填");
		}
		if (!StringUtils.hasText(str(params.get("license_code")))) {
			throw new BadRequestException("统一社会信用代码必填");
		}
		int lvt = parseLicenseValidityTypeForRuleA(params.get("license_validity_type"));
		if (!StringUtils.hasText(str(params.get("license_begin_date")))) {
			throw new BadRequestException("成立日期必填");
		}
		if (lvt == 0 && !StringUtils.hasText(str(params.get("license_end_date")))) {
			throw new BadRequestException("营业期限必填");
		}
		if (isMissingLicenseRegionsId(params.get("license_regions_id"))) {
			throw new BadRequestException("住所省市区必填");
		}
		if (!StringUtils.hasText(str(params.get("reg_detail")))) {
			throw new BadRequestException("住所详细地址地址必填");
		}
		if (!StringUtils.hasText(str(params.get("legal_name")))) {
			throw new BadRequestException("法人姓名必填");
		}
		String legalCertNo = str(params.get("legal_cert_no"));
		if (legalCertNo.length() != 18) {
			throw new BadRequestException("法人身份证号码必须是18位");
		}
		int lcvt = parseLegalCertValidityTypeForRuleB(params.get("legal_cert_validity_type"));
		if (!StringUtils.hasText(str(params.get("legal_cert_begin_date")))) {
			throw new BadRequestException("法人身份证开始有效期必填");
		}
		if (lcvt == 0 && !StringUtils.hasText(str(params.get("legal_cert_end_date")))) {
			throw new BadRequestException("法人身份证结束有效期必填");
		}
		String contactMobile = str(params.get("contact_mobile"));
		if (contactMobile.length() != 11) {
			throw new BadRequestException("手机必须是11位");
		}
		if (!StringUtils.hasText(str(params.get("ent_type")))) {
			throw new BadRequestException("企业类型必填");
		}
		if (!StringUtils.hasText(str(params.get("card_no")))) {
			throw new BadRequestException("银行卡号必填");
		}
		if (isMissingCardRegionsId(params.get("card_regions_id"))) {
			throw new BadRequestException("开户行所在省市必填");
		}
		int cardTypeInt = parseCardTypeForRuleC(params.get("card_type"));
		if (cardTypeInt == 0) {
			if (!StringUtils.hasText(str(params.get("bank_code")))) {
				throw new BadRequestException("银行号必填");
			}
			if (!StringUtils.hasText(str(params.get("branch_name")))) {
				throw new BadRequestException("支行名称必填");
			}
		}
		String mp = str(params.get("mp"));
		if (mp.length() != 11) {
			throw new BadRequestException("银行卡绑定手机号必须是11位");
		}
	}

	private static boolean isMissingLicenseRegionsId(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s);
		}
		return false;
	}

	private static boolean isMissingCardRegionsId(Object v) {
		return isMissingLicenseRegionsId(v);
	}

	private void normalizeLicenseRegions(Map<String, Object> params) {
		Object raw = params.get("license_regions_id");
		List<String> parts = toStringParts(raw, true);
		if (parts.size() != 3 || parts.stream().anyMatch(p -> !StringUtils.hasText(p))) {
			throw new BadRequestException("住所省市区数据格式错误");
		}
		params.put("reg_prov_id", parts.get(0));
		params.put("reg_area_id", parts.get(1));
		params.put("reg_district_id", parts.get(2));
		params.remove("license_regions_id");
	}

	private void normalizeCardRegions(Map<String, Object> params) {
		Object raw = params.get("card_regions_id");
		List<String> parts = toStringParts(raw, false);
		if (parts.size() != 2 || parts.stream().anyMatch(p -> !StringUtils.hasText(p))) {
			throw new BadRequestException("银行卡开户地区数据格式错误");
		}
		params.put("prov_id", parts.get(0));
		params.put("area_id", parts.get(1));
		params.remove("card_regions_id");
	}

	private static List<String> toStringParts(Object raw, boolean allowCommaString) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<String> out = new ArrayList<>();
			for (Object o : c) {
				out.add(str(o));
			}
			return out;
		}
		if (raw instanceof Object[] a) {
			List<String> out = new ArrayList<>();
			for (Object o : a) {
				out.add(str(o));
			}
			return out;
		}
		if (raw instanceof String s && allowCommaString && s.contains(",")) {
			String[] split = s.split(",", -1);
			List<String> out = new ArrayList<>();
			for (String p : split) {
				out.add(p == null ? "" : p.trim());
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s) && !allowCommaString) {
			if (s.contains(",")) {
				String[] split = s.split(",", -1);
				List<String> out = new ArrayList<>();
				for (String p : split) {
					out.add(p == null ? "" : p.trim());
				}
				return out;
			}
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			return List.of(s.trim());
		}
		return List.of(str(raw));
	}

	private static int parseLicenseValidityTypeForRuleA(Object raw) {
		if (raw == null) {
			throw new BadRequestException("营业期限类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "营业期限类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("营业期限类型必填");
			}
			return intFromString(t, "营业期限类型格式错误");
		}
		throw new BadRequestException("营业期限类型格式错误");
	}

	private static int parseLegalCertValidityTypeForRuleB(Object raw) {
		if (raw == null) {
			throw new BadRequestException("法人身份证有效期类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "法人身份证有效期类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("法人身份证有效期类型必填");
			}
			return intFromString(t, "法人身份证有效期类型格式错误");
		}
		throw new BadRequestException("法人身份证有效期类型格式错误");
	}

	private static int parseCardTypeForRuleC(Object raw) {
		if (raw == null) {
			throw new BadRequestException("结算银行账户类型错误");
		}
		if (raw instanceof Number n) {
			int v = intFromNumber(n, "结算银行账户类型错误");
			if (v != 0 && v != 1) {
				throw new BadRequestException("结算银行账户类型错误");
			}
			return v;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("结算银行账户类型错误");
			}
			int v = intFromString(t, "结算银行账户类型错误");
			if (v != 0 && v != 1) {
				throw new BadRequestException("结算银行账户类型错误");
			}
			return v;
		}
		throw new BadRequestException("结算银行账户类型错误");
	}

	private static int parseCardTypeForRuleD(Object raw) {
		return parseCardTypeForRuleC(raw);
	}

	private static int parseCardTypeForRuleUpdate(Object raw) {
		if (raw == null) {
			throw new BadRequestException("银行卡类型错误");
		}
		if (raw instanceof Number n) {
			int v = intFromNumber(n, "银行卡类型错误");
			if (v != 0 && v != 1 && v != 2) {
				throw new BadRequestException("银行卡类型错误");
			}
			return v;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("银行卡类型错误");
			}
			int v = intFromString(t, "银行卡类型错误");
			if (v != 0 && v != 1 && v != 2) {
				throw new BadRequestException("银行卡类型错误");
			}
			return v;
		}
		throw new BadRequestException("银行卡类型错误");
	}

	private static int parseLicenseValidityTypeForUpdateRule(Object raw) {
		if (raw == null) {
			throw new BadRequestException("营业执照有效期类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "营业执照有效期类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("营业执照有效期类型必填");
			}
			return intFromString(t, "营业执照有效期类型格式错误");
		}
		throw new BadRequestException("营业执照有效期类型格式错误");
	}

	private static int parseLegalCertValidityTypeForUpdateRule(Object raw) {
		if (raw == null) {
			throw new BadRequestException("法人身份证有效期类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "法人身份证有效期类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("法人身份证有效期类型必填");
			}
			return intFromString(t, "法人身份证有效期类型格式错误");
		}
		throw new BadRequestException("法人身份证有效期类型格式错误");
	}

	private static int parseCertValidityTypeForUpdateRule(Object raw) {
		if (raw == null) {
			throw new BadRequestException("持卡人身份证有效期类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "持卡人身份证有效期类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("持卡人身份证有效期类型必填");
			}
			return intFromString(t, "持卡人身份证有效期类型格式错误");
		}
		throw new BadRequestException("持卡人身份证有效期类型格式错误");
	}

	private static int parseCardTypeForPersistE(Object raw) {
		return parseCardTypeForRuleC(raw);
	}

	private static int parseEntTypeForPersistF(Object raw) {
		if (raw == null) {
			throw new BadRequestException("企业类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "企业类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("企业类型必填");
			}
			return intFromString(t, "企业类型格式错误");
		}
		throw new BadRequestException("企业类型格式错误");
	}

	private static int parseLicenseValidityTypeForPersistG(Object raw) {
		if (raw == null) {
			throw new BadRequestException("营业期限类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "营业期限类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("营业期限类型必填");
			}
			return intFromString(t, "营业期限类型格式错误");
		}
		throw new BadRequestException("营业期限类型格式错误");
	}

	private static int parseLegalCertValidityTypeForPersistH(Object raw) {
		if (raw == null) {
			throw new BadRequestException("法人身份证有效期类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "法人身份证有效期类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("法人身份证有效期类型必填");
			}
			return intFromString(t, "法人身份证有效期类型格式错误");
		}
		throw new BadRequestException("法人身份证有效期类型格式错误");
	}

	private static int intFromNumber(Number n, String formatErr) {
		long v = n.longValue();
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new BadRequestException(formatErr);
		}
		return (int) v;
	}

	private static int intFromString(String t, String formatErr) {
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException(formatErr);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}
}
