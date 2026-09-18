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

import cn.shopex.ecshopx.adapay.domain.AdapaySubmitLicense;
import cn.shopex.ecshopx.adapay.domain.AdapayUploadLicense;
import cn.shopex.ecshopx.adapay.mapper.AdapaySubmitLicenseMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayUploadLicenseMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AdapaySubmitLicenseInfoService {

	private static final List<String> SUBMIT_LICENSE_COLUMN_ORDER =
			List.of(
					"id",
					"company_id",
					"sub_api_key",
					"social_credit_code_id",
					"legal_certId_front_id",
					"legal_cert_id_back_id",
					"account_opening_permit_id",
					"business_add",
					"store_id",
					"transaction_test_record_id",
					"web_pic_id",
					"lease_contract_id",
					"settle_account_certificate_id",
					"buss_support_materials_id",
					"icp_registration_license_id",
					"industry_qualify_doc_type",
					"industry_qualify_doc_license_id",
					"shareholder_info_list",
					"is_sms",
					"audit_status",
					"audit_desc",
					"create_time",
					"update_time");

	private static final List<String> SUBMIT_LICENSE_PIC_RESOLVE_ORDER =
			List.of(
					"social_credit_code_id",
					"legal_certId_front_id",
					"legal_cert_id_back_id",
					"account_opening_permit_id",
					"store_id",
					"transaction_test_record_id",
					"web_pic_id",
					"lease_contract_id",
					"settle_account_certificate_id",
					"buss_support_materials_id",
					"icp_registration_license_id",
					"industry_qualify_doc_license_id");

	private static final Set<String> SKIP_KEYS =
			Set.of(
					"id",
					"company_id",
					"sub_api_key",
					"business_add",
					"industry_qualify_doc_type",
					"audit_status",
					"audit_desc",
					"create_time",
					"update_time");

	private final AdapaySubmitLicenseMapper adapaySubmitLicenseMapper;
	private final AdapayUploadLicenseMapper adapayUploadLicenseMapper;
	private final FileStorageService fileStorageService;
	private final ObjectMapper objectMapper;

	public AdapaySubmitLicenseInfoService(
			AdapaySubmitLicenseMapper adapaySubmitLicenseMapper,
			AdapayUploadLicenseMapper adapayUploadLicenseMapper,
			FileStorageService fileStorageService,
			ObjectMapper objectMapper) {
		this.adapaySubmitLicenseMapper = adapaySubmitLicenseMapper;
		this.adapayUploadLicenseMapper = adapayUploadLicenseMapper;
		this.fileStorageService = fileStorageService;
		this.objectMapper = objectMapper;
	}

	public Object submitLicenseInfo(long companyId) {
		AdapaySubmitLicense entity =
				adapaySubmitLicenseMapper.selectOne(
						new LambdaQueryWrapper<AdapaySubmitLicense>()
								.eq(AdapaySubmitLicense::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (entity == null) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, Object> submitInfo = new LinkedHashMap<>();
		putBaseColumns(entity, submitInfo);

		LinkedHashMap<String, String> fileDir = new LinkedHashMap<>();

		for (String key : SUBMIT_LICENSE_COLUMN_ORDER) {
			if (SKIP_KEYS.contains(key)) {
				continue;
			}
			if ("shareholder_info_list".equals(key)) {
				Object raw = submitInfo.get("shareholder_info_list");
				if (raw == null || !(raw instanceof String) || !truthyString((String) raw)) {
					continue;
				}
				String json = ((String) raw).trim();
				JsonNode root;
				try {
					root = objectMapper.readTree(json);
				} catch (JsonProcessingException e) {
					throw new BadRequestException("股东信息格式错误");
				}
				if (!root.isArray() || root.size() == 0) {
					continue;
				}
				JsonNode first = root.get(0);
				submitInfo.put("cert_id", textOrNull(first, "certId"));
				submitInfo.put("cert_name", textOrNull(first, "certName"));

				String backId = textOrNull(first, "certBackImageId");
				if (truthyString(backId)) {
					AdapayUploadLicense certBack = selectUpload(companyId, backId);
					if (certBack != null && truthyString(certBack.getFileUrl())) {
						String path = certBack.getFileUrl().trim();
						submitInfo.put(
								"cert_back_image_url",
								fileStorageService.privateDownloadUrl("file", path, 3600));
						fileDir.put("cert_back_image_url", path);
					}
				}

				String frontId = textOrNull(first, "certFrontImageId");
				if (truthyString(frontId)) {
					AdapayUploadLicense certFront = selectUpload(companyId, frontId);
					if (certFront != null && truthyString(certFront.getFileUrl())) {
						String path = certFront.getFileUrl().trim();
						submitInfo.put(
								"cert_front_image_url",
								fileStorageService.privateDownloadUrl("file", path, 3600));
						fileDir.put("cert_front_image_url", path);
					}
				}
				continue;
			}
		}

		for (String picKey : SUBMIT_LICENSE_PIC_RESOLVE_ORDER) {
			resolvePic(companyId, submitInfo, fileDir, picKey);
		}
		if (!fileDir.isEmpty()) {
			submitInfo.put("file_dir", fileDir);
		}
		return submitInfo;
	}

	private void putBaseColumns(AdapaySubmitLicense entity, LinkedHashMap<String, Object> submitInfo) {
		for (String snakeKey : SUBMIT_LICENSE_COLUMN_ORDER) {
			switch (snakeKey) {
				case "id" -> submitInfo.put("id", entity.getId());
				case "company_id" -> submitInfo.put("company_id", entity.getCompanyId());
				case "sub_api_key" -> submitInfo.put("sub_api_key", entity.getSubApiKey());
				case "social_credit_code_id" ->
						submitInfo.put("social_credit_code_id", entity.getSocialCreditCodeId());
				case "legal_certId_front_id" ->
						submitInfo.put("legal_certId_front_id", entity.getLegalCertIdFrontId());
				case "legal_cert_id_back_id" ->
						submitInfo.put("legal_cert_id_back_id", entity.getLegalCertIdBackId());
				case "account_opening_permit_id" ->
						submitInfo.put("account_opening_permit_id", entity.getAccountOpeningPermitId());
				case "business_add" -> submitInfo.put("business_add", entity.getBusinessAdd());
				case "store_id" -> submitInfo.put("store_id", entity.getStoreId());
				case "transaction_test_record_id" ->
						submitInfo.put("transaction_test_record_id", entity.getTransactionTestRecordId());
				case "web_pic_id" -> submitInfo.put("web_pic_id", entity.getWebPicId());
				case "lease_contract_id" -> submitInfo.put("lease_contract_id", entity.getLeaseContractId());
				case "settle_account_certificate_id" ->
						submitInfo.put("settle_account_certificate_id", entity.getSettleAccountCertificateId());
				case "buss_support_materials_id" ->
						submitInfo.put("buss_support_materials_id", entity.getBussSupportMaterialsId());
				case "icp_registration_license_id" ->
						submitInfo.put("icp_registration_license_id", entity.getIcpRegistrationLicenseId());
				case "industry_qualify_doc_type" ->
						submitInfo.put("industry_qualify_doc_type", entity.getIndustryQualifyDocType());
				case "industry_qualify_doc_license_id" ->
						submitInfo.put(
								"industry_qualify_doc_license_id", entity.getIndustryQualifyDocLicenseId());
				case "shareholder_info_list" ->
						submitInfo.put("shareholder_info_list", entity.getShareholderInfoList());
				case "is_sms" -> submitInfo.put("is_sms", entity.getIsSms());
				case "audit_status" -> submitInfo.put("audit_status", entity.getAuditStatus());
				case "audit_desc" -> submitInfo.put("audit_desc", entity.getAuditDesc());
				case "create_time" -> submitInfo.put("create_time", entity.getCreateTime());
				case "update_time" -> submitInfo.put("update_time", entity.getUpdateTime());
				default -> throw new IllegalStateException("Unknown column key: " + snakeKey);
			}
		}
	}

	private void resolvePic(
			long companyId,
			LinkedHashMap<String, Object> submitInfo,
			LinkedHashMap<String, String> fileDir,
			String key) {
		Object vObj = submitInfo.get(key);
		if (vObj == null) {
			return;
		}
		String val;
		if (vObj instanceof Number n) {
			val = String.valueOf(n.longValue());
		} else if (vObj instanceof Boolean b) {
			val = String.valueOf(b);
		} else if (vObj instanceof String s) {
			val = s;
		} else {
			val = String.valueOf(vObj);
		}
		if (!truthyString(val)) {
			return;
		}
		AdapayUploadLicense uploadInfo = selectUpload(companyId, val);
		if (uploadInfo == null) {
			return;
		}
		String fileType = uploadInfo.getFileType();
		if (fileType == null) {
			fileType = "";
		}
		fileType = fileType.trim();
		switch (fileType) {
			case "01":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "social_credit_code_url");
				break;
			case "02":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "legal_certId_front_url");
				break;
			case "03":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "legal_cert_id_back_url");
				break;
			case "04":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "store_url");
				break;
			case "05":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "account_opening_permit_url");
				break;
			case "08":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "settle_account_certificate_url");
				break;
			case "09":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "web_pic_url");
				break;
			case "10":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "industry_qualify_doc_license_url");
				break;
			case "11":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "icp_registration_license_url");
				break;
			case "12":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "lease_contract_url");
				break;
			case "13":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "transaction_test_record_url");
				break;
			case "14":
				putResolvedUrl(submitInfo, fileDir, uploadInfo, "buss_support_materials_url");
				break;
			case "06":
			case "07":
				break;
		}
	}

	private void putResolvedUrl(
			LinkedHashMap<String, Object> submitInfo,
			LinkedHashMap<String, String> fileDir,
			AdapayUploadLicense uploadInfo,
			String urlKey) {
		if (!truthyString(uploadInfo.getFileUrl())) {
			return;
		}
		String trimmed = uploadInfo.getFileUrl().trim();
		submitInfo.put(urlKey, fileStorageService.privateDownloadUrl("file", trimmed, 3600));
		fileDir.put(urlKey, trimmed);
	}

	private AdapayUploadLicense selectUpload(long companyId, String picId) {
		return adapayUploadLicenseMapper.selectOne(
				new LambdaQueryWrapper<AdapayUploadLicense>()
						.eq(AdapayUploadLicense::getCompanyId, companyId)
						.eq(AdapayUploadLicense::getPicId, picId)
						.last("LIMIT 1"));
	}

	/** Pic id / URL string checks: null, blank after trim, and {@code "0"} are false. */
	private static boolean truthyString(String value) {
		if (value == null) {
			return false;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return false;
		}
		return !"0".equals(trimmed);
	}

	private static String textOrNull(JsonNode node, String fieldName) {
		if (node == null || !node.has(fieldName)) {
			return null;
		}
		JsonNode n = node.get(fieldName);
		if (n == null || n.isNull()) {
			return null;
		}
		if (n.isTextual()) {
			String t = n.asText().trim();
			return t.isEmpty() ? null : t;
		}
		if (n.isNumber()) {
			return n.asText();
		}
		return null;
	}
}
