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

import cn.shopex.ecshopx.adapay.domain.AdapayEntryApply;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayEntryApplyMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdapayPersonMemberCreateService {

	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private final AdapayPersonMemberCreateService self;
	private final AdapayAdaPayPaymentSettingReadService adapayPaymentSettingReadService;
	private final MemberOperatorContextService memberOperatorContextService;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayEntryApplyMapper adapayEntryApplyMapper;
	private final AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService;

	public AdapayPersonMemberCreateService(
			@Lazy AdapayPersonMemberCreateService self,
			AdapayAdaPayPaymentSettingReadService adapayPaymentSettingReadService,
			MemberOperatorContextService memberOperatorContextService,
			AdapayMemberMapper adapayMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayEntryApplyMapper adapayEntryApplyMapper,
			AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService) {
		this.self = self;
		this.adapayPaymentSettingReadService = adapayPaymentSettingReadService;
		this.memberOperatorContextService = memberOperatorContextService;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayEntryApplyMapper = adapayEntryApplyMapper;
		this.adapayCreateMemberOperationLogService = adapayCreateMemberOperationLogService;
	}

	public boolean createFromRequest(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, String> normalized = normalizeBody(body == null ? Map.of() : body);
		validateActionRules(normalized);
		checkParams(normalized);
		self.createTransactional(companyId, jwtMap, normalized);
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean createTransactional(long companyId, Map<String, Object> jwtMap, Map<String, String> fields) {
		String appId = adapayPaymentSettingReadService.loadAppId(companyId);
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("adapay 支付信息未配置", 400);
		}

		long jwtOperatorId = toLong(jwtMap.get("operator_id"));
		String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
		Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
		Map<String, Object> operatorContext =
				memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);

		int now = (int) (System.currentTimeMillis() / 1000L);

		String effectiveCertType = StringUtils.hasText(fields.get("cert_type")) ? fields.get("cert_type") : "00";

		AdapayMember member = new AdapayMember();
		member.setAppId(appId);
		member.setOperatorId(toInt(operatorContext.get("operator_id")));
		member.setOperatorType(stringVal(operatorContext.get("operator_type")));
		member.setTelNo(fields.get("tel_no"));
		member.setUserName(fields.get("user_name"));
		member.setCertId(fields.get("cert_id"));
		member.setCertType(effectiveCertType);
		member.setCompanyId(companyId);
		member.setMemberType("person");
		member.setAuditState("0");
		member.setCreateTime(now);
		member.setUpdateTime(now);

		int ins = adapayMemberMapper.insert(member);
		if (ins <= 0 || member.getId() == null || member.getId() <= 0) {
			throw new BadRequestException("用户创建失败");
		}
		long memberId = member.getId();

		AdapaySettleAccount settle = new AdapaySettleAccount();
		settle.setBankAcctType("2");
		settle.setCardId(fields.get("bank_card_id"));
		settle.setCardName(fields.get("bank_card_name"));
		settle.setCertId(fields.get("bank_cert_id"));
		settle.setCertType(effectiveCertType);
		settle.setTelNo(fields.get("bank_tel_no"));
		settle.setCompanyId(companyId);
		settle.setMemberId(memberId);
		settle.setAppId(appId);
		settle.setChannel("bank_account");
		settle.setCreateTime(now);
		settle.setUpdateTime(now);

		int saIns = adapaySettleAccountMapper.insert(settle);
		if (saIns <= 0) {
			throw new BadRequestException("结算账户创建失败");
		}

		AdapayEntryApply apply = new AdapayEntryApply();
		apply.setUserName(fields.get("user_name"));
		apply.setCompanyId(String.valueOf(companyId));
		apply.setEntryId(String.valueOf(memberId));
		apply.setApplyType(stringVal(operatorContext.get("operator_type")));
		apply.setStatus("WAIT_APPROVE");
		apply.setAddress(null);
		apply.setCreateTime(now);
		apply.setUpdateTime(now);

		int apIns = adapayEntryApplyMapper.insert(apply);
		if (apIns <= 0) {
			throw new BadRequestException("开户申请创建失败");
		}

		adapayCreateMemberOperationLogService.recordCreateMemberLog(
				companyId, jwtMap, fields.get("user_name"), operatorContext);
		return true;
	}

	private static Map<String, String> normalizeBody(Map<String, Object> body) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("tel_no", trimToEmpty(stringVal(body.get("tel_no"))));
		m.put("user_name", trimToEmpty(stringVal(body.get("user_name"))));
		m.put("cert_id", trimToEmpty(stringVal(body.get("cert_id"))));
		m.put("cert_type", trimToEmpty(stringVal(body.get("cert_type"))));
		m.put("bank_card_name", trimToEmpty(stringVal(body.get("bank_card_name"))));
		m.put("bank_tel_no", trimToEmpty(stringVal(body.get("bank_tel_no"))));
		m.put("bank_card_id", trimToEmpty(stringVal(body.get("bank_card_id"))));
		m.put("bank_cert_id", trimToEmpty(stringVal(body.get("bank_cert_id"))));
		m.put("bank_cert_type", trimToEmpty(stringVal(body.get("bank_cert_type"))));
		return m;
	}

	private static void validateActionRules(Map<String, String> f) {
		if (!isValidMobile11(f.get("tel_no"))) {
			throw new BadRequestException("手机号码必须是11位");
		}
		if (!StringUtils.hasText(f.get("user_name")) || f.get("user_name").codePointCount(0, f.get("user_name").length()) > 15) {
			throw new BadRequestException("姓名必填且不能超过15个字符");
		}
		if (!StringUtils.hasText(f.get("cert_id")) || f.get("cert_id").length() != 18) {
			throw new BadRequestException("身份证号必须是18位");
		}
		if (!StringUtils.hasText(f.get("bank_card_name"))
				|| f.get("bank_card_name").codePointCount(0, f.get("bank_card_name").length()) > 15) {
			throw new BadRequestException("开户人姓名必填且不能超过15个字符");
		}
		if (!isValidMobile11(f.get("bank_tel_no"))) {
			throw new BadRequestException("银行预留手机号必须是11位");
		}
		if (!StringUtils.hasText(f.get("bank_card_id"))) {
			throw new BadRequestException("银行账号必填");
		}
		if (!StringUtils.hasText(f.get("bank_cert_id")) || f.get("bank_cert_id").length() != 18) {
			throw new BadRequestException("开户人身份证号必须是18位");
		}
	}

	private static boolean isValidMobile11(String s) {
		return StringUtils.hasText(s) && s.length() == 11 && s.chars().allMatch(Character::isDigit);
	}

	private void checkParams(Map<String, String> f) {
		String certId = f.get("cert_id");
		if (StringUtils.hasText(certId) && !LEGAL_CERT_PATTERN.matcher(certId).matches()) {
			throw new BadRequestException("身份证号码格式错误");
		}
		String bankCertId = f.get("bank_cert_id");
		if (StringUtils.hasText(bankCertId) && !LEGAL_CERT_PATTERN.matcher(bankCertId).matches()) {
			throw new BadRequestException("开户人身份证号码格式错误");
		}
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
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
