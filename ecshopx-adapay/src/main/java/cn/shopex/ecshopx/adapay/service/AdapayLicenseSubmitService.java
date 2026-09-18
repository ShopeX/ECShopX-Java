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

import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.adapay.domain.AdapayMerchantResident;
import cn.shopex.ecshopx.adapay.domain.AdapayUploadLicense;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantEntryMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantResidentMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayUploadLicenseMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayLicenseSubmitService {

	private static final Pattern LEGAL_CERT_PATTERN =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[X\\d]$");

	private final AdapayMerchantEntryMapper merchantEntryMapper;
	private final AdapayMerchantResidentMapper merchantResidentMapper;
	private final AdapayUploadLicenseMapper uploadLicenseMapper;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;

	public AdapayLicenseSubmitService(
			AdapayMerchantEntryMapper merchantEntryMapper,
			AdapayMerchantResidentMapper merchantResidentMapper,
			AdapayUploadLicenseMapper uploadLicenseMapper,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader) {
		this.merchantEntryMapper = merchantEntryMapper;
		this.merchantResidentMapper = merchantResidentMapper;
		this.uploadLicenseMapper = uploadLicenseMapper;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
	}

	public void submit(long companyId, Map<String, String> params) {
		validateActionParams(params);

		AdapayMerchantEntry entry =
				merchantEntryMapper.selectOne(
						new LambdaQueryWrapper<AdapayMerchantEntry>()
								.eq(AdapayMerchantEntry::getCompanyId, companyId)
								.last("LIMIT 1"));

		if (entry != null && "1".equals(entry.getEntryMerType())) {
			String socialCredit = params.getOrDefault("social_credit_code_url", "");
			if (!StringUtils.hasText(socialCredit)) {
				throw new ResourceException("商户类型为企业商户，三证合一码必传");
			}
		}

		AdapayMerchantResident resident =
				merchantResidentMapper.selectOne(
						new LambdaQueryWrapper<AdapayMerchantResident>()
								.eq(AdapayMerchantResident::getCompanyId, companyId)
								.last("LIMIT 1"));

		if (resident != null && "01".equals(resident.getFeeType())) {
			if (!StringUtils.hasText(params.getOrDefault("business_add", ""))) {
				throw new ResourceException("入驻的费率类型为线上，请传入商户的业务网址或者商城地址");
			}
		}
		if (resident != null && "02".equals(resident.getFeeType())) {
			if (!StringUtils.hasText(params.getOrDefault("store_url", ""))) {
				throw new ResourceException("入驻的费率类型为线下，门店必传");
			}
		}

		String certBack = params.getOrDefault("cert_back_image_url", "");
		String certFront = params.getOrDefault("cert_front_image_url", "");
		String certId = params.getOrDefault("cert_id", "");
		String certName = params.getOrDefault("cert_name", "");
		boolean anyShareholder =
				StringUtils.hasText(certBack)
						|| StringUtils.hasText(certFront)
						|| StringUtils.hasText(certId)
						|| StringUtils.hasText(certName);
		if (anyShareholder
				&& !(StringUtils.hasText(certBack)
						&& StringUtils.hasText(certFront)
						&& StringUtils.hasText(certId)
						&& StringUtils.hasText(certName))) {
			throw new ResourceException("股东信息必填");
		}

		String legalCertIdFrontId = null;
		String legalCertIdBackId = null;
		String accountOpeningPermitId = null;
		String socialCreditCodeId = null;
		String storeId = null;
		String transactionTestRecordId = null;
		String webPicId = null;
		String leaseContractId = null;
		String settleAccountCertificateId = null;
		String bussSupportMaterialsId = null;
		String icpRegistrationLicenseId = null;
		String industryQualifyDocLicenseId = null;
		String certBackImageId = null;
		String certFrontImageId = null;

		List<AdapayUploadLicense> licenses =
				uploadLicenseMapper.selectList(
						new LambdaQueryWrapper<AdapayUploadLicense>()
								.eq(AdapayUploadLicense::getCompanyId, companyId));

		for (AdapayUploadLicense value : licenses) {
			String fileUrl = value.getFileUrl() == null ? "" : value.getFileUrl().trim();
			if (fileUrl.isEmpty()) {
				continue;
			}
			String picId = value.getPicId() == null ? "" : value.getPicId().trim();
			String idStr =
					value.getId() == null ? "" : String.valueOf(value.getId().longValue());

			if (equalsTrimmed(fileUrl, params.get("social_credit_code_url"))) {
				socialCreditCodeId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("legal_certId_front_url"))) {
				legalCertIdFrontId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("legal_cert_id_back_url"))) {
				legalCertIdBackId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("account_opening_permit_url"))) {
				accountOpeningPermitId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("store_url"))) {
				storeId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("transaction_test_record_url"))) {
				transactionTestRecordId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("web_pic_url"))) {
				webPicId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("lease_contract_url"))) {
				leaseContractId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("settle_account_certificate_url"))) {
				settleAccountCertificateId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("buss_support_materials_url"))) {
				bussSupportMaterialsId = idStr;
			} else if (equalsTrimmed(fileUrl, params.get("icp_registration_license_url"))) {
				icpRegistrationLicenseId = idStr;
			} else if (equalsTrimmed(fileUrl, params.get("industry_qualify_doc_license_url"))) {
				industryQualifyDocLicenseId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("cert_back_image_url"))) {
				certBackImageId = picId;
			} else if (equalsTrimmed(fileUrl, params.get("cert_front_image_url"))) {
				certFrontImageId = picId;
			}
		}

		if (!StringUtils.hasText(legalCertIdFrontId)
				|| !StringUtils.hasText(legalCertIdBackId)
				|| !StringUtils.hasText(accountOpeningPermitId)) {
			throw new BadRequestException("证照图片未匹配，请确认已上传且 URL 与上传返回一致");
		}

		String subApiKey =
				entry == null || !StringUtils.hasText(entry.getLiveApiKey())
						? ""
						: entry.getLiveApiKey();

		Map<String, Object> data = new HashMap<>();
		data.put("subApiKey", subApiKey);
		data.put("socialCreditCodeId", socialCreditCodeId);
		data.put("legalCertIdFrontId", legalCertIdFrontId);
		data.put("legalCertIdBackId", legalCertIdBackId);
		data.put("accountOpeningPermitId", accountOpeningPermitId);
		data.put("businessAdd", nullToEmpty(params.get("business_add")));
		data.put("storeId", storeId);
		data.put("transactionTestRecordId", transactionTestRecordId);
		data.put("webPicId", webPicId);
		data.put("leaseContractId", leaseContractId);
		data.put("settleAccountCertificateId", settleAccountCertificateId);
		data.put("bussSupportMaterialsId", bussSupportMaterialsId);
		data.put("icpRegistrationLicenseId", icpRegistrationLicenseId);
		data.put("industryQualifyDocType", nullToEmpty(params.get("industry_qualify_doc_type")));
		data.put("industryQualifyDocLicenseId", industryQualifyDocLicenseId);

		if (StringUtils.hasText(params.get("cert_id"))) {
			Map<String, Object> sh = new HashMap<>();
			sh.put("certBackImageId", nullToEmpty(certBackImageId));
			sh.put("certFrontImageId", nullToEmpty(certFrontImageId));
			sh.put("certId", nullToEmpty(params.get("cert_id")));
			sh.put("certName", nullToEmpty(params.get("cert_name")));
			List<Map<String, Object>> shareholderInfoList = new ArrayList<>();
			shareholderInfoList.add(sh);
			data.put("shareholderInfoList", shareholderInfoList);
		}

		applyTopLevelFalsyFilter(data);

		data.put("company_id", companyId);
		data.put("api_method", "MerchantProfile.merProfileForAudit");

		adapayPaymentSettingRedisReader.getPaymentSetting(companyId);

		throw new ResourceException("暂不支持开户流程");
	}

	private static void validateActionParams(Map<String, String> params) {
		if (!StringUtils.hasText(params.get("legal_certId_front_url"))) {
			throw new BadRequestException("法人身份证正面必传");
		}
		if (!StringUtils.hasText(params.get("legal_cert_id_back_url"))) {
			throw new BadRequestException("法人身份证反面必传");
		}
		if (!StringUtils.hasText(params.get("account_opening_permit_url"))) {
			throw new BadRequestException("开户许可证必传");
		}
		if (!StringUtils.hasText(params.get("is_sms"))) {
			throw new BadRequestException("是否短信提醒必传");
		}
		if (StringUtils.hasText(params.get("cert_id"))
				&& !LEGAL_CERT_PATTERN.matcher(params.get("cert_id").trim()).matches()) {
			throw new BadRequestException("股东身份证号格式错误");
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static boolean equalsTrimmed(String a, String b) {
		String ta = a == null ? "" : a.trim();
		String tb = b == null ? "" : b.trim();
		return Objects.equals(ta, tb);
	}

	private static void applyTopLevelFalsyFilter(Map<String, Object> data) {
		Iterator<Map.Entry<String, Object>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			if (shouldRemoveTopLevelValue(e.getValue())) {
				it.remove();
			}
		}
	}

	private static boolean shouldRemoveTopLevelValue(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		if (v instanceof Byte || v instanceof Short || v instanceof Integer || v instanceof Long) {
			return ((Number) v).longValue() == 0L;
		}
		if (v instanceof Float f) {
			return f == 0.0f;
		}
		if (v instanceof Double d) {
			return d == 0.0;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}
}
