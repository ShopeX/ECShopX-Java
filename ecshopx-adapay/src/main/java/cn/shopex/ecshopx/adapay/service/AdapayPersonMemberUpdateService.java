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

import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMemberUpdateLog;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberUpdateLogMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantSettleAccountGateway;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayPersonMemberUpdateService {

	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private static final String AUDIT_SUCCESS = "E";
	private static final String AUDIT_ACCOUNT_FAIL = "D";

	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayMemberUpdateLogMapper adapayMemberUpdateLogMapper;
	private final AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;
	private final AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService;
	private final MemberOperatorContextService memberOperatorContextService;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorMapper distributorMapper;

	public AdapayPersonMemberUpdateService(
			AdapayMemberMapper adapayMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayMemberUpdateLogMapper adapayMemberUpdateLogMapper,
			AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper,
			AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService,
			MemberOperatorContextService memberOperatorContextService,
			OperatorsQueryService operatorsQueryService,
			DistributorMapper distributorMapper) {
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayMemberUpdateLogMapper = adapayMemberUpdateLogMapper;
		this.adapaySubMerchantSettleAccountGateway = adapaySubMerchantSettleAccountGateway;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
		this.adapayCreateMemberOperationLogService = adapayCreateMemberOperationLogService;
		this.memberOperatorContextService = memberOperatorContextService;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorMapper = distributorMapper;
	}

	public void update(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		String memberIdRaw = trimToEmpty(stringVal(body.get("member_id")));
		if (!StringUtils.hasText(memberIdRaw)) {
			throw new BadRequestException("id必填");
		}
		long memberId;
		try {
			memberId = Long.parseLong(memberIdRaw);
		} catch (NumberFormatException e) {
			throw new ResourceException("开户信息不存在");
		}
		if (memberId <= 0L) {
			throw new ResourceException("开户信息不存在");
		}

		String certId = trimToEmpty(stringVal(body.get("cert_id")));
		if (StringUtils.hasText(certId) && !LEGAL_CERT_PATTERN.matcher(certId).matches()) {
			throw new BadRequestException("身份证号码格式错误");
		}
		String bankCertId = trimToEmpty(stringVal(body.get("bank_cert_id")));
		if (StringUtils.hasText(bankCertId) && !LEGAL_CERT_PATTERN.matcher(bankCertId).matches()) {
			throw new BadRequestException("开户人身份证号码格式错误");
		}

		AdapayMember memberInfo =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getId, memberId)
								.eq(AdapayMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (memberInfo == null) {
			throw new ResourceException("开户信息不存在");
		}

		AdapaySettleAccount account =
				adapaySettleAccountMapper.selectOne(
						new LambdaQueryWrapper<AdapaySettleAccount>()
								.eq(AdapaySettleAccount::getMemberId, memberId)
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (account == null) {
			throw new BadRequestException("结算账户不存在", 400);
		}

		String settleId = account.getSettleAccountId();
		if (StringUtils.hasText(settleId)) {
			Map<String, Object> delRes =
					adapaySubMerchantSettleAccountGateway.deleteSettleAccount(
							companyId, memberInfo.getAppId(), settleId, memberInfo.getId());
			Object dataObj = delRes.get("data");
			if (dataObj instanceof Map<?, ?> rawDel) {
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) rawDel;
				String status = String.valueOf(m.get("status"));
				Object codeObj = m.get("error_code");
				if ("failed".equals(status)
						&& codeObj != null
						&& !"account_not_exists".equals(String.valueOf(codeObj))) {
					String errMsg = m.get("error_msg") != null ? String.valueOf(m.get("error_msg")) : "";
					throw new BadRequestException("结算账户更新失败: " + errMsg);
				}
			}
		}

		int nowEpochSec = (int) (System.currentTimeMillis() / 1000L);

		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getId, memberId)
						.eq(AdapayMember::getCompanyId, companyId)
						.set(AdapayMember::getIsUpdate, 1)
						.set(AdapayMember::getUpdateTime, nowEpochSec));

		String bankTelNoTrimmed = trimToEmpty(stringVal(body.get("bank_tel_no")));
		String bankCardIdTrimmed = trimToEmpty(stringVal(body.get("bank_card_id")));
		adapaySettleAccountMapper.update(
				null,
				new LambdaUpdateWrapper<AdapaySettleAccount>()
						.eq(AdapaySettleAccount::getMemberId, memberId)
						.eq(AdapaySettleAccount::getCompanyId, companyId)
						.set(AdapaySettleAccount::getCardId, bankCardIdTrimmed)
						.set(AdapaySettleAccount::getTelNo, bankTelNoTrimmed)
						.set(AdapaySettleAccount::getUpdateTime, nowEpochSec));

		AdapaySettleAccount settleAfter =
				adapaySettleAccountMapper.selectOne(
						new LambdaQueryWrapper<AdapaySettleAccount>()
								.eq(AdapaySettleAccount::getMemberId, memberId)
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.last("LIMIT 1"));
		Map<String, Object> accountInfo =
				AdapaySubMerchantSettleAccountGateway.toAccountInfoMap(settleAfter);
		Map<String, Object> createRes =
				adapaySubMerchantSettleAccountGateway.createSettleAccount(
						companyId, memberInfo.getAppId(), memberInfo.getId(), accountInfo);

		Object createDataObj = createRes.get("data");
		if (!(createDataObj instanceof Map<?, ?> rawCreateData)) {
			String errorMsg = "";
			insertMemberUpdateLog(
					companyId, memberInfo, memberId, body, AUDIT_ACCOUNT_FAIL,
					errorMsg + "(请尽快提交修改信息，避免影响分账功能)", nowEpochSec);
			throw new BadRequestException("结算账户更新失败: " + errorMsg);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) rawCreateData;
		String cStatus = String.valueOf(data.get("status"));

		if ("succeeded".equals(cStatus)) {
			adapayMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayMember>()
							.eq(AdapayMember::getId, memberId)
							.eq(AdapayMember::getCompanyId, companyId)
							.set(AdapayMember::getAuditState, AUDIT_SUCCESS)
							.set(AdapayMember::getAuditDesc, "")
							.set(AdapayMember::getUpdateTime, nowEpochSec));

			adapaySettleAccountMapper.update(
					null,
					new LambdaUpdateWrapper<AdapaySettleAccount>()
							.eq(AdapaySettleAccount::getMemberId, memberId)
							.eq(AdapaySettleAccount::getCompanyId, companyId)
							.set(AdapaySettleAccount::getSettleAccountId, String.valueOf(data.get("id")))
							.set(AdapaySettleAccount::getUpdateTime, nowEpochSec));

			insertMemberUpdateLog(
					companyId, memberInfo, memberId, body, AUDIT_SUCCESS, "", nowEpochSec);

			long jwtOperatorId = toLong(jwtMap.get("operator_id"));
			String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
			Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
			Map<String, Object> operatorContext =
					memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);
			String displayName = resolveUpdateLogDisplayName(companyId, jwtMap);
			adapayCreateMemberOperationLogService.recordUpdateMemberLog(
					companyId, jwtMap, displayName, operatorContext);
			return;
		}

		String errorMsg = data.get("error_msg") != null ? String.valueOf(data.get("error_msg")) : "";
		insertMemberUpdateLog(
				companyId, memberInfo, memberId, body, AUDIT_ACCOUNT_FAIL,
				errorMsg + "(请尽快提交修改信息，避免影响分账功能)", nowEpochSec);
		throw new BadRequestException("结算账户更新失败: " + errorMsg);
	}

	private void insertMemberUpdateLog(
			long companyId,
			AdapayMember memberInfo,
			long memberId,
			Map<String, Object> body,
			String auditState,
			String auditDesc,
			int nowEpochSec) {
		LinkedHashMap<String, Object> logPayload = buildLogPayload(companyId, body);
		String plainJson;
		try {
			plainJson = objectMapper.writeValueAsString(logPayload);
		} catch (JsonProcessingException e) {
			throw new ResourceException("日志数据序列化失败");
		}
		String stored = sensitiveFieldEncryptor.encrypt(plainJson);
		AdapayMemberUpdateLog row = new AdapayMemberUpdateLog();
		row.setCompanyId(String.valueOf(companyId));
		row.setAppId(memberInfo.getAppId());
		row.setMemberId(String.valueOf(memberId));
		row.setData(stored);
		row.setAuditState(auditState);
		row.setAuditDesc(auditDesc);
		row.setCreateTime(nowEpochSec);
		row.setUpdateTime(nowEpochSec);
		adapayMemberUpdateLogMapper.insert(row);
	}

	private static LinkedHashMap<String, Object> buildLogPayload(long companyId, Map<String, Object> body) {
		LinkedHashMap<String, Object> logPayload = new LinkedHashMap<>();
		logPayload.put("company_id", companyId);
		String[] fixedKeys = {
			"member_id",
			"tel_no",
			"user_name",
			"cert_id",
			"cert_type",
			"bank_card_id",
			"bank_card_name",
			"bank_cert_id",
			"bank_cert_type",
			"bank_tel_no",
			"submit_review"
		};
		for (String key : fixedKeys) {
			if (body.containsKey(key)) {
				logPayload.put(key, body.get(key));
			}
		}
		TreeSet<String> remaining = new TreeSet<>();
		for (String key : body.keySet()) {
			if (!logPayload.containsKey(key)) {
				remaining.add(key);
			}
		}
		for (String key : remaining) {
			logPayload.put(key, body.get(key));
		}
		return logPayload;
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
