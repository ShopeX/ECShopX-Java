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
import cn.shopex.ecshopx.adapay.mapper.AdapayBankCodesMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayCorpMemberReadService {

	private final AdapayCorpMemberMapper adapayCorpMemberMapper;
	private final AdapayBankCodesMapper adapayBankCodesMapper;
	private final FileStorageService fileStorageService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdapayCorpMemberReadService(
			AdapayCorpMemberMapper adapayCorpMemberMapper,
			AdapayBankCodesMapper adapayBankCodesMapper,
			FileStorageService fileStorageService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
		this.adapayBankCodesMapper = adapayBankCodesMapper;
		this.fileStorageService = fileStorageService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> get(long companyId, int operatorId) {
		AdapayCorpMember row = adapayCorpMemberMapper.selectOne(
				new LambdaQueryWrapper<AdapayCorpMember>()
						.eq(AdapayCorpMember::getCompanyId, companyId)
						.eq(AdapayCorpMember::getOperatorId, operatorId)
						.orderByAsc(AdapayCorpMember::getId)
						.last("LIMIT 1"));
		if (row == null) {
			throw new BadRequestException("用户信息不存在");
		}

		String legalPerson = sensitiveFieldEncryptor.decrypt(row.getLegalPerson());
		String legalCertId = sensitiveFieldEncryptor.decrypt(row.getLegalCertId());
		String legalMp = sensitiveFieldEncryptor.decrypt(row.getLegalMp());
		String cardNoDecrypted = sensitiveFieldEncryptor.decrypt(row.getCardNo());

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", row.getId());
		map.put("app_id", row.getAppId());
		map.put("order_no", row.getOrderNo());
		map.put("member_id", row.getMemberId());
		map.put("company_id", row.getCompanyId());
		Integer operatorIdVal = row.getOperatorId();
		if (operatorIdVal == null || operatorIdVal == 0) {
			map.put("operator_id", null);
		} else {
			map.put("operator_id", operatorIdVal.intValue());
		}
		map.put("name", row.getName());
		map.put(
				"prov_code",
				StringUtils.hasText(row.getProvCode()) ? row.getProvCode().trim() : null);
		map.put("area_code", nullToEmpty(row.getAreaCode()));
		map.put("social_credit_code", row.getSocialCreditCode());
		map.put("social_credit_code_expires", row.getSocialCreditCodeExpires());
		map.put("business_scope", row.getBusinessScope());
		map.put("legal_person", legalPerson);
		map.put("legal_cert_id", legalCertId);
		map.put("legal_cert_id_expires", row.getLegalCertIdExpires());
		map.put("legal_mp", legalMp);
		map.put("address", row.getAddress());
		map.put("zip_code", row.getZipCode());
		map.put("telphone", row.getTelphone());
		map.put("email", row.getEmail());
		map.put("attach_file", row.getAttachFile());
		map.put("attach_file_name", row.getAttachFileName());
		map.put("confirm_letter_file", row.getConfirmLetterFile());
		map.put("confirm_letter_file_name", row.getConfirmLetterFileName());
		map.put("bank_code", row.getBankCode());
		map.put("bank_acct_type", row.getBankAcctType());
		map.put("card_no", cardNoDecrypted);
		map.put("card_name", row.getCardName());
		map.put("audit_state", row.getAuditState());
		map.put("audit_desc", row.getAuditDesc());
		map.put("create_time", row.getCreateTime());
		map.put("update_time", row.getUpdateTime());
		map.put("disabled_type", "");

		String bankCode = Optional.ofNullable(row.getBankCode()).map(String::trim).orElse("");
		if (StringUtils.hasText(bankCode)) {
			AdapayBankCodes bank = adapayBankCodesMapper.selectOne(
					new LambdaQueryWrapper<AdapayBankCodes>()
							.eq(AdapayBankCodes::getBankCode, bankCode)
							.last("LIMIT 1"));
			String bankName = bank == null || bank.getBankName() == null ? "" : bank.getBankName();
			map.put("bank_name", bankName);
		}

		if (StringUtils.hasText(row.getAttachFile())) {
			map.put(
					"attach_file_url",
					fileStorageService.privateDownloadUrl("file", row.getAttachFile().trim(), 3600));
		}
		if (StringUtils.hasText(row.getConfirmLetterFile())) {
			map.put(
					"confirm_letter_file_url",
					fileStorageService.privateDownloadUrl(
							"file", row.getConfirmLetterFile().trim(), 3600));
		}

		map.put("div_fee_mode", "内扣");
		return map;
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}
}
