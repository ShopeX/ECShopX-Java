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

package cn.shopex.ecshopx.hfpay.service.enterapply;

import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Converts {@link HfpayEnterapply} entities to snake_case column maps for persistence and API payloads.
 * {@link #enterapplyDetail} adds absolute image URLs from a configured public base and expands controlling
 * shareholder JSON into flat fields.
 */
public final class HfpayEnterapplyRowConverter {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private HfpayEnterapplyRowConverter() {
	}

	public static Map<String, Object> columnNamesData(HfpayEnterapply e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("hfpay_enterapply_id", e.getHfpayEnterapplyId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("user_id", e.getUserId());
		m.put("user_cust_id", e.getUserCustId());
		m.put("acct_id", e.getAcctId());
		m.put("apply_type", e.getApplyType());
		m.put("corp_license_type", e.getCorpLicenseType());
		m.put("corp_name", e.getCorpName());
		m.put("business_code", e.getBusinessCode());
		m.put("institution_code", e.getInstitutionCode());
		m.put("tax_code", e.getTaxCode());
		m.put("social_credit_code", e.getSocialCreditCode());
		m.put("license_start_date", e.getLicenseStartDate());
		m.put("license_end_date", e.getLicenseEndDate());
		m.put("controlling_shareholder", e.getControllingShareholder());
		m.put("legal_name", e.getLegalName());
		m.put("legal_id_card_type", e.getLegalIdCardType());
		m.put("legal_id_card", e.getLegalIdCard());
		m.put("legal_cert_start_date", e.getLegalCertStartDate());
		m.put("legal_cert_end_date", e.getLegalCertEndDate());
		m.put("legal_mobile", e.getLegalMobile());
		m.put("contact_name", e.getContactName());
		m.put("contact_mobile", e.getContactMobile());
		m.put("contact_email", e.getContactEmail());
		m.put("bank_acct_name", e.getBankAcctName());
		m.put("bank_id", e.getBankId());
		m.put("bank_name", e.getBankName());
		m.put("bank_acct_num", e.getBankAcctNum());
		m.put("bank_prov", e.getBankProv());
		m.put("bank_prov_name", e.getBankProvName());
		m.put("bank_area", e.getBankArea());
		m.put("bank_area_name", e.getBankAreaName());
		m.put("solo_name", e.getSoloName());
		m.put("solo_business_address", e.getSoloBusinessAddress());
		m.put("solo_reg_address", e.getSoloRegAddress());
		m.put("solo_fixed_telephone", e.getSoloFixedTelephone());
		m.put("business_scope", e.getBusinessScope());
		m.put("occupation", e.getOccupation());
		m.put("user_name", e.getUserName());
		m.put("id_card_type", e.getIdCardType());
		m.put("id_card", e.getIdCard());
		m.put("user_mobile", e.getUserMobile());
		m.put("hf_order_id", e.getHfOrderId());
		m.put("hf_order_date", e.getHfOrderDate());
		m.put("hf_apply_id", e.getHfApplyId());
		m.put("status", e.getStatus());
		m.put("business_code_img", e.getBusinessCodeImg());
		m.put("business_code_img_local", e.getBusinessCodeImgLocal());
		m.put("institution_code_img", e.getInstitutionCodeImg());
		m.put("institution_code_img_local", e.getInstitutionCodeImgLocal());
		m.put("tax_code_img", e.getTaxCodeImg());
		m.put("tax_code_img_local", e.getTaxCodeImgLocal());
		m.put("social_credit_code_img", e.getSocialCreditCodeImg());
		m.put("social_credit_code_img_local", e.getSocialCreditCodeImgLocal());
		m.put("legal_card_imgz", e.getLegalCardImgz());
		m.put("legal_card_imgz_local", e.getLegalCardImgzLocal());
		m.put("legal_card_imgf", e.getLegalCardImgf());
		m.put("legal_card_imgf_local", e.getLegalCardImgfLocal());
		m.put("bank_acct_img", e.getBankAcctImg());
		m.put("bank_acct_img_local", e.getBankAcctImgLocal());
		m.put("resp_code", e.getRespCode());
		m.put("resp_desc", e.getRespDesc());
		m.put("created_at", formatDateTime(e.getCreatedAt()));
		m.put("updated_at", formatDateTime(e.getUpdatedAt()));
		m.put("bank_branch", e.getBankBranch());
		m.put("bank_acct_num_imgz", e.getBankAcctNumImgz());
		m.put("bank_acct_num_imgf", e.getBankAcctNumImgf());
		m.put("bank_acct_num_imgz_local", e.getBankAcctNumImgzLocal());
		m.put("bank_acct_num_imgf_local", e.getBankAcctNumImgfLocal());
		m.put("contact_cert_num", e.getContactCertNum());
		m.put("open_license_no", e.getOpenLicenseNo());
		return m;
	}

	public static Map<String, Object> enterapplyDetail(
			HfpayEnterapply e,
			String importImagePublicBase,
			ObjectMapper objectMapper) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>(columnNamesData(e));
		String base = importImagePublicBase == null ? "" : importImagePublicBase.trim();
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		m.put("business_code_img_full_url", fullUrl(base, e.getBusinessCodeImgLocal()));
		m.put("institution_code_img_full_url", fullUrl(base, e.getInstitutionCodeImgLocal()));
		m.put("tax_code_img_full_url", fullUrl(base, e.getTaxCodeImgLocal()));
		m.put("social_credit_code_img_full_url", fullUrl(base, e.getSocialCreditCodeImgLocal()));
		m.put("legal_card_imgz_full_url", fullUrl(base, e.getLegalCardImgzLocal()));
		m.put("legal_card_imgf_full_url", fullUrl(base, e.getLegalCardImgfLocal()));
		m.put("bank_acct_img_full_url", fullUrl(base, e.getBankAcctImgLocal()));
		m.put("bank_acct_num_imgz_full_url", fullUrl(base, e.getBankAcctNumImgzLocal()));
		m.put("bank_acct_num_imgf_full_url", fullUrl(base, e.getBankAcctNumImgfLocal()));

		m.put("controlling_shareholder_cust_name", "");
		m.put("controlling_shareholder_id_card_type", "");
		m.put("controlling_shareholder_id_card", "");
		String rawCs = e.getControllingShareholder();
		if (StringUtils.hasText(rawCs)) {
			try {
				JsonNode arr = objectMapper.readTree(rawCs);
				if (arr != null && arr.isArray() && arr.size() > 0) {
					JsonNode first = arr.get(0);
					m.put("controlling_shareholder_cust_name", textNode(first, "custName"));
					m.put("controlling_shareholder_id_card_type", textNode(first, "idCardType"));
					m.put("controlling_shareholder_id_card", textNode(first, "idCard"));
				}
			} catch (Exception ignored) {
				// Malformed controlling_shareholder JSON: keep the three expanded fields as empty strings.
			}
		}
		return m;
	}

	private static String fullUrl(String base, String localPath) {
		if (!StringUtils.hasText(localPath)) {
			return "";
		}
		if (!StringUtils.hasText(base)) {
			return "";
		}
		String p = localPath.trim().replace('\\', '/');
		if (p.startsWith("/")) {
			p = p.substring(1);
		}
		return base + "/" + p;
	}

	private static String textNode(JsonNode node, String field) {
		if (node == null || !node.has(field) || node.get(field).isNull()) {
			return "";
		}
		return node.get(field).asText("");
	}

	private static Object formatDateTime(LocalDateTime t) {
		return t == null ? null : DATE_TIME.format(t);
	}
}
