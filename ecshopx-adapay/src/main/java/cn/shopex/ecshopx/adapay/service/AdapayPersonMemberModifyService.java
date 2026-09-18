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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdapayPersonMemberModifyService {

	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private final AdapayPersonMemberModifyService self;
	private final MemberOperatorContextService memberOperatorContextService;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayEntryApplyMapper adapayEntryApplyMapper;
	private final AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorMapper distributorMapper;

	public AdapayPersonMemberModifyService(
			@Lazy AdapayPersonMemberModifyService self,
			MemberOperatorContextService memberOperatorContextService,
			AdapayMemberMapper adapayMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayEntryApplyMapper adapayEntryApplyMapper,
			AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService,
			OperatorsQueryService operatorsQueryService,
			DistributorMapper distributorMapper) {
		this.self = self;
		this.memberOperatorContextService = memberOperatorContextService;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayEntryApplyMapper = adapayEntryApplyMapper;
		this.adapayCreateMemberOperationLogService = adapayCreateMemberOperationLogService;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorMapper = distributorMapper;
	}

	public void modify(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, String> normalized = normalizeBody(body == null ? Map.of() : body);
		validateActionRules(normalized);
		checkParams(normalized);
		self.modifyTransactional(companyId, jwtMap, normalized);
	}

	@Transactional(rollbackFor = Exception.class)
	public void modifyTransactional(long companyId, Map<String, Object> jwtMap, Map<String, String> fields) {
		long memberId;
		try {
			memberId = Long.parseLong(fields.get("member_id"));
		} catch (NumberFormatException e) {
			throw new BadRequestException("id必填");
		}
		if (memberId <= 0L) {
			throw new BadRequestException("id必填");
		}

		long jwtOperatorId = toLong(jwtMap.get("operator_id"));
		String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
		Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
		Map<String, Object> operatorContext =
				memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);

		AdapayMember member =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getId, memberId)
								.eq(AdapayMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (member == null) {
			throw new ResourceException("开户信息不存在");
		}

		String audit = member.getAuditState();
		boolean isUpdateAction = "D".equals(audit) || "E".equals(audit);

		int now = (int) (System.currentTimeMillis() / 1000L);

		LambdaUpdateWrapper<AdapayMember> uw =
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getId, member.getId())
						.eq(AdapayMember::getCompanyId, companyId)
						.set(AdapayMember::getTelNo, fields.get("tel_no"))
						.set(AdapayMember::getAuditState, "0")
						.set(AdapayMember::getAuditDesc, "")
						.set(AdapayMember::getUpdateTime, now);
		if (!isUpdateAction) {
			uw.set(AdapayMember::getUserName, fields.get("user_name")).set(AdapayMember::getCertId, fields.get("cert_id"));
		}
		int rows = adapayMemberMapper.update(null, uw);
		if (rows < 1) {
			throw new BadRequestException("用户更新失败");
		}

		LambdaUpdateWrapper<AdapaySettleAccount> suw =
				new LambdaUpdateWrapper<AdapaySettleAccount>()
						.eq(AdapaySettleAccount::getMemberId, member.getId())
						.eq(AdapaySettleAccount::getCompanyId, companyId)
						.set(AdapaySettleAccount::getCardId, fields.get("bank_card_id"))
						.set(AdapaySettleAccount::getTelNo, fields.get("bank_tel_no"))
						.set(AdapaySettleAccount::getUpdateTime, now);
		if (!isUpdateAction) {
			suw.set(AdapaySettleAccount::getCardName, fields.get("bank_card_name"))
					.set(AdapaySettleAccount::getCertId, fields.get("bank_cert_id"));
		}
		int sRows = adapaySettleAccountMapper.update(null, suw);
		if (sRows < 1) {
			throw new BadRequestException("结算账户更新失败");
		}

		AdapayEntryApply apply = new AdapayEntryApply();
		apply.setUserName(fields.get("user_name"));
		apply.setCompanyId(String.valueOf(companyId));
		apply.setEntryId(String.valueOf(member.getId()));
		apply.setApplyType(stringVal(operatorContext.get("operator_type")));
		apply.setStatus("WAIT_APPROVE");
		apply.setAddress(null);
		apply.setCreateTime(now);
		apply.setUpdateTime(now);

		int ins = adapayEntryApplyMapper.insert(apply);
		if (ins <= 0) {
			throw new BadRequestException("开户申请创建失败");
		}

		adapayCreateMemberOperationLogService.recordUpdateMemberLog(
				companyId, jwtMap, resolveUpdateLogDisplayName(companyId, jwtMap), operatorContext);
	}

	private String resolveUpdateLogDisplayName(long companyId, Map<String, Object> jwtMap) {
		String operatorType = stringVal(jwtMap.get("operator_type")).toLowerCase(Locale.ROOT);
		if ("distributor".equals(operatorType)) {
			long distributorId = toLong(jwtMap.get("distributor_id"));
			Distributor d =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, distributorId)
									.last("LIMIT 1"));
			if (d == null || !StringUtils.hasText(d.getName())) {
				throw new ResourceException("店铺信息不存在");
			}
			return d.getName().trim();
		}
		Map<String, Object> filter =
				Map.of("company_id", companyId, "operator_id", toLong(jwtMap.get("operator_id")));
		Map<String, Object> op = operatorsQueryService.getInfo(new HashMap<>(filter));
		if (op == null) {
			return "";
		}
		Object u = op.get("username");
		if (u != null && StringUtils.hasText(u.toString())) {
			return u.toString();
		}
		Object m = op.get("mobile");
		return m != null ? m.toString() : "";
	}

	private static Map<String, String> normalizeBody(Map<String, Object> body) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("member_id", trimToEmpty(stringVal(body.get("member_id"))));
		m.put("tel_no", trimToEmpty(stringVal(body.get("tel_no"))));
		m.put("user_name", trimToEmpty(stringVal(body.get("user_name"))));
		m.put("cert_id", trimToEmpty(stringVal(body.get("cert_id"))));
		m.put("bank_card_name", trimToEmpty(stringVal(body.get("bank_card_name"))));
		m.put("bank_tel_no", trimToEmpty(stringVal(body.get("bank_tel_no"))));
		m.put("bank_card_id", trimToEmpty(stringVal(body.get("bank_card_id"))));
		m.put("bank_cert_id", trimToEmpty(stringVal(body.get("bank_cert_id"))));
		return m;
	}

	private static void validateActionRules(Map<String, String> f) {
		if (!StringUtils.hasText(f.get("member_id"))) {
			throw new BadRequestException("id必填");
		}
		if (!isValidMobile11(f.get("tel_no"))) {
			throw new BadRequestException("手机号码必须是11位");
		}
		if (!StringUtils.hasText(f.get("user_name"))
				|| f.get("user_name").codePointCount(0, f.get("user_name").length()) > 15) {
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
}
