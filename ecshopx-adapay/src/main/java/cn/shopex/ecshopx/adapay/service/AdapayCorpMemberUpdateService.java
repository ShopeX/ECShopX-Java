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
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMemberUpdateLog;
import cn.shopex.ecshopx.adapay.mapper.AdapayBankCodesMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberUpdateLogMapper;
import cn.shopex.ecshopx.adapay.service.callback.AdapayCorpMemberUpdateOutboundGateway;
import cn.shopex.ecshopx.adapay.service.callback.handler.AdapayCallbackCorpMemberUpdateHandler;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.employee.MemberOperatorContextService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
public class AdapayCorpMemberUpdateService {

	private static final long MAX_FILE_SIZE = (long) (8.5 * 1024 * 1024);
	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private final AdapayCorpMemberUpdateService self;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapayBankCodesMapper adapayBankCodesMapper;
	private final FileStorageService fileStorageService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	@SuppressWarnings("unused")
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;

	private final AdapayCorpMemberUpdateOutboundGateway adapayCorpMemberUpdateOutboundGateway;
	private final AdapayMemberUpdateLogMapper adapayMemberUpdateLogMapper;
	private final AdapayCallbackCorpMemberUpdateHandler adapayCallbackCorpMemberUpdateHandler;
	private final MemberOperatorContextService memberOperatorContextService;
	private final AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService;
	private final ObjectMapper objectMapper;

	public AdapayCorpMemberUpdateService(
			@Lazy AdapayCorpMemberUpdateService self,
			AdapayMemberMapper adapayMemberMapper,
			AdapayBankCodesMapper adapayBankCodesMapper,
			FileStorageService fileStorageService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayCorpMemberUpdateOutboundGateway adapayCorpMemberUpdateOutboundGateway,
			AdapayMemberUpdateLogMapper adapayMemberUpdateLogMapper,
			AdapayCallbackCorpMemberUpdateHandler adapayCallbackCorpMemberUpdateHandler,
			MemberOperatorContextService memberOperatorContextService,
			AdapayCreateMemberOperationLogService adapayCreateMemberOperationLogService,
			ObjectMapper objectMapper) {
		this.self = self;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapayBankCodesMapper = adapayBankCodesMapper;
		this.fileStorageService = fileStorageService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapayCorpMemberUpdateOutboundGateway = adapayCorpMemberUpdateOutboundGateway;
		this.adapayMemberUpdateLogMapper = adapayMemberUpdateLogMapper;
		this.adapayCallbackCorpMemberUpdateHandler = adapayCallbackCorpMemberUpdateHandler;
		this.memberOperatorContextService = memberOperatorContextService;
		this.adapayCreateMemberOperationLogService = adapayCreateMemberOperationLogService;
		this.objectMapper = objectMapper;
	}

	public void update(
			long companyId,
			Map<String, Object> jwtMap,
			Map<String, String> formFields,
			MultipartFile attachFile) {
		validateUpdateActionRules(formFields);
		checkUpdateParams(formFields, attachFile, null);
		self.updateTransactional(companyId, jwtMap, formFields, attachFile);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateTransactional(
			long companyId,
			Map<String, Object> jwtMap,
			Map<String, String> formFields,
			MultipartFile attachFile) {
		long memberId = parsePositiveLongRequired(formFields.get("member_id"));
		AdapayMember memberInfo =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getId, memberId)
								.last("LIMIT 1"));
		if (memberInfo == null) {
			throw new BadRequestException("开户信息不存在", 400);
		}
		if (!StringUtils.hasText(memberInfo.getAppId())) {
			throw new BadRequestException("支付应用未配置");
		}
		int rows =
				adapayMemberMapper.update(
						null,
						new LambdaUpdateWrapper<AdapayMember>()
								.eq(AdapayMember::getId, memberId)
								.set(AdapayMember::getIsUpdate, 1));
		if (rows <= 0) {
			throw new ResourceException("用户信息更新失败");
		}

		byte[] zipBytesOrNull = null;
		String attachPublicUrlOrNull = null;
		if (attachFile == null || attachFile.isEmpty()) {
			formFields.remove("attach_file");
			formFields.remove("attach_file_name");
		} else {
			String originalFilename = attachFile.getOriginalFilename();
			String safeName = originalFilename != null ? originalFilename : "attach.zip";
			String ext = extensionLower(safeName);
			if (!"zip".equals(ext)) {
				throw new BadRequestException("文件类型不符合要求");
			}
			try {
				zipBytesOrNull = attachFile.getBytes();
			} catch (IOException e) {
				throw new ResourceException("用户信息更新失败");
			}
			long epochSecond = System.currentTimeMillis() / 1000L;
			String md5Name = DigestUtils.md5DigestAsHex(safeName.getBytes(StandardCharsets.UTF_8));
			String attachPath = "adapay/" + companyId + "/" + epochSecond + "/" + md5Name + ".zip";
			fileStorageService.put("file", attachPath, zipBytesOrNull);
			formFields.put("attach_file", attachPath);
			formFields.put("attach_file_name", safeName);
			attachPublicUrlOrNull = fileStorageService.url("file", attachPath);
		}

		Map<String, Object> envelope =
				adapayCorpMemberUpdateOutboundGateway.corpMemberUpdate(
						companyId,
						memberInfo.getAppId(),
						formFields,
						zipBytesOrNull,
						attachPublicUrlOrNull);
		Map<String, Object> data = unwrapData(envelope);
		String status = str(data.get("status")).toLowerCase();
		String appId = memberInfo.getAppId();
		String memberIdStr = trimToEmpty(formFields.get("member_id"));
		int now = (int) (System.currentTimeMillis() / 1000L);

		if ("failed".equals(status)) {
			String auditDesc = str(data.get("error_msg"));
			insertMemberUpdateLog(
					companyId, appId, memberIdStr, formFields, "B", auditDesc, now);
			throw new BadRequestException("数据更新失败: " + auditDesc);
		}

		insertMemberUpdateLog(companyId, appId, memberIdStr, formFields, "A", "", now);

		if ("succeeded".equals(status)) {
			Map<String, Object> callbackData = new LinkedHashMap<>(data);
			callbackData.put("app_id", appId);
			callbackData.put("member_id", memberIdStr);
			callbackData.put("audit_state", "S");
			callbackData.put("audit_desc", "");
			adapayCallbackCorpMemberUpdateHandler.succeeded(callbackData);
		}

		long jwtOperatorId = toLong(jwtMap.get("operator_id"));
		String jwtOperatorType = stringVal(jwtMap.get("operator_type"));
		Long jwtDistributorId = toLongObject(jwtMap.get("distributor_id"));
		Map<String, Object> operatorContext =
				memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);
		adapayCreateMemberOperationLogService.recordUpdateMemberLog(
				companyId, jwtMap, trimToEmpty(formFields.get("name")), operatorContext);
	}

	private void insertMemberUpdateLog(
			long companyId,
			String appId,
			String memberIdStr,
			Map<String, String> formFields,
			String auditState,
			String auditDesc,
			int now) {
		String plainJson;
		try {
			plainJson = objectMapper.writeValueAsString(serializeLogData(companyId, formFields));
		} catch (JsonProcessingException e) {
			throw new ResourceException("用户信息更新失败");
		}
		String stored = sensitiveFieldEncryptor.encrypt(plainJson);
		AdapayMemberUpdateLog log = new AdapayMemberUpdateLog();
		log.setCompanyId(String.valueOf(companyId));
		log.setAppId(appId);
		log.setMemberId(memberIdStr);
		log.setData(stored);
		log.setAuditState(auditState);
		log.setAuditDesc(auditDesc == null ? "" : auditDesc);
		log.setCreateTime(now);
		log.setUpdateTime(now);
		adapayMemberUpdateLogMapper.insert(log);
	}

	private LinkedHashMap<String, Object> serializeLogData(long companyId, Map<String, String> formFields) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", String.valueOf(companyId));
		m.put("member_id", strOrEmpty(formFields.get("member_id")));
		m.put("name", strOrEmpty(formFields.get("name")));
		m.put("prov_code", strOrEmpty(formFields.get("prov_code")));
		m.put("area_code", strOrEmpty(formFields.get("area_code")));
		m.put("social_credit_code", strOrEmpty(formFields.get("social_credit_code")));
		m.put("social_credit_code_expires", strOrEmpty(formFields.get("social_credit_code_expires")));
		m.put("business_scope", strOrEmpty(formFields.get("business_scope")));
		m.put("legal_person", strOrEmpty(formFields.get("legal_person")));
		m.put("legal_cert_id", strOrEmpty(formFields.get("legal_cert_id")));
		m.put("legal_cert_id_expires", strOrEmpty(formFields.get("legal_cert_id_expires")));
		m.put("legal_mp", strOrEmpty(formFields.get("legal_mp")));
		m.put("address", strOrEmpty(formFields.get("address")));
		m.put("zip_code", strOrEmpty(formFields.get("zip_code")));
		m.put("telphone", strOrEmpty(formFields.get("telphone")));
		m.put("email", strOrEmpty(formFields.get("email")));
		m.put("bank_code", strOrEmpty(formFields.get("bank_code")));
		m.put("bank_acct_type", strOrEmpty(formFields.get("bank_acct_type")));
		m.put("card_no", strOrEmpty(formFields.get("card_no")));
		m.put("card_name", strOrEmpty(formFields.get("card_name")));
		m.put("attach_file", strOrEmpty(formFields.get("attach_file")));
		m.put("attach_file_name", strOrEmpty(formFields.get("attach_file_name")));
		return m;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> unwrapData(Map<String, Object> envelope) {
		if (envelope == null) {
			return Map.of();
		}
		Object d = envelope.get("data");
		if (d instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Map.of();
	}

	private void validateUpdateActionRules(Map<String, String> formFields) {
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
		if (!StringUtils.hasText(bankAcct) || (!"1".equals(bankAcct) && !"2".equals(bankAcct))) {
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
		if (!StringUtils.hasText(legalCert) || legalCert.length() != 18) {
			throw new ResourceException("法人身份证号码必须是18位");
		}
		if (!StringUtils.hasText(trimToEmpty(formFields.get("legal_cert_id_expires")))) {
			throw new ResourceException("法人身份证有效期必填");
		}
		String legalMp = trimToEmpty(formFields.get("legal_mp"));
		if (legalMp.length() != 11) {
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
		if (!StringUtils.hasText(trimToEmpty(formFields.get("card_name")))) {
			throw new ResourceException("银行卡对应的户名必填");
		}
	}

	private void checkUpdateParams(
			Map<String, String> formFields, MultipartFile attachFile, MultipartFile confirmLetterFileOptional) {
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

	private static String strOrEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String extensionLower(String filename) {
		int i = filename.lastIndexOf('.');
		if (i < 0 || i >= filename.length() - 1) {
			return "";
		}
		return filename.substring(i + 1).toLowerCase();
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
