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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdapayCorpMemberCreateService {

	private static final long MAX_FILE_SIZE = (long) (8.5 * 1024 * 1024);
	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");
	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[\\w!#$%&'*+/=?`{|}~^.-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^.-]+)*@"
					+ "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");
	private static final DateTimeFormatter ORDER_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private final AdapayCorpMemberCreateService self;
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

	public AdapayCorpMemberCreateService(
			@Lazy AdapayCorpMemberCreateService self,
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

	public boolean createFromMultipart(
			long companyId,
			Map<String, Object> jwtMap,
			Map<String, String> formFields,
			MultipartFile attachFile,
			MultipartFile confirmLetterFileOptional) {
		validateActionRules(formFields, attachFile);
		checkParams(formFields, attachFile, confirmLetterFileOptional);
		return self.createTransactional(companyId, jwtMap, formFields, attachFile);
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean createTransactional(
			long companyId,
			Map<String, Object> jwtMap,
			Map<String, String> formFields,
			MultipartFile attachFile) {
		createCorpMemberInternal(companyId, jwtMap, formFields, attachFile);
		return true;
	}

	private void createCorpMemberInternal(
			long companyId,
			Map<String, Object> jwtMap,
			Map<String, String> formFields,
			MultipartFile attachFile) {
		String orderNo =
				LocalDateTime.now().format(ORDER_TS)
						+ ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
		String appId = adapayPaymentSettingReadService.loadAppId(companyId);

		long jwtOperatorId = toLong(jwtMap.get("operator_id"));
		String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
		Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
		Map<String, Object> operatorContext =
				memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);

		String addressNorm = formFields.get("address");
		String nameNorm = formFields.get("name");
		String emailNorm = formFields.get("email");
		String legalMpNorm = formFields.get("legal_mp");
		String socialCreditNorm = formFields.get("social_credit_code");
		String provCode = formFields.get("prov_code");
		String areaCode = formFields.get("area_code");

		int now = (int) (System.currentTimeMillis() / 1000L);
		AdapayMember member = new AdapayMember();
		member.setAppId(appId);
		member.setLocation(addressNorm);
		member.setCompanyId(companyId);
		member.setOperatorId(toInt(operatorContext.get("operator_id")));
		member.setOperatorType(stringVal(operatorContext.get("operator_type")));
		member.setEmail(emailNorm);
		member.setMemberType("corp");
		member.setTelNo(legalMpNorm);
		member.setUserName(nameNorm);
		member.setCertType("");
		member.setCertId(socialCreditNorm);
		member.setAuditState("0");
		member.setCreateTime(now);
		member.setUpdateTime(now);

		int ins = adapayMemberMapper.insert(member);
		if (ins <= 0 || member.getId() == null || member.getId() <= 0) {
			throw new ResourceException("用户信息保存失败");
		}
		long memberId = member.getId();

		String attachPath;
		String attachOrigName;
		try {
			String originalFilename = attachFile.getOriginalFilename();
			String safeName = originalFilename != null ? originalFilename : "attach.zip";
			String ext = extensionLower(safeName);
			if (!"zip".equals(ext)) {
				throw new BadRequestException("文件类型不符合要求");
			}
			long epochSecond = System.currentTimeMillis() / 1000L;
			String md5Name = DigestUtils.md5DigestAsHex(safeName.getBytes(StandardCharsets.UTF_8));
			attachPath = "adapay/" + companyId + "/" + epochSecond + "/" + md5Name + ".zip";
			fileStorageService.put("file", attachPath, attachFile.getBytes());
			attachOrigName = safeName;
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("用户信息保存失败");
		}

		int jwtOpInt = toInt(jwtOperatorId);
		AdapayCorpMember corp = new AdapayCorpMember();
		corp.setAppId(appId);
		corp.setOrderNo(orderNo);
		corp.setMemberId(memberId);
		corp.setCompanyId(companyId);
		corp.setOperatorId(jwtOpInt);
		corp.setDealerId(0L);
		corp.setDistributorId(0L);
		corp.setName(nameNorm);
		corp.setProvCode(provCode);
		corp.setAreaCode(areaCode);
		corp.setSocialCreditCode(formFields.get("social_credit_code"));
		corp.setSocialCreditCodeExpires(formFields.get("social_credit_code_expires"));
		corp.setBusinessScope(formFields.get("business_scope"));
		corp.setLegalPerson(formFields.get("legal_person"));
		corp.setLegalCertId(formFields.get("legal_cert_id"));
		corp.setLegalCertIdExpires(formFields.get("legal_cert_id_expires"));
		corp.setLegalMp(legalMpNorm);
		corp.setAddress(addressNorm);
		corp.setZipCode(formFields.getOrDefault("zip_code", ""));
		corp.setTelphone(formFields.getOrDefault("telphone", ""));
		corp.setEmail(emailNorm);
		corp.setAttachFile(attachPath);
		corp.setAttachFileName(attachOrigName);
		corp.setConfirmLetterFile("");
		corp.setConfirmLetterFileName("");
		corp.setBankCode(formFields.get("bank_code"));
		corp.setBankAcctType(formFields.get("bank_acct_type"));
		corp.setCardNo(formFields.get("card_no"));
		corp.setCardName(formFields.get("card_name"));
		corp.setCreateTime(now);
		corp.setUpdateTime(now);

		int corpIns = adapayCorpMemberMapper.insert(corp);
		if (corpIns <= 0) {
			throw new ResourceException("企业用户信息保存失败");
		}

		String bankName = resolveBankName(formFields.get("bank_code"));

		AdapaySettleAccount settle = new AdapaySettleAccount();
		settle.setAppId(appId);
		settle.setMemberId(memberId);
		settle.setCompanyId(companyId);
		settle.setChannel("bank_account");
		settle.setCardId(formFields.get("card_no"));
		settle.setCardName(formFields.get("card_name"));
		settle.setCertId(formFields.get("legal_cert_id"));
		settle.setCertType("00");
		settle.setTelNo(legalMpNorm);
		settle.setBankCode(formFields.get("bank_code"));
		settle.setBankName(bankName);
		settle.setBankAcctType(formFields.get("bank_acct_type"));
		settle.setCreateTime(now);
		settle.setUpdateTime(now);

		int saIns = adapaySettleAccountMapper.insert(settle);
		if (saIns <= 0) {
			throw new BadRequestException("结算账户创建失败");
		}

		String provName = resolveAreaName(provCode);
		String areaName = resolveAreaName(areaCode);
		String applyAddress = provName + "-" + areaName;

		AdapayEntryApply apply = new AdapayEntryApply();
		apply.setUserName(nameNorm);
		apply.setCompanyId(String.valueOf(companyId));
		apply.setEntryId(String.valueOf(memberId));
		apply.setApplyType(stringVal(operatorContext.get("operator_type")));
		apply.setAddress(applyAddress);
		apply.setStatus("WAIT_APPROVE");
		apply.setCreateTime(now);
		apply.setUpdateTime(now);

		int apIns = adapayEntryApplyMapper.insert(apply);
		if (apIns <= 0) {
			throw new BadRequestException("开户申请创建失败");
		}

		adapayCreateMemberOperationLogService.recordCreateMemberLog(companyId, jwtMap, nameNorm, operatorContext);
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

	private void validateActionRules(Map<String, String> formFields, MultipartFile attachFile) {
		if (!StringUtils.hasText(trimToEmpty(formFields.get("name")))) {
			throw new BadRequestException("企业名称必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("area")))) {
			throw new BadRequestException("地区必填");
		}
		String email = formFields.get("email");
		if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
			throw new BadRequestException("邮箱格式有误");
		}
		String bankAcct = trimToEmpty(formFields.get("bank_acct_type"));
		if (!"1".equals(bankAcct) && !"2".equals(bankAcct)) {
			throw new BadRequestException("银行账户类型错误");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("social_credit_code")))) {
			throw new BadRequestException("营业执照号必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("social_credit_code_expires")))) {
			throw new BadRequestException("商户有效日期必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("business_scope")))) {
			throw new BadRequestException("经营范围必填");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("legal_person")))) {
			throw new BadRequestException("法人姓名必填");
		}
		String legalCert = trimToEmpty(formFields.get("legal_cert_id"));
		if (legalCert.length() != 18) {
			throw new BadRequestException("法人身份证号码必须是18位");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("legal_cert_id_expires")))) {
			throw new BadRequestException("法人身份证有效期必填");
		}
		String legalMp = trimToEmpty(formFields.get("legal_mp"));
		if (legalMp.length() != 11 || !legalMp.chars().allMatch(Character::isDigit)) {
			throw new BadRequestException("法人手机号必须是11位");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("address")))) {
			throw new BadRequestException("企业地址必填");
		}
		if (attachFile == null || attachFile.isEmpty()) {
			throw new ResourceException("附件必须上传");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("bank_code")))) {
			throw new BadRequestException("请选择结算银行卡所属银行");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("card_no")))) {
			throw new BadRequestException("银行卡号必填");
		}
		String cardName = trimToEmpty(formFields.get("card_name"));
		if (!StringUtils.hasText(cardName)) {
			throw new BadRequestException("银行卡对应的户名必填");
		}
		if (cardName.codePointCount(0, cardName.length()) > 20) {
			throw new BadRequestException("银行卡对应的户名必填");
		}
	}

	private void checkParams(
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

		if (attachFile == null || attachFile.isEmpty()) {
			throw new ResourceException("附件必须上传");
		}
		if (attachFile.getSize() >= MAX_FILE_SIZE) {
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
			formFields.put("business_scope", truncateByCodePoints(trimToEmpty(formFields.get("business_scope")), 500));
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
