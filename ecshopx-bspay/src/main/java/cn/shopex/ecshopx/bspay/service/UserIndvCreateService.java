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
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class UserIndvCreateService {

	private static final Pattern CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private final BsPayPaymentSettingService bsPayPaymentSettingService;
	private final BsPayOperatorResolveService bsPayOperatorResolveService;
	private final UserIndvMapper userIndvMapper;
	private final UserCardMapper userCardMapper;
	private final EntryApplyMapper entryApplyMapper;

	public UserIndvCreateService(
			BsPayPaymentSettingService bsPayPaymentSettingService,
			BsPayOperatorResolveService bsPayOperatorResolveService,
			UserIndvMapper userIndvMapper,
			UserCardMapper userCardMapper,
			EntryApplyMapper entryApplyMapper) {
		this.bsPayPaymentSettingService = bsPayPaymentSettingService;
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
		this.userIndvMapper = userIndvMapper;
		this.userCardMapper = userCardMapper;
		this.entryApplyMapper = entryApplyMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void create(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, Object> params = new LinkedHashMap<>(body);
		params.put("company_id", companyId);
		String certNo = str(params.get("cert_no"));
		String cardNo = str(params.get("card_no"));
		params.put("cert_no", certNo);
		params.put("card_no", cardNo);

		validateActionRules(params);
		checkIndvCertAndRegions(params);

		Map<String, Object> setting = bsPayPaymentSettingService.requireSettingMap(companyId);
		String sysId = Objects.toString(setting.get("sys_id"), "").trim();

		int now = (int) (System.currentTimeMillis() / 1000);

		log.info("bspay_createUser_data====>{}", params);

		UserIndv userIndv = new UserIndv();
		userIndv.setSysId(sysId);
		userIndv.setCompanyId(companyId);
		userIndv.setName(str(params.get("name")));
		userIndv.setCertNo(certNo);
		userIndv.setCertValidityType(parseCertValidityTypeForPersist(params.get("cert_validity_type")));
		userIndv.setCertBeginDate(str(params.get("cert_begin_date")));
		userIndv.setCertEndDate(str(params.get("cert_end_date")));
		userIndv.setMobileNo(str(params.get("mobile_no")));
		userIndv.setCreated(now);
		userIndv.setUpdated(now);

		int rows = userIndvMapper.insert(userIndv);
		if (rows <= 0 || userIndv.getId() == null) {
			throw new ResourceException("个人用户信息保存失败");
		}

		log.info("bspay_createUser_res====>{}", userIndv.getId());

		Map<String, Object> cardInfoMap = new LinkedHashMap<>();
		cardInfoMap.put("sys_id", sysId);
		cardInfoMap.put("user_id", userIndv.getId());
		cardInfoMap.put("company_id", companyId);
		cardInfoMap.put("user_type", "indv");
		cardInfoMap.put("card_type", "1");
		cardInfoMap.put("card_name", str(params.get("name")));
		cardInfoMap.put("card_no", cardNo);
		cardInfoMap.put("prov_id", str(params.get("prov_id")));
		cardInfoMap.put("area_id", str(params.get("area_id")));
		cardInfoMap.put("bank_code", str(params.get("bank_code")));
		cardInfoMap.put("branch_name", str(params.get("branch_name")));
		cardInfoMap.put("cert_no", certNo);
		cardInfoMap.put("cert_validity_type", params.get("cert_validity_type"));
		cardInfoMap.put("cert_begin_date", str(params.get("cert_begin_date")));
		cardInfoMap.put("cert_end_date", str(params.get("cert_end_date")));
		cardInfoMap.put("mp", str(params.get("mp")));

		log.info("bspay_createUser_cardInfo====>{}", cardInfoMap);

		UserCard userCard = new UserCard();
		userCard.setSysId(sysId);
		userCard.setUserId(userIndv.getId());
		userCard.setCompanyId(companyId);
		userCard.setUserType("indv");
		userCard.setCardType("1");
		userCard.setCardName(str(params.get("name")));
		userCard.setCardNo(cardNo);
		userCard.setProvId(str(params.get("prov_id")));
		userCard.setAreaId(str(params.get("area_id")));
		userCard.setBankCode(str(params.get("bank_code")));
		userCard.setBranchName(str(params.get("branch_name")));
		userCard.setCertNo(certNo);
		userCard.setCertValidityType(parseCertValidityTypeForPersist(params.get("cert_validity_type")));
		userCard.setCertBeginDate(str(params.get("cert_begin_date")));
		userCard.setCertEndDate(str(params.get("cert_end_date")));
		userCard.setMp(str(params.get("mp")));
		userCard.setCreated(now);
		userCard.setUpdated(now);

		int cardInsertRows = userCardMapper.insert(userCard);
		if (cardInsertRows <= 0) {
			throw new ResourceException("结算卡创建失败");
		}
		log.info("bspay_createUser_cardRes====>{}", cardInsertRows);

		BsPayOperatorResolveService.OperatorContext opCtx = bsPayOperatorResolveService.resolve(jwtMap);

		Map<String, Object> applyMap = new LinkedHashMap<>();
		applyMap.put("user_type", "indv");
		applyMap.put("user_name", str(params.get("name")));
		applyMap.put("company_id", companyId);
		applyMap.put("user_id", String.valueOf(userIndv.getId()));
		applyMap.put("operator_id", opCtx.operatorId());
		applyMap.put("operator_type", opCtx.operatorType());
		applyMap.put("address", "");
		applyMap.put("status", "WAIT_APPROVE");

		log.info("bspay_entryApply_params====>{}", applyMap);

		EntryApply entryApply = new EntryApply();
		entryApply.setUserType("indv");
		entryApply.setUserName(str(params.get("name")));
		entryApply.setCompanyId(companyId);
		entryApply.setUserId(String.valueOf(userIndv.getId()));
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
		log.info("bspay_entryApply_res====>{}", applyInsertRows);
	}

	private void validateActionRules(Map<String, Object> params) {
		if (!StringUtils.hasText(str(params.get("name")))) {
			throw new BadRequestException("个人姓名必填");
		}
		if (cardTypeEqualsOne(params.get("card_type")) && !StringUtils.hasText(str(params.get("cert_no")))) {
			throw new BadRequestException("个人身份证号必填");
		}
		int cvt = parseCertValidityTypeForRule(params.get("cert_validity_type"));
		if (!StringUtils.hasText(str(params.get("cert_begin_date")))) {
			throw new BadRequestException("个人身份证有效期起始日期必填");
		}
		if (cvt == 0 && !StringUtils.hasText(str(params.get("cert_end_date")))) {
			throw new BadRequestException("个人身份证有效期截止日期必填");
		}
		if (str(params.get("mobile_no")).length() != 11) {
			throw new BadRequestException("手机号格式错误");
		}
		if (!StringUtils.hasText(str(params.get("card_no")))) {
			throw new BadRequestException("银行卡号必填");
		}
		if (params.get("card_regions_id") == null) {
			throw new BadRequestException("银行卡开户地区必填");
		}
		if (str(params.get("mp")).length() != 11) {
			throw new BadRequestException("银行预留手机号格式错误");
		}
	}

	private static boolean cardTypeEqualsOne(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		if (raw instanceof String s) {
			return "1".equals(s.trim());
		}
		return false;
	}

	private static int parseCertValidityTypeForRule(Object raw) {
		if (raw == null) {
			throw new BadRequestException("个人身份证有效期类型必填");
		}
		if (raw instanceof Number n) {
			int v = intFromNumber(n, "个人身份证有效期类型格式错误");
			if (v != 0 && v != 1) {
				throw new BadRequestException("个人身份证有效期类型格式错误");
			}
			return v;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("个人身份证有效期类型必填");
			}
			int v = intFromString(t, "个人身份证有效期类型格式错误");
			if (v != 0 && v != 1) {
				throw new BadRequestException("个人身份证有效期类型格式错误");
			}
			return v;
		}
		throw new BadRequestException("个人身份证有效期类型格式错误");
	}

	public void validateIndvModifyPrerequisites(Map<String, Object> params) {
		requirePositiveLongId(params);
		validateActionRules(params);
	}

	private void requirePositiveLongId(Map<String, Object> params) {
		Object idRaw = params.get("id");
		if (idRaw == null) {
			throw new BadRequestException("Id必填");
		}
		if (idRaw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				throw new BadRequestException("Id必填");
			}
			params.put("id", v);
			return;
		}
		if (idRaw instanceof String s) {
			String t = s == null ? "" : s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("Id必填");
			}
			long v;
			try {
				v = Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("Id必填");
			}
			if (v <= 0) {
				throw new BadRequestException("Id必填");
			}
			params.put("id", v);
			return;
		}
		throw new BadRequestException("Id必填");
	}

	static String trimBspayParamString(Object o) {
		return str(o);
	}

	public void checkIndvCertAndRegions(Map<String, Object> params) {
		if (StringUtils.hasText(str(params.get("cert_no")))) {
			if (!CERT_PATTERN.matcher(str(params.get("cert_no"))).matches()) {
				throw new BadRequestException("身份证号码格式错误");
			}
		}
		Object regionsRaw = params.get("card_regions_id");
		if (!(regionsRaw instanceof Collection<?>) && !(regionsRaw instanceof Object[])) {
			throw new BadRequestException("开户行所在省市格式错误");
		}
		String first;
		String second;
		if (regionsRaw instanceof Collection<?> col) {
			if (col.size() != 2) {
				throw new BadRequestException("开户行所在省市格式错误");
			}
			Iterator<?> it = col.iterator();
			first = str(it.next());
			second = str(it.next());
		} else {
			Object[] arr = (Object[]) regionsRaw;
			if (arr.length != 2) {
				throw new BadRequestException("开户行所在省市格式错误");
			}
			first = str(arr[0]);
			second = str(arr[1]);
		}
		if (!StringUtils.hasText(first) || !StringUtils.hasText(second)) {
			throw new BadRequestException("开户行所在省市格式错误");
		}
		params.put("prov_id", first);
		params.put("area_id", second);
		params.remove("card_regions_id");
	}

	public static int parseCertValidityTypeForPersist(Object raw) {
		if (raw == null) {
			throw new BadRequestException("个人身份证有效期类型必填");
		}
		if (raw instanceof Number n) {
			return intFromNumber(n, "个人身份证有效期类型格式错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("个人身份证有效期类型必填");
			}
			return intFromString(t, "个人身份证有效期类型格式错误");
		}
		throw new BadRequestException("个人身份证有效期类型格式错误");
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
