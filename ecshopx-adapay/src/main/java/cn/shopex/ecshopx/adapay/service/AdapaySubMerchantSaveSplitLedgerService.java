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
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayEntryApplyMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.config.AdapayCallbackProperties;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantAdaPayOutboundGateway;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantSettleAccountGateway;
import cn.shopex.ecshopx.adapay.service.callback.handler.AdapayCallbackCorpMemberUpdateHandler;
import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapaySubMerchantSaveSplitLedgerService {

	private static final Logger log = LoggerFactory.getLogger(AdapaySubMerchantSaveSplitLedgerService.class);

	private static final String AUDIT_WAIT = "A";
	private static final String AUDIT_FAIL = "B";
	private static final String AUDIT_MEMBER_FAIL = "C";
	private static final String AUDIT_ACCOUNT_FAIL = "D";
	private static final String AUDIT_SUCCESS = "E";
	private static final String STATUS_APPROVED = "APPROVED";
	private static final String MEMBER_TYPE_PERSON = "person";
	private static final String MEMBER_TYPE_CORP = "corp";

	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapayCallbackProperties adapayCallbackProperties;
	private final AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway;
	private final AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway;
	private final AdapayCallbackCorpMemberUpdateHandler adapayCallbackCorpMemberUpdateHandler;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;
	private final AdapaySubMerchantLastIsSmsRedisWriter adapaySubMerchantLastIsSmsRedisWriter;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayCorpMemberMapper adapayCorpMemberMapper;
	private final AdapayEntryApplyMapper adapayEntryApplyMapper;
	private final OperatorsMapper operatorsMapper;
	private final DistributorMapper distributorMapper;
	private final ObjectMapper objectMapper;

	public AdapaySubMerchantSaveSplitLedgerService(
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayCallbackProperties adapayCallbackProperties,
			AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway,
			AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway,
			AdapayCallbackCorpMemberUpdateHandler adapayCallbackCorpMemberUpdateHandler,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort,
			AdapaySubMerchantLastIsSmsRedisWriter adapaySubMerchantLastIsSmsRedisWriter,
			AdapayMemberMapper adapayMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayCorpMemberMapper adapayCorpMemberMapper,
			AdapayEntryApplyMapper adapayEntryApplyMapper,
			OperatorsMapper operatorsMapper,
			DistributorMapper distributorMapper,
			ObjectMapper objectMapper) {
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapayCallbackProperties = adapayCallbackProperties;
		this.adapaySubMerchantAdaPayOutboundGateway = adapaySubMerchantAdaPayOutboundGateway;
		this.adapaySubMerchantSettleAccountGateway = adapaySubMerchantSettleAccountGateway;
		this.adapayCallbackCorpMemberUpdateHandler = adapayCallbackCorpMemberUpdateHandler;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
		this.adapaySubMerchantLastIsSmsRedisWriter = adapaySubMerchantLastIsSmsRedisWriter;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
		this.adapayEntryApplyMapper = adapayEntryApplyMapper;
		this.operatorsMapper = operatorsMapper;
		this.distributorMapper = distributorMapper;
		this.objectMapper = objectMapper;
	}

	public void saveSplitLedger(long companyId, long jwtOperatorId, Map<String, Object> body) {
		validateRequiredKeys(body);
		String rawSplitLedgerJson = rawSplitLedgerString(body.get("split_ledger_info"));
		JsonNode splitRoot = parseSplitLedgerJson(rawSplitLedgerJson);
		validateProportions(splitRoot);

		String applyType = String.valueOf(body.get("apply_type")).trim();
		if (!"dealer".equals(applyType) && !"distributor".equals(applyType)) {
			throw new BadRequestException("apply_type 取值无效");
		}
		String statusVal = String.valueOf(body.get("status")).trim();
		if (!"APPROVED".equals(statusVal) && !"REJECT".equals(statusVal)) {
			throw new BadRequestException("status 取值无效");
		}

		long applyId = parseRequiredLong(body.get("id"), "id 不能为空");
		long saveId = parseRequiredLong(body.get("save_id"), "save_id 不能为空");

		AdapayEntryApply apply =
				adapayEntryApplyMapper.selectOne(
						new LambdaQueryWrapper<AdapayEntryApply>()
								.eq(AdapayEntryApply::getId, applyId)
								.eq(AdapayEntryApply::getCompanyId, String.valueOf(companyId)));
		if (apply == null) {
			throw new ResourceException("未找到该申请记录");
		}

		String rawEntry = apply.getEntryId() == null ? "" : apply.getEntryId().trim();
		if (!StringUtils.hasText(rawEntry)) {
			throw new BadRequestException("申请关联的 entry_id 格式非法");
		}
		long entryMemberPk;
		try {
			entryMemberPk = Long.parseLong(rawEntry);
		} catch (NumberFormatException e) {
			throw new BadRequestException("申请关联的 entry_id 格式非法");
		}

		AdapayMember member =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getId, entryMemberPk));
		if (member == null) {
			throw new ResourceException("子商户会员不存在");
		}

		AdapaySettleAccount settleAccount =
				adapaySettleAccountMapper.selectOne(
						new LambdaQueryWrapper<AdapaySettleAccount>()
								.eq(AdapaySettleAccount::getMemberId, member.getId())
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.last("LIMIT 1"));

		if (STATUS_APPROVED.equals(statusVal)) {
			handleApproved(
					companyId,
					body,
					member,
					settleAccount,
					applyType,
					saveId,
					rawSplitLedgerJson);
		} else {
			handleReject(member, body.get("comments"));
		}

		finishEntryMemberSmsAndLogs(companyId, jwtOperatorId, body, member, saveId, statusVal);
	}

	private void validateRequiredKeys(Map<String, Object> body) {
		if (isMissing(body.get("id"))) {
			throw new BadRequestException("id 不能为空");
		}
		if (isMissing(body.get("save_id"))) {
			throw new BadRequestException("save_id 不能为空");
		}
		if (isMissing(body.get("status"))) {
			throw new BadRequestException("status 不能为空");
		}
		if (isMissing(body.get("apply_type"))) {
			throw new BadRequestException("apply_type 不能为空");
		}
		if (isMissing(body.get("split_ledger_info"))) {
			throw new BadRequestException("split_ledger_info 不能为空");
		}
	}

	private static boolean isMissing(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		return false;
	}

	/**
	 * 分账信息落库与校验均须基于客户端提交的字符串原文；非字符串（例如 JSON 体中已解析为 Map）不予再序列化替代。
	 */
	private String rawSplitLedgerString(Object raw) {
		if (raw instanceof String s) {
			return s;
		}
		throw new BadRequestException("分账信息格式错误");
	}

	private JsonNode parseSplitLedgerJson(String raw) {
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root == null || !root.isObject()) {
				throw new BadRequestException("分账信息格式错误");
			}
			return root;
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("分账信息格式错误");
		}
	}

	private void validateProportions(JsonNode splitLedger) {
		BigDecimal dealer = nz(splitLedger.get("dealer_proportion"));
		BigDecimal hq = nz(splitLedger.get("headquarters_proportion"));
		boolean dealerTruthy = dealer.compareTo(BigDecimal.ZERO) > 0;
		if (dealerTruthy) {
			if (hq.add(dealer).compareTo(new BigDecimal("100")) > 0) {
				throw new ResourceException("分账占比合必须小于等于100%");
			}
		} else {
			if (hq.compareTo(new BigDecimal("100")) > 0) {
				throw new ResourceException("分账占比必须小于等于100%");
			}
		}
	}

	private BigDecimal nz(JsonNode node) {
		if (node == null || node.isNull()) {
			return BigDecimal.ZERO;
		}
		if (node.isNumber()) {
			return node.decimalValue();
		}
		String s = node.asText("").trim();
		if (!StringUtils.hasText(s)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("分账信息格式错误");
		}
	}

	private long parseRequiredLong(Object raw, String missingMsg) {
		if (raw == null) {
			throw new BadRequestException(missingMsg);
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(missingMsg);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(missingMsg);
		}
	}

	private void handleApproved(
			long companyId,
			Map<String, Object> body,
			AdapayMember member,
			AdapaySettleAccount settleAccount,
			String applyType,
			long saveId,
			String rawSplitLedgerJson) {
		Map<String, Object> setting = adapayPaymentSettingRedisReader.getPaymentSetting(companyId);
		String appId = setting.get("app_id") == null ? "" : String.valueOf(setting.get("app_id")).trim();
		if (!StringUtils.hasText(appId)) {
			throw new ResourceException("AdaPay 支付配置不存在");
		}

		boolean isUpdateAction =
				settleAccount != null && StringUtils.hasText(settleAccount.getSettleAccountId());
		boolean isUpdateMember = Boolean.TRUE.equals(member.getIsCreated());

		String memberType = member.getMemberType() == null ? "" : member.getMemberType().trim();
		if (MEMBER_TYPE_PERSON.equals(memberType)) {
			handleApprovedPerson(
					companyId, appId, member, settleAccount, isUpdateAction, isUpdateMember, applyType, saveId);
		} else if (MEMBER_TYPE_CORP.equals(memberType)) {
			handleApprovedCorp(companyId, appId, member, isUpdateMember);
		}

		persistSplitLedger(companyId, applyType, saveId, rawSplitLedgerJson);
	}

	private void handleApprovedPerson(
			long companyId,
			String appId,
			AdapayMember member,
			AdapaySettleAccount settleAccount,
			boolean isUpdateAction,
			boolean isUpdateMember,
			String applyType,
			long saveId) {
		Map<String, Object> savePersonResult =
				adapaySubMerchantAdaPayOutboundGateway.savePersonMember(
						companyId, appId, member, isUpdateMember);
		Object pdata = savePersonResult.get("data");
		if (!(pdata instanceof Map<?, ?> pdm)) {
			adapayMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayMember>()
							.eq(AdapayMember::getCompanyId, companyId)
							.eq(AdapayMember::getId, member.getId())
							.set(AdapayMember::getAuditState, AUDIT_MEMBER_FAIL)
							.set(AdapayMember::getAuditDesc, ""));
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> personData = (Map<String, Object>) pdm;
		if (!"succeeded".equals(String.valueOf(personData.get("status")))) {
			String err =
					personData.get("error_msg") != null
							? String.valueOf(personData.get("error_msg"))
							: "";
			adapayMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayMember>()
							.eq(AdapayMember::getCompanyId, companyId)
							.eq(AdapayMember::getId, member.getId())
							.set(AdapayMember::getAuditState, AUDIT_MEMBER_FAIL)
							.set(AdapayMember::getAuditDesc, err));
			return;
		}

		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getId, member.getId())
						.set(AdapayMember::getIsCreated, true));

		boolean settleAccountAble = true;
		if (isUpdateAction && settleAccount != null) {
			Map<String, Object> deleteSettleResult =
					adapaySubMerchantSettleAccountGateway.deleteSettleAccount(
							companyId,
							appId,
							settleAccount.getSettleAccountId(),
							member.getId());
			Object dObj = deleteSettleResult.get("data");
			if (dObj instanceof Map<?, ?> dm) {
				if ("failed".equals(String.valueOf(dm.get("status")))) {
					Object code = dm.get("error_code");
					if (code != null && !"account_not_exists".equals(String.valueOf(code))) {
						String errMsg =
								dm.get("error_msg") != null ? String.valueOf(dm.get("error_msg")) : "";
						adapayMemberMapper.update(
								null,
								new LambdaUpdateWrapper<AdapayMember>()
										.eq(AdapayMember::getCompanyId, companyId)
										.eq(AdapayMember::getId, member.getId())
										.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL)
										.set(AdapayMember::getAuditDesc, errMsg));
						settleAccountAble = false;
					}
				}
			}
		}

		if (settleAccountAble) {
			adapayMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayMember>()
							.eq(AdapayMember::getCompanyId, companyId)
							.eq(AdapayMember::getId, member.getId())
							.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL));

			Map<String, Object> accountInfo =
					AdapaySubMerchantSettleAccountGateway.toAccountInfoMap(settleAccount);
			Map<String, Object> createSettleResult =
					adapaySubMerchantSettleAccountGateway.createSettleAccount(
							companyId, appId, member.getId(), accountInfo);
			Object cObj = createSettleResult.get("data");
			if (cObj instanceof Map<?, ?> cm
					&& "succeeded".equals(String.valueOf(cm.get("status")))) {
				adapayMemberMapper.update(
						null,
						new LambdaUpdateWrapper<AdapayMember>()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getId, member.getId())
								.set(AdapayMember::getAuditState, AUDIT_SUCCESS)
								.set(AdapayMember::getAuditDesc, ""));
				Object newId = cm.get("id");
				if (newId != null && settleAccount != null) {
					adapaySettleAccountMapper.update(
							null,
							new LambdaUpdateWrapper<AdapaySettleAccount>()
									.eq(AdapaySettleAccount::getCompanyId, companyId)
									.eq(AdapaySettleAccount::getMemberId, member.getId())
									.set(AdapaySettleAccount::getSettleAccountId, String.valueOf(newId)));
				}
				if ("dealer".equals(applyType)) {
					int now = (int) (System.currentTimeMillis() / 1000L);
					operatorsMapper.update(
							null,
							new LambdaUpdateWrapper<Operators>()
									.eq(Operators::getOperatorId, saveId)
									.eq(Operators::getCompanyId, companyId)
									.set(Operators::getAdapayOpenAccountTime, String.valueOf(now))
									.set(Operators::getUpdated, now));
				}
			} else {
				String errMsg = "";
				if (cObj instanceof Map<?, ?> cm2 && cm2.get("error_msg") != null) {
					errMsg = String.valueOf(cm2.get("error_msg"));
				}
				adapayMemberMapper.update(
						null,
						new LambdaUpdateWrapper<AdapayMember>()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getId, member.getId())
								.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL)
								.set(AdapayMember::getAuditDesc, errMsg));
			}
		}
	}

	private void handleApprovedCorp(long companyId, String appId, AdapayMember member, boolean isUpdateMember) {
		AdapayCorpMember corpRow =
				adapayCorpMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayCorpMember>()
								.eq(AdapayCorpMember::getMemberId, member.getId())
								.eq(AdapayCorpMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		Map<String, Object> merged = buildMergedForCorp(member, corpRow);
		Map<String, Object> saveCorpResult =
				adapaySubMerchantAdaPayOutboundGateway.saveCorpMember(
						companyId, appId, merged, corpRow, isUpdateMember, true);
		Object dataObj = saveCorpResult.get("data");
		if (!(dataObj instanceof Map<?, ?> dm)) {
			adapayMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayMember>()
							.eq(AdapayMember::getCompanyId, companyId)
							.eq(AdapayMember::getId, member.getId())
							.set(AdapayMember::getAuditState, AUDIT_MEMBER_FAIL)
							.set(AdapayMember::getAuditDesc, ""));
			if (corpRow != null) {
				adapayCorpMemberMapper.update(
						null,
						new LambdaUpdateWrapper<AdapayCorpMember>()
								.eq(AdapayCorpMember::getCompanyId, companyId)
								.eq(AdapayCorpMember::getMemberId, member.getId())
								.set(AdapayCorpMember::getAuditState, AUDIT_MEMBER_FAIL)
								.set(AdapayCorpMember::getAuditDesc, ""));
			}
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> corpData = (Map<String, Object>) dm;
		if ("failed".equals(String.valueOf(corpData.get("status")))) {
			String errMsg =
					corpData.get("error_msg") != null ? String.valueOf(corpData.get("error_msg")) : "";
			adapayMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayMember>()
							.eq(AdapayMember::getCompanyId, companyId)
							.eq(AdapayMember::getId, member.getId())
							.set(AdapayMember::getAuditState, AUDIT_FAIL)
							.set(AdapayMember::getAuditDesc, errMsg));
			if (corpRow != null) {
				adapayCorpMemberMapper.update(
						null,
						new LambdaUpdateWrapper<AdapayCorpMember>()
								.eq(AdapayCorpMember::getCompanyId, companyId)
								.eq(AdapayCorpMember::getMemberId, member.getId())
								.set(AdapayCorpMember::getAuditState, AUDIT_FAIL)
								.set(AdapayCorpMember::getAuditDesc, errMsg));
			}
			return;
		}

		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getId, member.getId())
						.set(AdapayMember::getAuditState, AUDIT_WAIT));
		if (corpRow != null) {
			adapayCorpMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayCorpMember>()
							.eq(AdapayCorpMember::getCompanyId, companyId)
							.eq(AdapayCorpMember::getMemberId, member.getId())
							.set(AdapayCorpMember::getAuditState, AUDIT_WAIT));
		}
		if (isUpdateMember && StringUtils.hasText(adapayCallbackProperties.getSettleOutboundBaseUrl())) {
			LinkedHashMap<String, Object> callbackData = new LinkedHashMap<>(corpData);
			callbackData.put("member_id", String.valueOf(member.getId()));
			callbackData.put("app_id", appId);
			callbackData.put("audit_state", "S");
			callbackData.put("audit_desc", "");
			adapayCallbackCorpMemberUpdateHandler.applySaveSplitLedgerCorpSucceeded(callbackData);
		}
	}

	private static Map<String, Object> buildMergedForCorp(AdapayMember member, AdapayCorpMember corp) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("member_id", member.getId());
		if (corp != null) {
			m.put("name", nullToEmpty(corp.getName()));
			m.put("prov_code", nullToEmpty(corp.getProvCode()));
			m.put("area_code", nullToEmpty(corp.getAreaCode()));
			m.put("social_credit_code", nullToEmpty(corp.getSocialCreditCode()));
			m.put("social_credit_code_expires", nullToEmpty(corp.getSocialCreditCodeExpires()));
			m.put("business_scope", nullToEmpty(corp.getBusinessScope()));
			m.put("legal_person", nullToEmpty(corp.getLegalPerson()));
			m.put("legal_cert_id", nullToEmpty(corp.getLegalCertId()));
			m.put("legal_cert_id_expires", nullToEmpty(corp.getLegalCertIdExpires()));
			m.put("legal_mp", nullToEmpty(corp.getLegalMp()));
			m.put("address", nullToEmpty(corp.getAddress()));
			m.put("zip_code", nullToEmpty(corp.getZipCode()));
			m.put("telphone", nullToEmpty(corp.getTelphone()));
			m.put("email", nullToEmpty(corp.getEmail()));
			m.put("bank_code", nullToEmpty(corp.getBankCode()));
			m.put("bank_acct_type", nullToEmpty(corp.getBankAcctType()));
			m.put("card_no", nullToEmpty(corp.getCardNo()));
			m.put("card_name", nullToEmpty(corp.getCardName()));
		} else {
			m.put("name", "");
			m.put("prov_code", "");
			m.put("area_code", "");
			m.put("social_credit_code", "");
			m.put("social_credit_code_expires", "");
			m.put("business_scope", "");
			m.put("legal_person", "");
			m.put("legal_cert_id", "");
			m.put("legal_cert_id_expires", "");
			m.put("legal_mp", "");
			m.put("address", "");
			m.put("zip_code", "");
			m.put("telphone", "");
			m.put("email", "");
			m.put("bank_code", "");
			m.put("bank_acct_type", "");
			m.put("card_no", "");
			m.put("card_name", "");
		}
		return m;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private void persistSplitLedger(
			long companyId, String applyType, long saveId, String rawSplitLedgerJson) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		if ("dealer".equals(applyType)) {
			operatorsMapper.update(
					null,
					new LambdaUpdateWrapper<Operators>()
							.eq(Operators::getCompanyId, companyId)
							.eq(Operators::getOperatorId, saveId)
							.set(Operators::getSplitLedgerInfo, rawSplitLedgerJson)
							.set(Operators::getUpdated, now));
		} else {
			distributorMapper.update(
					null,
					new LambdaUpdateWrapper<Distributor>()
							.eq(Distributor::getCompanyId, companyId)
							.eq(Distributor::getDistributorId, saveId)
							.set(Distributor::getSplitLedgerInfo, rawSplitLedgerJson)
							.set(Distributor::getUpdated, (long) now));
		}
	}

	private void handleReject(AdapayMember member, Object commentsObj) {
		String comments = commentsObj == null ? "" : String.valueOf(commentsObj);
		long companyId = member.getCompanyId();
		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getId, member.getId())
						.set(AdapayMember::getAuditState, AUDIT_FAIL)
						.set(AdapayMember::getAuditDesc, comments));
		String mt = member.getMemberType() == null ? "" : member.getMemberType().trim();
		if (MEMBER_TYPE_CORP.equals(mt) || "crop".equals(mt)) {
			adapayCorpMemberMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayCorpMember>()
							.eq(AdapayCorpMember::getCompanyId, companyId)
							.eq(AdapayCorpMember::getMemberId, member.getId())
							.set(AdapayCorpMember::getAuditState, AUDIT_FAIL)
							.set(AdapayCorpMember::getAuditDesc, comments));
		}
	}

	private void finishEntryMemberSmsAndLogs(
			long companyId,
			long jwtOperatorId,
			Map<String, Object> body,
			AdapayMember member,
			long saveId,
			String statusVal) {
		long applyId = parseRequiredLong(body.get("id"), "id 不能为空");
		Object isSmsRaw = body.get("is_sms");
		String isSmsForApply = isSmsRaw == null ? null : String.valueOf(isSmsRaw);
		String comments =
				body.get("comments") == null ? "" : String.valueOf(body.get("comments"));

		adapayEntryApplyMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayEntryApply>()
						.eq(AdapayEntryApply::getCompanyId, String.valueOf(companyId))
						.eq(AdapayEntryApply::getId, applyId)
						.set(AdapayEntryApply::getStatus, statusVal)
						.set(AdapayEntryApply::getComments, comments)
						.set(AdapayEntryApply::getIsSms, isSmsForApply));

		Object isSmsForMember = body.get("is_sms");
		adapayMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getId, member.getId())
						.set(AdapayMember::getIsSms, isSmsForMember == null ? null : String.valueOf(isSmsForMember)));

		AdapayMember newMemberInfo =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getId, member.getId()));

		if (isSmsTruthy(body.get("is_sms"))
				&& newMemberInfo != null
				&& !AUDIT_WAIT.equals(newMemberInfo.getAuditState())) {
			try {
				log.debug(
						"SMS sub_account_approved skipped (Java parity v1); use SmsManagerService in full rollout");
			} catch (Exception ignored) {
			}
		}

		adapaySubMerchantLastIsSmsRedisWriter.setLastIsSms(companyId, body.get("is_sms"));

		String opType = newMemberInfo != null ? newMemberInfo.getOperatorType() : member.getOperatorType();
		if (opType != null) {
			String ot = opType.trim();
			if ("distributor".equals(ot) || "dealer".equals(ot)) {
				writeSplitLedgerLogs(companyId, jwtOperatorId, body, saveId, newMemberInfo != null ? newMemberInfo : member, ot);
			}
		}
	}

	private void writeSplitLedgerLogs(
			long companyId,
			long jwtOperatorId,
			Map<String, Object> body,
			long saveId,
			AdapayMember member,
			String memberOperatorType) {
		String name = "";
		long relId = saveId;
		if ("distributor".equals(memberOperatorType)) {
			Distributor d =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, saveId)
									.last("LIMIT 1"));
			if (d != null && d.getName() != null) {
				name = d.getName();
			}
			relId = saveId;
		} else if ("dealer".equals(memberOperatorType)) {
			Integer memberOpId = member.getOperatorId();
			if (memberOpId != null && memberOpId > 0) {
				Operators operatorInfo =
						operatorsMapper.selectOne(
								new LambdaQueryWrapper<Operators>()
										.eq(Operators::getCompanyId, companyId)
										.eq(Operators::getOperatorId, memberOpId.longValue())
										.last("LIMIT 1"));
				if (operatorInfo != null) {
					name =
							operatorInfo.getUsername() != null
									? operatorInfo.getUsername()
									: "";
					Boolean isMain = operatorInfo.getIsDealerMain();
					if (Boolean.FALSE.equals(isMain)
							&& StringUtils.hasText(operatorInfo.getDealerParentId())) {
						try {
							relId = Long.parseLong(operatorInfo.getDealerParentId().trim());
						} catch (NumberFormatException e) {
							relId = operatorInfo.getOperatorId();
						}
					} else {
						relId = operatorInfo.getOperatorId();
					}
				}
			}
		}

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("status", String.valueOf(body.get("status")));
		logParams.put("name", name);
		logParams.put("is_sms", body.get("is_sms"));

		long merchantRelId = jwtOperatorId;
		adapayOperationLogRecordPort.logRecord(
				logParams,
				merchantRelId,
				"sub_approve/save_split_ledger",
				"merchant",
				jwtOperatorId);
		adapayOperationLogRecordPort.logRecord(
				logParams, relId, "sub_approve/save_split_ledger", memberOperatorType, jwtOperatorId);
	}

	private static boolean isSmsTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}
}
