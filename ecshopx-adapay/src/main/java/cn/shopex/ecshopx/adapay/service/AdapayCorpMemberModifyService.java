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

import cn.shopex.ecshopx.adapay.domain.AdapayBankCodes;
import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayEntryApply;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayRegions;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayBankCodesMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayEntryApplyMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayRegionsMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdapayCorpMemberModifyService {

	private static final long MAX_FILE_SIZE = (long) (8.5 * 1024 * 1024);
	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");
	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[\\w!#$%&'*+/=?`{|}~^.-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^.-]+)*@"
					+ "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

	private final AdapayCorpMemberModifyService self;
	private final AdapayAdaPayPaymentSettingReadService adapayPaymentSettingReadService;
	private final MemberOperatorContextService memberOperatorContextService;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapayCorpMemberMapper adapayCorpMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final AdapayEntryApplyMapper adapayEntryApplyMapper;
	private final AdapayBankCodesMapper adapayBankCodesMapper;
	private final AdapayRegionsMapper adapayRegionsMapper;
	private final FileStorageService fileStorageService;
	private final AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService;

	public AdapayCorpMemberModifyService(
			@Lazy AdapayCorpMemberModifyService self,
			AdapayAdaPayPaymentSettingReadService adapayPaymentSettingReadService,
			MemberOperatorContextService memberOperatorContextService,
			AdapayMemberMapper adapayMemberMapper,
			AdapayCorpMemberMapper adapayCorpMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			AdapayEntryApplyMapper adapayEntryApplyMapper,
			AdapayBankCodesMapper adapayBankCodesMapper,
			AdapayRegionsMapper adapayRegionsMapper,
			FileStorageService fileStorageService,
			AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService) {
		this.self = self;
		this.adapayPaymentSettingReadService = adapayPaymentSettingReadService;
		this.memberOperatorContextService = memberOperatorContextService;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.adapayEntryApplyMapper = adapayEntryApplyMapper;
		this.adapayBankCodesMapper = adapayBankCodesMapper;
		this.adapayRegionsMapper = adapayRegionsMapper;
		this.fileStorageService = fileStorageService;
		this.adapayCreateMemberOperationLogService = adapayCreateMemberOperationLogService;
	}

	public boolean modifyFromMultipart(
			long companyId,
			Map<String, Object> jwtMap,
			Map<String, String> formFields,
			MultipartFile attachFile,
			MultipartFile confirmLetterFileOptional) {
		validateModifyActionRules(formFields);
		checkModifyParams(formFields, attachFile, confirmLetterFileOptional);
		return self.modifyTransactional(companyId, jwtMap, formFields, attachFile);
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean modifyTransactional(
			long companyId,
			Map<String, Object> jwtMap,
			Map<String, String> formFields,
			MultipartFile attachFile) {
		long memberId = parsePositiveLongRequired(formFields.get("member_id"));
		adapayPaymentSettingReadService.loadAppId(companyId);

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
			throw new ResourceException("开户信息不存在", 400);
		}

		boolean isSuccessUpdate =
				"D".equals(member.getAuditState()) || "E".equals(member.getAuditState());
		int nowEpochSecondsInt = (int) (System.currentTimeMillis() / 1000L);

		LambdaUpdateWrapper<AdapayMember> memUw =
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getId, memberId)
						.eq(AdapayMember::getCompanyId, companyId)
						.set(AdapayMember::getLocation, trimToEmpty(formFields.get("address")))
						.set(AdapayMember::getEmail, formFields.getOrDefault("email", ""))
						.set(AdapayMember::getTelNo, trimToEmpty(formFields.get("legal_mp")))
						.set(AdapayMember::getUserName, trimToEmpty(formFields.get("name")))
						.set(AdapayMember::getAuditState, "0")
						.set(AdapayMember::getAuditDesc, "")
						.set(AdapayMember::getUpdateTime, nowEpochSecondsInt);
		if (!isSuccessUpdate) {
			memUw.set(AdapayMember::getCertId, trimToEmpty(formFields.get("social_credit_code")));
		}
		int memUpd = adapayMemberMapper.update(null, memUw);
		if (memUpd <= 0) {
			throw new ResourceException("用户信息更新失败");
		}

		String attachPath = null;
		String attachOrigName = null;
		if (attachFile != null && !attachFile.isEmpty()) {
			try {
				String originalFilename = attachFile.getOriginalFilename();
				String safeName = originalFilename != null ? originalFilename : "attach.zip";
				String ext = extensionLower(safeName);
				if (!"zip".equals(ext)) {
					throw new ResourceException("文件类型不符合要求");
				}
				long epochSecond = System.currentTimeMillis() / 1000L;
				String md5Name = DigestUtils.md5DigestAsHex(safeName.getBytes(StandardCharsets.UTF_8));
				attachPath = "adapay/" + companyId + "/" + epochSecond + "/" + md5Name + ".zip";
				byte[] zipBytes = attachFile.getBytes();
				fileStorageService.put("file", attachPath, zipBytes);
				attachOrigName = safeName;
			} catch (ResourceException e) {
				throw e;
			} catch (IOException e) {
				throw new ResourceException("用户信息更新失败");
			} catch (RuntimeException e) {
				throw new ResourceException("用户信息更新失败");
			}
		}

		LambdaUpdateWrapper<AdapayCorpMember> corpUw =
				new LambdaUpdateWrapper<AdapayCorpMember>()
						.eq(AdapayCorpMember::getMemberId, memberId)
						.eq(AdapayCorpMember::getCompanyId, companyId)
						.set(AdapayCorpMember::getName, trimToEmpty(formFields.get("name")))
						.set(AdapayCorpMember::getProvCode, trimToEmpty(formFields.get("prov_code")))
						.set(AdapayCorpMember::getAreaCode, trimToEmpty(formFields.get("area_code")))
						.set(AdapayCorpMember::getSocialCreditCode, trimToEmpty(formFields.get("social_credit_code")))
						.set(
								AdapayCorpMember::getSocialCreditCodeExpires,
								trimToEmpty(formFields.get("social_credit_code_expires")))
						.set(AdapayCorpMember::getBusinessScope, trimToEmpty(formFields.get("business_scope")))
						.set(AdapayCorpMember::getLegalPerson, trimToEmpty(formFields.get("legal_person")))
						.set(AdapayCorpMember::getLegalCertId, trimToEmpty(formFields.get("legal_cert_id")))
						.set(
								AdapayCorpMember::getLegalCertIdExpires,
								trimToEmpty(formFields.get("legal_cert_id_expires")))
						.set(AdapayCorpMember::getLegalMp, trimToEmpty(formFields.get("legal_mp")))
						.set(AdapayCorpMember::getAddress, trimToEmpty(formFields.get("address")))
						.set(AdapayCorpMember::getZipCode, formFields.getOrDefault("zip_code", ""))
						.set(AdapayCorpMember::getTelphone, formFields.getOrDefault("telphone", ""))
						.set(AdapayCorpMember::getEmail, formFields.getOrDefault("email", ""))
						.set(AdapayCorpMember::getBankCode, trimToEmpty(formFields.get("bank_code")))
						.set(AdapayCorpMember::getBankAcctType, trimToEmpty(formFields.get("bank_acct_type")))
						.set(AdapayCorpMember::getCardNo, trimToEmpty(formFields.get("card_no")))
						.set(AdapayCorpMember::getCardName, trimToEmpty(formFields.get("card_name")))
						.set(AdapayCorpMember::getUpdateTime, nowEpochSecondsInt);
		if (attachPath != null) {
			corpUw.set(AdapayCorpMember::getAttachFile, attachPath)
					.set(AdapayCorpMember::getAttachFileName, attachOrigName != null ? attachOrigName : "");
		}
		int corpUpd = adapayCorpMemberMapper.update(null, corpUw);
		if (corpUpd <= 0) {
			throw new ResourceException("企业用户信息更新失败");
		}

		String bankName = resolveBankName(formFields.get("bank_code"));
		LambdaUpdateWrapper<AdapaySettleAccount> settleUw =
				new LambdaUpdateWrapper<AdapaySettleAccount>()
						.eq(AdapaySettleAccount::getMemberId, memberId)
						.eq(AdapaySettleAccount::getCompanyId, companyId)
						.set(AdapaySettleAccount::getBankAcctType, trimToEmpty(formFields.get("bank_acct_type")))
						.set(AdapaySettleAccount::getCardId, trimToEmpty(formFields.get("card_no")))
						.set(AdapaySettleAccount::getCardName, trimToEmpty(formFields.get("card_name")))
						.set(AdapaySettleAccount::getCertId, trimToEmpty(formFields.get("legal_cert_id")))
						.set(AdapaySettleAccount::getCertType, "00")
						.set(AdapaySettleAccount::getTelNo, trimToEmpty(formFields.get("legal_mp")))
						.set(AdapaySettleAccount::getChannel, "bank_account")
						.set(AdapaySettleAccount::getBankCode, trimToEmpty(formFields.get("bank_code")))
						.set(AdapaySettleAccount::getBankName, bankName)
						.set(AdapaySettleAccount::getUpdateTime, nowEpochSecondsInt);
		int settleUpd = adapaySettleAccountMapper.update(null, settleUw);
		if (settleUpd <= 0) {
			throw new ResourceException("结算账户更新失败");
		}

		String provName = resolveAreaName(formFields.get("prov_code"));
		String areaName = resolveAreaName(formFields.get("area_code"));
		String applyAddress = provName + "-" + areaName;

		AdapayEntryApply apply = new AdapayEntryApply();
		apply.setUserName(trimToEmpty(formFields.get("name")));
		apply.setCompanyId(String.valueOf(companyId));
		apply.setEntryId(String.valueOf(memberId));
		apply.setApplyType(stringVal(operatorContext.get("operator_type")));
		apply.setAddress(applyAddress);
		apply.setStatus("WAIT_APPROVE");
		apply.setCreateTime(nowEpochSecondsInt);
		apply.setUpdateTime(nowEpochSecondsInt);

		int apIns = adapayEntryApplyMapper.insert(apply);
		if (apIns <= 0) {
			throw new ResourceException("开户申请创建失败");
		}

		adapayCreateMemberOperationLogService.recordUpdateMemberLog(
				companyId, jwtMap, trimToEmpty(formFields.get("name")), operatorContext);
		return true;
	}

	private void validateModifyActionRules(Map<String, String> formFields) {
		if (!StringUtils.hasText(trimToEmpty(formFields.get("member_id")))) {
			throw new ResourceException("开户id不能为空");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("name")))) {
			throw new ResourceException("企业名称必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("area")))) {
			throw new ResourceException("地区必填");
		}
		String bankAcct = trimToEmpty(formFields.get("bank_acct_type"));
		if (!"1".equals(bankAcct) && !"2".equals(bankAcct)) {
			throw new ResourceException("银行账户类型错误");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("social_credit_code")))) {
			throw new ResourceException("营业执照号必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("social_credit_code_expires")))) {
			throw new ResourceException("商户有效日期必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("business_scope")))) {
			throw new ResourceException("经营范围必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("legal_person")))) {
			throw new ResourceException("法人姓名必填");
		}
		String legalCert = trimToEmpty(formFields.get("legal_cert_id"));
		if (legalCert.length() != 18) {
			throw new ResourceException("法人身份证号码必须是18位");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("legal_cert_id_expires")))) {
			throw new ResourceException("法人身份证有效期必填");
		}
		String legalMp = trimToEmpty(formFields.get("legal_mp"));
		if (legalMp.length() != 11 || !legalMp.chars().allMatch(Character::isDigit)) {
			throw new ResourceException("法人手机号必须是11位");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("address")))) {
			throw new ResourceException("企业地址必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("bank_code")))) {
			throw new ResourceException("请选择结算银行卡所属银行");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("card_no")))) {
			throw new ResourceException("银行卡号必填");
		}
		String cardName = trimToEmpty(formFields.get("card_name"));
		if (!StringUtils.hasText(cardName)) {
			throw new ResourceException("银行卡对应的户名必填");
		}
		if (cardName.codePointCount(0, cardName.length()) > 20) {
			throw new ResourceException("银行卡对应的户名必填");
		}
		String email = trimToEmpty(formFields.get("email"));
		if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email).matches()) {
			throw new ResourceException("邮箱格式有误");
		}
	}

	private void checkModifyParams(
			Map<String, String> formFields,
			MultipartFile attachFile,
			MultipartFile confirmLetterFileOptional) {
		String nameRaw = trimToEmpty(formFields.get("name"));
		String bankAcct = trimToEmpty(formFields.get("bank_acct_type"));
		String cardName = trimToEmpty(formFields.get("card_name"));
		if ("1".equals(bankAcct) && !cardName.equals(nameRaw)) {
			throw new ResourceException("银行卡对应的户名，必须与企业名称一致");
		}

		String areaStr = formFields.get("area");
		if (!StringUtils.hasText(areaStr)) {
			throw new ResourceException("地区数据格式错误");
		}
		List<String> parts = List.of(areaStr.split(","));
		if (parts.size() != 2
				|| !StringUtils.hasText(parts.get(0).trim())
				|| !StringUtils.hasText(parts.get(1).trim())) {
			throw new ResourceException("地区数据格式错误");
		}
		formFields.put("prov_code", parts.get(0).trim());
		formFields.put("area_code", parts.get(1).trim());

		if (attachFile != null && !attachFile.isEmpty() && attachFile.getSize() >= MAX_FILE_SIZE) {
			throw new ResourceException("附件不能超过8M");
		}

		String zip = formFields.get("zip_code");
		if (StringUtils.hasText(zip)) {
			formFields.put("zip_code", truncateByCodePoints(zip.trim(), 6));
		} else {
			formFields.put("zip_code", "");
		}

		if (StringUtils.hasText(formFields.get("address"))) {
			formFields.put("address", truncateByCodePoints(trimToEmpty(formFields.get("address")), 60));
		}
		if (StringUtils.hasText(formFields.get("name"))) {
			formFields.put("name", truncateByCodePoints(trimToEmpty(formFields.get("name")), 30));
		}
		if (StringUtils.hasText(formFields.get("business_scope"))) {
			formFields.put(
					"business_scope", truncateByCodePoints(trimToEmpty(formFields.get("business_scope")), 500));
		}
		if (StringUtils.hasText(formFields.get("legal_person"))) {
			formFields.put("legal_person", truncateByCodePoints(trimToEmpty(formFields.get("legal_person")), 20));
		}

		String legalCertId = trimToEmpty(formFields.get("legal_cert_id"));
		if (StringUtils.hasText(legalCertId) && !LEGAL_CERT_PATTERN.matcher(legalCertId).matches()) {
			throw new ResourceException("法人身份证号码格式错误");
		}

		String bankCode = trimToEmpty(formFields.get("bank_code"));
		long bc =
				adapayBankCodesMapper.selectCount(
						new LambdaQueryWrapper<AdapayBankCodes>().eq(AdapayBankCodes::getBankCode, bankCode));
		if (bc <= 0) {
			throw new ResourceException("请选择正确的结算银行卡所属银行");
		}

		if (confirmLetterFileOptional != null
				&& !confirmLetterFileOptional.isEmpty()
				&& confirmLetterFileOptional.getSize() >= MAX_FILE_SIZE) {
			throw new ResourceException("附件不能超过8M");
		}
	}

	private String resolveBankName(String bankCode) {
		AdapayBankCodes one =
				adapayBankCodesMapper.selectOne(
						new LambdaQueryWrapper<AdapayBankCodes>()
								.eq(AdapayBankCodes::getBankCode, bankCode)
								.last("LIMIT 1"));
		if (one == null || !StringUtils.hasText(one.getBankName())) {
			throw new ResourceException("银行编号不存在");
		}
		return one.getBankName();
	}

	private String resolveAreaName(String areaCode) {
		AdapayRegions one =
				adapayRegionsMapper.selectOne(
						new LambdaQueryWrapper<AdapayRegions>()
								.eq(AdapayRegions::getAreaCode, areaCode)
								.last("LIMIT 1"));
		if (one == null || !StringUtils.hasText(one.getAreaName())) {
			throw new ResourceException("地区编号不存在");
		}
		return one.getAreaName();
	}

	private static long parsePositiveLongRequired(String raw) {
		String s = trimToEmpty(raw);
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("开户id不能为空");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new ResourceException("开户id不能为空");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("开户id不能为空");
		}
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static String truncateByCodePoints(String s, int maxCodePoints) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		int count = s.codePointCount(0, s.length());
		if (count <= maxCodePoints) {
			return s;
		}
		int end = s.offsetByCodePoints(0, maxCodePoints);
		return s.substring(0, end);
	}

	private static String extensionLower(String filename) {
		int i = filename.lastIndexOf('.');
		if (i < 0 || i >= filename.length() - 1) {
			return "";
		}
		return filename.substring(i + 1).toLowerCase();
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
