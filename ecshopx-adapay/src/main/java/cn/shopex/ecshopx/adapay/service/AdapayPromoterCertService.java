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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantAdaPayOutboundGateway;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantSettleAccountGateway;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdapayPromoterCertService {

	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private static final String AUDIT_MEMBER_FAIL = "C";
	private static final String AUDIT_ACCOUNT_FAIL = "D";
	private static final String AUDIT_SUCCESS = "E";

	private static final Map<String, String> AUDIT_STATE_TO_AUDIT_VALUE =
			Map.of(
					"A", "AUDIT_WAIT",
					"B", "AUDIT_FAIL",
					"C", "AUDIT_MEMBER_FAIL",
					"D", "AUDIT_ACCOUNT_FAIL",
					"E", "AUDIT_SUCCESS");

	private final AdapayPromoterCertService self;
	private final AdapayAdaPayPaymentSettingReadService adapayAdaPayPaymentSettingReadService;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway;
	private final AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway;
	private final PromoterMapper promoterMapper;

	public AdapayPromoterCertService(
			@Lazy AdapayPromoterCertService self,
			AdapayAdaPayPaymentSettingReadService adapayAdaPayPaymentSettingReadService,
			AdapayMemberMapper adapayMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway,
			AdapaySubMerchantSettleAccountGateway adapaySubMerchantSettleAccountGateway,
			PromoterMapper promoterMapper) {
		this.self = self;
		this.adapayAdaPayPaymentSettingReadService = adapayAdaPayPaymentSettingReadService;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapaySubMerchantAdaPayOutboundGateway = adapaySubMerchantAdaPayOutboundGateway;
		this.adapaySubMerchantSettleAccountGateway = adapaySubMerchantSettleAccountGateway;
		this.promoterMapper = promoterMapper;
	}

	public Map<String, Object> getCertInfo(long companyId, long userId, boolean dataMasking) {
		long cnt =
				promoterMapper.selectCount(
						Wrappers.<Promoter>lambdaQuery()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId));
		if (cnt == 0L) {
			throw new ResourceException("非推广员不能分销员认证");
		}

		AdapayMember member =
				adapayMemberMapper.selectOne(
						Wrappers.<AdapayMember>lambdaQuery()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getOperatorId, (int) userId)
								.eq(AdapayMember::getOperatorType, "promoter")
								.last("LIMIT 1"));
		if (member == null) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("member_id", 0L);
			empty.put("tel_no", "");
			empty.put("card_id", "");
			empty.put("cert_id", "");
			empty.put("card_name", "");
			empty.put("cert_status", List.of());
			return empty;
		}

		String auditStateNorm =
				member.getAuditState() == null ? "" : member.getAuditState().trim();
		String auditValue = AUDIT_STATE_TO_AUDIT_VALUE.getOrDefault(auditStateNorm, "");

		Map<String, Object> certStatus = new LinkedHashMap<>();
		certStatus.put("audit_state", nz(member.getAuditState()));
		certStatus.put("audit_value", auditValue);
		certStatus.put("audit_desc", nz(member.getAuditDesc()));
		certStatus.put("error_info", nz(member.getErrorInfo()));
		certStatus.put("create_time", intTimeToString(member.getCreateTime()));
		certStatus.put("update_time", intTimeToString(member.getUpdateTime()));

		AdapaySettleAccount settle =
				adapaySettleAccountMapper.selectOne(
						Wrappers.<AdapaySettleAccount>lambdaQuery()
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.eq(AdapaySettleAccount::getMemberId, member.getId())
								.orderByDesc(AdapaySettleAccount::getId)
								.last("LIMIT 1"));

		String telPlain;
		String cardPlain;
		String certPlain;
		String namePlain;
		if (settle == null) {
			telPlain = "";
			cardPlain = "";
			certPlain = "";
			namePlain = "";
		} else {
			telPlain = nz(settle.getTelNo());
			cardPlain = nz(settle.getCardId());
			certPlain = nz(settle.getCertId());
			namePlain = nz(settle.getCardName());
		}

		String telOut;
		String cardOut;
		String certOut;
		String nameOut;
		if (!dataMasking) {
			telOut = telPlain;
			cardOut = cardPlain;
			certOut = certPlain;
			nameOut = namePlain;
		} else {
			telOut = maskOrEmpty(DataMasking.maskMobile(telPlain));
			cardOut = maskOrEmpty(DataMasking.maskBankcard(cardPlain));
			certOut = maskOrEmpty(DataMasking.maskIdcard(certPlain));
			nameOut = maskOrEmpty(DataMasking.maskTruename(namePlain));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("member_id", member.getId());
		out.put("tel_no", telOut);
		out.put("card_id", cardOut);
		out.put("cert_id", certOut);
		out.put("card_name", nameOut);
		out.put("settle_account_id", settle == null ? "" : nz(settle.getSettleAccountId()));
		out.put("cert_status", certStatus);
		return out;
	}

	public void createCert(long companyId, long promoterUserId, Map<String, Object> input) {
		String telNo = str(input, "tel_no");
		String cardId = str(input, "card_id");
		String certId = str(input, "cert_id");
		String cardName = str(input, "card_name");

		validateFields(telNo, cardId, certId, cardName);

		String appId = adapayAdaPayPaymentSettingReadService.loadAppId(companyId);
		if (!StringUtils.hasText(appId)) {
			throw new ResourceException("adapay 支付信息未配置");
		}

		long memberPk =
				self.insertTransactional(companyId, promoterUserId, appId, telNo, cardId, certId, cardName);

		AdapayMember memberRow =
				adapayMemberMapper.selectOne(
						Wrappers.<AdapayMember>lambdaQuery()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getId, memberPk));
		if (memberRow == null) {
			throw new ResourceException("用户创建失败");
		}

		AdapaySettleAccount settleRow =
				adapaySettleAccountMapper.selectOne(
						Wrappers.<AdapaySettleAccount>lambdaQuery()
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.eq(AdapaySettleAccount::getMemberId, memberPk)
								.orderByDesc(AdapaySettleAccount::getId)
								.last("LIMIT 1"));
		if (settleRow == null) {
			throw new ResourceException("结算账户创建失败");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		Map<String, Object> savePersonResult =
				adapaySubMerchantAdaPayOutboundGateway.savePersonMember(companyId, appId, memberRow, false);

		if ("succeeded".equals(statusOfData(savePersonResult))) {
			new LambdaUpdateChainWrapper<>(adapayMemberMapper)
					.eq(AdapayMember::getCompanyId, companyId)
					.eq(AdapayMember::getId, memberPk)
					.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL)
					.set(AdapayMember::getUpdateTime, now)
					.update();

			Map<String, Object> createSettleResult =
					adapaySubMerchantSettleAccountGateway.createSettleAccount(
							companyId, appId, memberPk, AdapaySubMerchantSettleAccountGateway.toAccountInfoMap(settleRow));

			if ("succeeded".equals(statusOfData(createSettleResult))) {
				new LambdaUpdateChainWrapper<>(adapayMemberMapper)
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getId, memberPk)
						.set(AdapayMember::getAuditState, AUDIT_SUCCESS)
						.set(AdapayMember::getAuditDesc, "")
						.set(AdapayMember::getUpdateTime, now)
						.update();

				String settleAccountId = settleIdFrom(createSettleResult);
				new LambdaUpdateChainWrapper<>(adapaySettleAccountMapper)
						.eq(AdapaySettleAccount::getCompanyId, companyId)
						.eq(AdapaySettleAccount::getId, settleRow.getId())
						.set(AdapaySettleAccount::getSettleAccountId, settleAccountId)
						.set(AdapaySettleAccount::getUpdateTime, now)
						.update();
			} else {
				new LambdaUpdateChainWrapper<>(adapayMemberMapper)
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getId, memberPk)
						.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL)
						.set(AdapayMember::getAuditDesc, errorMsgFrom(createSettleResult))
						.set(AdapayMember::getUpdateTime, now)
						.update();
			}
		} else {
			new LambdaUpdateChainWrapper<>(adapayMemberMapper)
					.eq(AdapayMember::getCompanyId, companyId)
					.eq(AdapayMember::getId, memberPk)
					.set(AdapayMember::getAuditState, AUDIT_MEMBER_FAIL)
					.set(AdapayMember::getAuditDesc, errorMsgFrom(savePersonResult))
					.set(AdapayMember::getUpdateTime, now)
					.update();
		}
	}

	public void updateCert(long companyId, Map<String, Object> input) {
		long memberPk = parseLongMemberId(input);
		String telNo = str(input, "tel_no");
		String cardId = str(input, "card_id");
		String certId = str(input, "cert_id");
		String cardName = str(input, "card_name");
		validateUpdateCertFields(telNo, cardId, certId, cardName);

		String appId = adapayAdaPayPaymentSettingReadService.loadAppId(companyId);
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("adapay 支付信息未配置");
		}

		AdapayMember memberDbInfo =
				adapayMemberMapper.selectOne(
						Wrappers.<AdapayMember>lambdaQuery()
								.eq(AdapayMember::getId, memberPk)
								.eq(AdapayMember::getCompanyId, companyId));
		if (memberDbInfo == null) {
			throw new ResourceException("开户信息不存在");
		}
		String originalAuditState = memberDbInfo.getAuditState();

		self.updateMemberAndSettleInTransaction(companyId, memberPk, telNo, cardId, certId, cardName);

		AdapaySettleAccount settleAccount =
				adapaySettleAccountMapper.selectOne(
						Wrappers.<AdapaySettleAccount>lambdaQuery()
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.eq(AdapaySettleAccount::getMemberId, memberPk)
								.orderByDesc(AdapaySettleAccount::getId)
								.last("LIMIT 1"));

		AdapayMember updatedMemberDbInfo =
				adapayMemberMapper.selectOne(
						Wrappers.<AdapayMember>lambdaQuery()
								.eq(AdapayMember::getId, memberPk)
								.eq(AdapayMember::getCompanyId, companyId));
		if (updatedMemberDbInfo == null) {
			throw new ResourceException("开户信息不存在");
		}

		boolean isUpdate =
				Objects.equals(originalAuditState, AUDIT_SUCCESS)
						|| Objects.equals(originalAuditState, AUDIT_ACCOUNT_FAIL);

		int now = (int) (System.currentTimeMillis() / 1000L);
		Map<String, Object> savePersonResult =
				adapaySubMerchantAdaPayOutboundGateway.savePersonMember(
						companyId, appId, updatedMemberDbInfo, isUpdate);

		if ("succeeded".equals(statusOfData(savePersonResult))) {
			boolean settleAccountAble = true;

			boolean shouldDelete = false;
			String sidTrim = "";
			if (settleAccount != null && StringUtils.hasText(settleAccount.getSettleAccountId())) {
				sidTrim = settleAccount.getSettleAccountId().trim();
				shouldDelete = StringUtils.hasText(sidTrim) && !"0".equals(sidTrim);
			}

			if (shouldDelete) {
				Map<String, Object> deleteResult =
						adapaySubMerchantSettleAccountGateway.deleteSettleAccount(
								companyId, appId, sidTrim, memberPk);
				if ("failed".equals(statusOfData(deleteResult))) {
					String errCode = errorCodeFrom(deleteResult);
					if (StringUtils.hasText(errCode) && !"account_not_exists".equals(errCode)) {
						new LambdaUpdateChainWrapper<>(adapayMemberMapper)
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getId, memberPk)
								.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL)
								.set(AdapayMember::getAuditDesc, errorMsgFrom(deleteResult))
								.set(AdapayMember::getUpdateTime, now)
								.update();
						settleAccountAble = false;
					}
				}
			}

			if (settleAccountAble) {
				if (settleAccount == null) {
					throw new ResourceException("未查询到更新数据");
				}
				new LambdaUpdateChainWrapper<>(adapayMemberMapper)
						.eq(AdapayMember::getCompanyId, companyId)
						.eq(AdapayMember::getId, memberPk)
						.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL)
						.set(AdapayMember::getUpdateTime, now)
						.update();

				Map<String, Object> createSettleResult =
						adapaySubMerchantSettleAccountGateway.createSettleAccount(
								companyId,
								appId,
								memberPk,
								AdapaySubMerchantSettleAccountGateway.toAccountInfoMap(settleAccount));

				if ("succeeded".equals(statusOfData(createSettleResult))) {
					new LambdaUpdateChainWrapper<>(adapayMemberMapper)
							.eq(AdapayMember::getCompanyId, companyId)
							.eq(AdapayMember::getId, memberPk)
							.set(AdapayMember::getAuditState, AUDIT_SUCCESS)
							.set(AdapayMember::getAuditDesc, "")
							.set(AdapayMember::getUpdateTime, now)
							.update();

					String settleAccountId = settleIdFrom(createSettleResult);
					new LambdaUpdateChainWrapper<>(adapaySettleAccountMapper)
							.eq(AdapaySettleAccount::getCompanyId, companyId)
							.eq(AdapaySettleAccount::getId, settleAccount.getId())
							.set(AdapaySettleAccount::getSettleAccountId, settleAccountId)
							.set(AdapaySettleAccount::getUpdateTime, now)
							.update();
				} else {
					new LambdaUpdateChainWrapper<>(adapayMemberMapper)
							.eq(AdapayMember::getCompanyId, companyId)
							.eq(AdapayMember::getId, memberPk)
							.set(AdapayMember::getAuditState, AUDIT_ACCOUNT_FAIL)
							.set(AdapayMember::getAuditDesc, errorMsgFrom(createSettleResult))
							.set(AdapayMember::getUpdateTime, now)
							.update();
				}
			}
		} else {
			new LambdaUpdateChainWrapper<>(adapayMemberMapper)
					.eq(AdapayMember::getCompanyId, companyId)
					.eq(AdapayMember::getId, memberDbInfo.getId())
					.set(AdapayMember::getAuditState, AUDIT_MEMBER_FAIL)
					.set(AdapayMember::getAuditDesc, errorMsgFrom(savePersonResult))
					.set(AdapayMember::getUpdateTime, now)
					.update();
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateMemberAndSettleInTransaction(
			long companyId, long memberPk, String telNo, String cardId, String certId, String cardName) {
		int now = (int) (System.currentTimeMillis() / 1000L);

		AdapaySettleAccount settleRow =
				adapaySettleAccountMapper.selectOne(
						Wrappers.<AdapaySettleAccount>lambdaQuery()
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.eq(AdapaySettleAccount::getMemberId, memberPk)
								.orderByDesc(AdapaySettleAccount::getId)
								.last("LIMIT 1"));
		if (settleRow == null) {
			throw new ResourceException("未查询到更新数据");
		}

		int memberRows =
				adapayMemberMapper.update(
						null,
						Wrappers.<AdapayMember>lambdaUpdate()
								.eq(AdapayMember::getId, memberPk)
								.eq(AdapayMember::getCompanyId, companyId)
								.set(AdapayMember::getTelNo, telNo)
								.set(AdapayMember::getUserName, cardName)
								.set(AdapayMember::getCertId, certId)
								.set(AdapayMember::getAuditState, "0")
								.set(AdapayMember::getAuditDesc, "")
								.set(AdapayMember::getUpdateTime, now));
		if (memberRows <= 0) {
			throw new ResourceException("用户更新失败");
		}

		int settleRows =
				adapaySettleAccountMapper.update(
						null,
						Wrappers.<AdapaySettleAccount>lambdaUpdate()
								.eq(AdapaySettleAccount::getId, settleRow.getId())
								.eq(AdapaySettleAccount::getCompanyId, companyId)
								.set(AdapaySettleAccount::getCardId, cardId)
								.set(AdapaySettleAccount::getCardName, cardName)
								.set(AdapaySettleAccount::getCertId, certId)
								.set(AdapaySettleAccount::getTelNo, telNo)
								.set(AdapaySettleAccount::getUpdateTime, now));
		if (settleRows <= 0) {
			throw new ResourceException("结算账户更新失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public long insertTransactional(
			long companyId,
			long promoterUserId,
			String appId,
			String telNo,
			String cardId,
			String certId,
			String cardName) {
		int now = (int) (System.currentTimeMillis() / 1000L);

		AdapayMember member = new AdapayMember();
		member.setAppId(appId);
		member.setOperatorId((int) promoterUserId);
		member.setOperatorType("promoter");
		member.setTelNo(telNo);
		member.setUserName(cardName);
		member.setCertId(certId);
		member.setCertType("00");
		member.setCompanyId(companyId);
		member.setMemberType("person");
		member.setAuditState("0");
		member.setCreateTime(now);
		member.setUpdateTime(now);

		int ins = adapayMemberMapper.insert(member);
		if (ins <= 0 || member.getId() == null || member.getId() <= 0L) {
			throw new ResourceException("用户创建失败");
		}
		long memberId = member.getId();

		AdapaySettleAccount settle = new AdapaySettleAccount();
		settle.setAppId(appId);
		settle.setMemberId(memberId);
		settle.setBankAcctType("2");
		settle.setCardId(cardId);
		settle.setCardName(cardName);
		settle.setCertId(certId);
		settle.setCertType("00");
		settle.setTelNo(telNo);
		settle.setCompanyId(companyId);
		settle.setChannel("bank_account");
		settle.setCreateTime(now);
		settle.setUpdateTime(now);

		int saIns = adapaySettleAccountMapper.insert(settle);
		if (saIns <= 0) {
			throw new ResourceException("结算账户创建失败");
		}
		return memberId;
	}

	private static long parseLongMemberId(Map<String, Object> input) {
		if (input == null) {
			throw new ResourceException("认证ID必传");
		}
		if (!input.containsKey("member_id")) {
			throw new ResourceException("认证ID必传");
		}
		Object raw = input.get("member_id");
		if (raw == null) {
			throw new ResourceException("认证ID必传");
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("认证ID必传");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("开户信息不存在");
		}
	}

	private static void validateUpdateCertFields(
			String telNo, String cardId, String certId, String cardName) {
		if (!isValidMobile11(telNo)) {
			throw new ResourceException("手机号码必须是11位");
		}
		if (!StringUtils.hasText(cardId)) {
			throw new ResourceException("银行账号必填");
		}
		if (certId.length() != 18) {
			throw new ResourceException("身份证号必须是18位");
		}
		if (!StringUtils.hasText(cardName) || cardName.codePointCount(0, cardName.length()) > 15) {
			throw new ResourceException("开户人姓名必填且不能超过15个字符");
		}
		if (StringUtils.hasText(certId) && !LEGAL_CERT_PATTERN.matcher(certId).matches()) {
			throw new ResourceException("身份证号码格式错误");
		}
	}

	private static void validateFields(String telNo, String cardId, String certId, String cardName) {
		if (!isValidMobile11(telNo)) {
			throw new ResourceException("手机号码必须是11位");
		}
		if (!StringUtils.hasText(cardId)) {
			throw new ResourceException("银行账号必填");
		}
		if (certId.length() != 18) {
			throw new ResourceException("身份证号必须是18位");
		}
		if (!StringUtils.hasText(cardName) || cardName.codePointCount(0, cardName.length()) > 15) {
			throw new ResourceException("开户人姓名必填且不能超过15个字符");
		}
		if (StringUtils.hasText(certId) && !LEGAL_CERT_PATTERN.matcher(certId).matches()) {
			throw new ResourceException("身份证号码格式错误");
		}
	}

	private static boolean isValidMobile11(String s) {
		return StringUtils.hasText(s) && s.length() == 11 && s.chars().allMatch(Character::isDigit);
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String intTimeToString(Integer t) {
		if (t == null) {
			return "";
		}
		return String.valueOf(t.intValue());
	}

	private static String maskOrEmpty(String masked) {
		return masked == null ? "" : masked;
	}

	private static String str(Map<String, Object> input, String key) {
		Object v = input == null ? null : input.get(key);
		if (v == null) {
			return "";
		}
		return v.toString().trim();
	}

	private static String statusOfData(Map<String, Object> root) {
		if (root == null) {
			return "failed";
		}
		Object data = root.get("data");
		if (!(data instanceof Map<?, ?> dm)) {
			return "failed";
		}
		Object st = dm.get("status");
		return st == null ? "" : String.valueOf(st);
	}

	private static String errorMsgFrom(Map<String, Object> root) {
		if (root == null) {
			return "";
		}
		Object data = root.get("data");
		if (!(data instanceof Map<?, ?> dm)) {
			return "";
		}
		Object em = dm.get("error_msg");
		return em == null ? "" : String.valueOf(em);
	}

	private static String errorCodeFrom(Map<String, Object> root) {
		if (root == null) {
			return "";
		}
		Object data = root.get("data");
		if (!(data instanceof Map<?, ?> dm)) {
			return "";
		}
		Object ec = dm.get("error_code");
		return ec == null ? "" : String.valueOf(ec);
	}

	private static String settleIdFrom(Map<String, Object> root) {
		if (root == null) {
			return "";
		}
		Object data = root.get("data");
		if (!(data instanceof Map<?, ?> dm)) {
			return "";
		}
		Object id = dm.get("id");
		return id == null ? "" : String.valueOf(id);
	}
}
