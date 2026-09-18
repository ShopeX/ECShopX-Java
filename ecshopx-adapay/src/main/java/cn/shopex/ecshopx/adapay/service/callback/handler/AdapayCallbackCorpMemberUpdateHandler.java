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

package cn.shopex.ecshopx.adapay.service.callback.handler;

import cn.shopex.ecshopx.adapay.domain.AdapayBankCodes;
import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMemberUpdateLog;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayBankCodesMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberUpdateLogMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.adapay.service.callback.AdapayCallbackSmsNotifier;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantSettleAccountGateway;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdapayCallbackCorpMemberUpdateHandler {

	private static final String AUDIT_UPDATE_SUCCESS = "S";
	private static final String AUDIT_UPDATE_FAIL = "F";
	private static final String AUDIT_ACCOUNT_FAIL = "D";
	private static final String AUDIT_FAIL = "B";
	private static final String AUDIT_SUCCESS = "E";

	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapayCorpMemberMapper adapayCorpMemberMapper;
	private final AdapayMemberUpdateLogMapper adapayMemberUpdateLogMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayBankCodesMapper adapayBankCodesMapper;
	private final AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway;
	private final AdapayCallbackSmsNotifier adapayCallbackSmsNotifier;
	private final ObjectMapper objectMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdapayCallbackCorpMemberUpdateHandler(
			AdapayMemberMapper adapayMemberMapper,
			AdapayCorpMemberMapper adapayCorpMemberMapper,
			AdapayMemberUpdateLogMapper adapayMemberUpdateLogMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayBankCodesMapper adapayBankCodesMapper,
			AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway,
			AdapayCallbackSmsNotifier adapayCallbackSmsNotifier,
			ObjectMapper objectMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
		this.adapayMemberUpdateLogMapper = adapayMemberUpdateLogMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayBankCodesMapper = adapayBankCodesMapper;
		this.adapaySubMerchantSettleAccountGateway = adapaySubMerchantSettleAccountGateway;
		this.adapayCallbackSmsNotifier = adapayCallbackSmsNotifier;
		this.objectMapper = objectMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public void applySaveSplitLedgerCorpSucceeded(Map<String, Object> callbackDataFromSaveCorpApi) {
		LinkedHashMap<String, Object> normalized = new LinkedHashMap<>(callbackDataFromSaveCorpApi);
		normalized.put("audit_state", "S");
		Object desc = normalized.get("audit_desc");
		normalized.put("audit_desc", desc != null ? String.valueOf(desc) : "");
		succeeded(normalized);
	}

	public List<Object> succeeded(Map<String, Object> data) {
		String auditRaw = text(data.get("audit_state"));
		String mappedAudit = mapIncomingAudit(auditRaw);
		String appId = text(data.get("app_id"));
		String memberIdKey = text(data.get("member_id"));
		AdapayMemberUpdateLog logRow =
				adapayMemberUpdateLogMapper.selectOne(
						new LambdaQueryWrapper<AdapayMemberUpdateLog>()
								.eq(AdapayMemberUpdateLog::getMemberId, memberIdKey)
								.eq(AdapayMemberUpdateLog::getAppId, appId)
								.orderByDesc(AdapayMemberUpdateLog::getId)
								.last("LIMIT 1"));
		if (logRow == null) {
			throw new ResourceException("企业用户修改记录不存在");
		}
		Map<String, Object> logData = parseLogJson(sensitiveFieldEncryptor.decrypt(logRow.getData()));
		adapayMemberUpdateLogMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMemberUpdateLog>()
						.eq(AdapayMemberUpdateLog::getId, logRow.getId())
						.set(AdapayMemberUpdateLog::getAuditState, mappedAudit)
						.set(AdapayMemberUpdateLog::getAuditDesc, text(data.get("audit_desc"))));
		if (AUDIT_ACCOUNT_FAIL.equals(mappedAudit)) {
			SyncPair pair = syncBaseData(logRow, logData);
			AdapaySettleAccount settle =
					adapaySettleAccountMapper.selectOne(
							new LambdaQueryWrapper<AdapaySettleAccount>()
									.eq(AdapaySettleAccount::getAppId, appId)
									.eq(AdapaySettleAccount::getMemberId, parseLong(memberIdKey))
									.last("LIMIT 1"));
			if (settle != null && StringUtils.hasText(settle.getSettleAccountId())) {
				Map<String, Object> deleteResult =
						adapaySubMerchantSettleAccountGateway.deleteSettleAccount(
								settle.getCompanyId(), appId, settle.getSettleAccountId(), parseLong(memberIdKey));
				if (deleteFailedBlocking(deleteResult)) {
					String errMsg = nestedErrorMsg(deleteResult);
					adapayMemberUpdateLogMapper.update(
							null,
							new LambdaUpdateWrapper<AdapayMemberUpdateLog>()
									.eq(AdapayMemberUpdateLog::getId, logRow.getId())
									.set(AdapayMemberUpdateLog::getAuditDesc, errMsg));
					patchMemberAndCorpAuditDesc(appId, memberIdKey, errMsg);
					sendSmsIfNeeded(pair);
					return List.of("success");
				}
			}
			String bankName = resolveBankName(str(logData.get("bank_code")));
			adapaySettleAccountMapper.update(
					null,
					new LambdaUpdateWrapper<AdapaySettleAccount>()
							.eq(AdapaySettleAccount::getAppId, appId)
							.eq(AdapaySettleAccount::getMemberId, parseLong(memberIdKey))
							.set(AdapaySettleAccount::getBankAcctType, strOrDefault(logData.get("bank_acct_type"), "2"))
							.set(AdapaySettleAccount::getCardId, str(logData.get("card_no")))
							.set(AdapaySettleAccount::getCardName, str(logData.get("card_name")))
							.set(AdapaySettleAccount::getCertId, str(logData.get("legal_cert_id")))
							.set(AdapaySettleAccount::getCertType, strOrDefault(logData.get("cert_type"), "00"))
							.set(AdapaySettleAccount::getTelNo, str(logData.get("legal_mp")))
							.set(AdapaySettleAccount::getBankCode, str(logData.get("bank_code")))
							.set(AdapaySettleAccount::getBankName, bankName)
							.set(AdapaySettleAccount::getProvCode, str(logData.get("prov_code")))
							.set(AdapaySettleAccount::getAreaCode, str(logData.get("area_code"))));
			long memberPk = parseLong(memberIdKey);
			AdapayMember memberInfo =
					adapayMemberMapper.selectOne(
							new LambdaQueryWrapper<AdapayMember>()
									.eq(AdapayMember::getId, memberPk)
									.eq(AdapayMember::getAppId, appId)
									.last("LIMIT 1"));
			if (memberInfo == null || memberInfo.getCompanyId() == null) {
				throw new ResourceException("开户会员信息不存在");
			}
			AdapaySettleAccount settleRow =
					adapaySettleAccountMapper.selectOne(
							new LambdaQueryWrapper<AdapaySettleAccount>()
									.eq(AdapaySettleAccount::getAppId, appId)
									.eq(AdapaySettleAccount::getMemberId, memberPk)
									.last("LIMIT 1"));
			Map<String, Object> accountInfo =
					AdapaySubMerchantSettleAccountGateway.toAccountInfoMap(settleRow);
			Map<String, Object> createResult =
					adapaySubMerchantSettleAccountGateway.createSettleAccount(
							memberInfo.getCompanyId(), appId, memberPk, accountInfo);
			if (createSucceeded(createResult)) {
				adapayMemberUpdateLogMapper.update(
						null,
						new LambdaUpdateWrapper<AdapayMemberUpdateLog>()
								.eq(AdapayMemberUpdateLog::getId, logRow.getId())
								.set(AdapayMemberUpdateLog::getAuditState, AUDIT_SUCCESS));
				patchMemberAndCorpAuditState(appId, memberIdKey, AUDIT_SUCCESS);
				String newSettleId = nestedId(createResult);
				if (StringUtils.hasText(newSettleId)) {
					adapaySettleAccountMapper.update(
							null,
							new LambdaUpdateWrapper<AdapaySettleAccount>()
									.eq(AdapaySettleAccount::getAppId, appId)
									.eq(AdapaySettleAccount::getMemberId, memberPk)
									.set(AdapaySettleAccount::getSettleAccountId, newSettleId));
				}
			} else {
				String err = nestedErrorMsg(createResult);
				String desc = "###" + err + "(请尽快提交修改信息，避免影响分账功能)";
				adapayMemberUpdateLogMapper.update(
						null,
						new LambdaUpdateWrapper<AdapayMemberUpdateLog>()
								.eq(AdapayMemberUpdateLog::getId, logRow.getId())
								.set(AdapayMemberUpdateLog::getAuditState, AUDIT_ACCOUNT_FAIL)
								.set(AdapayMemberUpdateLog::getAuditDesc, desc));
				patchMemberAndCorpAuditDesc(appId, memberIdKey, err);
			}
		}
		AdapayMember memberRow =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getId, parseLong(memberIdKey))
								.eq(AdapayMember::getAppId, appId)
								.last("LIMIT 1"));
		AdapayCorpMember corpRow =
				adapayCorpMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayCorpMember>()
								.eq(AdapayCorpMember::getMemberId, parseLong(memberIdKey))
								.eq(AdapayCorpMember::getAppId, appId)
								.last("LIMIT 1"));
		if (memberRow != null && isTruthySms(memberRow.getIsSms()) && corpRow != null) {
			Map<String, Object> sms = new HashMap<>();
			sms.put("mer_name", corpRow.getName());
			sms.put("tel_no", memberRow.getTelNo());
			sms.put("company_id", memberRow.getCompanyId());
			adapayCallbackSmsNotifier.trySend(sms);
		}
		return List.of("success");
	}

	public List<Object> failed(Map<String, Object> data) {
		return List.of("success");
	}

	private void patchMemberAndCorpAuditDesc(String appId, String memberIdKey, String errMsg) {
		long pk = parseLong(memberIdKey);
		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getId, pk)
						.eq(AdapayMember::getAppId, appId)
						.set(AdapayMember::getAuditDesc, errMsg));
		adapayCorpMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayCorpMember>()
						.eq(AdapayCorpMember::getMemberId, pk)
						.eq(AdapayCorpMember::getAppId, appId)
						.set(AdapayCorpMember::getAuditDesc, errMsg));
	}

	private void patchMemberAndCorpAuditState(String appId, String memberIdKey, String state) {
		long pk = parseLong(memberIdKey);
		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getId, pk)
						.eq(AdapayMember::getAppId, appId)
						.set(AdapayMember::getAuditState, state));
		adapayCorpMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayCorpMember>()
						.eq(AdapayCorpMember::getMemberId, pk)
						.eq(AdapayCorpMember::getAppId, appId)
						.set(AdapayCorpMember::getAuditState, state));
	}

	private SyncPair syncBaseData(AdapayMemberUpdateLog logRow, Map<String, Object> logData) {
		long memberPk = parseLong(logRow.getMemberId());
		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getId, memberPk)
						.eq(AdapayMember::getAppId, logRow.getAppId())
						.set(AdapayMember::getEmail, str(logData.get("email")))
						.set(AdapayMember::getTelNo, str(logData.get("legal_mp")))
						.set(AdapayMember::getUserName, str(logData.get("name")))
						.set(AdapayMember::getCertId, str(logData.get("social_credit_code"))));
		adapayCorpMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayCorpMember>()
						.eq(AdapayCorpMember::getMemberId, memberPk)
						.eq(AdapayCorpMember::getAppId, logRow.getAppId())
						.set(AdapayCorpMember::getName, str(logData.get("name")))
						.set(AdapayCorpMember::getProvCode, str(logData.get("prov_code")))
						.set(AdapayCorpMember::getAreaCode, str(logData.get("area_code")))
						.set(AdapayCorpMember::getSocialCreditCode, str(logData.get("social_credit_code")))
						.set(AdapayCorpMember::getLegalPerson, str(logData.get("legal_person")))
						.set(AdapayCorpMember::getLegalCertId, str(logData.get("legal_cert_id")))
						.set(AdapayCorpMember::getLegalMp, str(logData.get("legal_mp")))
						.set(AdapayCorpMember::getAddress, str(logData.get("address")))
						.set(AdapayCorpMember::getZipCode, str(logData.get("zip_code")))
						.set(AdapayCorpMember::getTelphone, str(logData.get("telphone")))
						.set(AdapayCorpMember::getEmail, str(logData.get("email")))
						.set(AdapayCorpMember::getBankCode, str(logData.get("bank_code")))
						.set(AdapayCorpMember::getBankAcctType, str(logData.get("bank_acct_type")))
						.set(AdapayCorpMember::getCardNo, str(logData.get("card_no")))
						.set(AdapayCorpMember::getCardName, str(logData.get("card_name"))));
		AdapayMember memberInfo =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getId, memberPk)
								.eq(AdapayMember::getAppId, logRow.getAppId())
								.last("LIMIT 1"));
		AdapayCorpMember corpInfo =
				adapayCorpMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayCorpMember>()
								.eq(AdapayCorpMember::getMemberId, memberPk)
								.eq(AdapayCorpMember::getAppId, logRow.getAppId())
								.last("LIMIT 1"));
		return new SyncPair(memberInfo, corpInfo);
	}

	private void sendSmsIfNeeded(SyncPair pair) {
		if (pair.member() != null
				&& isTruthySms(pair.member().getIsSms())
				&& pair.corp() != null) {
			Map<String, Object> sms = new HashMap<>();
			sms.put("mer_name", pair.corp().getName());
			sms.put("tel_no", pair.member().getTelNo());
			sms.put("company_id", pair.member().getCompanyId());
			adapayCallbackSmsNotifier.trySend(sms);
		}
	}

	private Map<String, Object> parseLogJson(String json) {
		if (!StringUtils.hasText(json)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private String resolveBankName(String bankCode) {
		if (!StringUtils.hasText(bankCode)) {
			return "";
		}
		AdapayBankCodes row =
				adapayBankCodesMapper.selectOne(
						new LambdaQueryWrapper<AdapayBankCodes>()
								.eq(AdapayBankCodes::getBankCode, bankCode)
								.last("LIMIT 1"));
		return row == null || row.getBankName() == null ? "" : row.getBankName();
	}

	private static String mapIncomingAudit(String raw) {
		if (AUDIT_UPDATE_SUCCESS.equals(raw)) {
			return AUDIT_ACCOUNT_FAIL;
		}
		if (AUDIT_UPDATE_FAIL.equals(raw)) {
			return AUDIT_FAIL;
		}
		return raw;
	}

	private static boolean deleteFailedBlocking(Map<String, Object> resp) {
		Object data = resp.get("data");
		if (!(data instanceof Map<?, ?> m)) {
			return false;
		}
		Object status = m.get("status");
		if (!"failed".equals(String.valueOf(status))) {
			return false;
		}
		Object code = m.get("error_code");
		return code == null || !"account_not_exists".equals(String.valueOf(code));
	}

	private static boolean createSucceeded(Map<String, Object> resp) {
		Object data = resp.get("data");
		if (!(data instanceof Map<?, ?> m)) {
			return false;
		}
		return "succeeded".equals(String.valueOf(m.get("status")));
	}

	private static String nestedErrorMsg(Map<String, Object> resp) {
		Object data = resp.get("data");
		if (data instanceof Map<?, ?> m && m.get("error_msg") != null) {
			return String.valueOf(m.get("error_msg"));
		}
		return "未知错误";
	}

	private static String nestedId(Map<String, Object> resp) {
		Object data = resp.get("data");
		if (data instanceof Map<?, ?> m && m.get("id") != null) {
			return String.valueOf(m.get("id"));
		}
		return "";
	}

	private static boolean isTruthySms(String v) {
		return "1".equals(v) || "true".equalsIgnoreCase(v);
	}

	private static long parseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String text(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String strOrDefault(Object o, String def) {
		String s = str(o);
		return StringUtils.hasText(s) ? s : def;
	}

	private record SyncPair(AdapayMember member, AdapayCorpMember corp) {}
}
