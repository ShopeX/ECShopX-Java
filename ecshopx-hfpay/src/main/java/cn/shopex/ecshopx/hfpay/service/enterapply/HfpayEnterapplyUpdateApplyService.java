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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayEnterapplyUpdateApplyService {

	private final HfpayEnterapplyMapper hfpayEnterapplyMapper;

	public HfpayEnterapplyUpdateApplyService(HfpayEnterapplyMapper hfpayEnterapplyMapper) {
		this.hfpayEnterapplyMapper = hfpayEnterapplyMapper;
	}

	public Map<String, Object> updateApply(Map<String, Object> filter, Map<String, Object> editData) {
		Object idRaw = filter.get("hfpay_enterapply_id");
		Object companyRaw = filter.get("company_id");
		if (idRaw == null || companyRaw == null) {
			throw new ResourceException("未查询到更新数据");
		}
		long enterapplyId = toLong(idRaw, "hfpay_enterapply_id");
		long companyId = toLong(companyRaw, "company_id");

		HfpayEnterapply entity = hfpayEnterapplyMapper.selectOne(new LambdaQueryWrapper<HfpayEnterapply>()
				.eq(HfpayEnterapply::getHfpayEnterapplyId, enterapplyId)
				.eq(HfpayEnterapply::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}

		applyColumnData(entity, editData);
		hfpayEnterapplyMapper.updateById(entity);

		HfpayEnterapply updated = hfpayEnterapplyMapper.selectById(enterapplyId);
		if (updated == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return HfpayEnterapplyRowConverter.columnNamesData(updated);
	}

	private static long toLong(Object raw, String field) {
		if (raw instanceof Number) {
			return ((Number) raw).longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("未查询到更新数据");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public static void applyColumnData(HfpayEnterapply e, Map<String, Object> editData) {
		for (Map.Entry<String, Object> en : editData.entrySet()) {
			if (en.getValue() == null) {
				continue;
			}
			String k = en.getKey();
			Object v = en.getValue();
			switch (k) {
				case "hfpay_enterapply_id" -> e.setHfpayEnterapplyId(toLongBox(v));
				case "company_id" -> e.setCompanyId(toLongBox(v));
				case "distributor_id" -> e.setDistributorId(toLongBox(v));
				case "user_id" -> e.setUserId(toLongBox(v));
				case "user_cust_id" -> e.setUserCustId(str(v));
				case "acct_id" -> e.setAcctId(str(v));
				case "apply_type" -> e.setApplyType(str(v));
				case "corp_license_type" -> e.setCorpLicenseType(str(v));
				case "corp_name" -> e.setCorpName(str(v));
				case "business_code" -> e.setBusinessCode(str(v));
				case "institution_code" -> e.setInstitutionCode(str(v));
				case "tax_code" -> e.setTaxCode(str(v));
				case "social_credit_code" -> e.setSocialCreditCode(str(v));
				case "license_start_date" -> e.setLicenseStartDate(str(v));
				case "license_end_date" -> e.setLicenseEndDate(str(v));
				case "controlling_shareholder" -> e.setControllingShareholder(str(v));
				case "legal_name" -> e.setLegalName(str(v));
				case "legal_id_card_type" -> e.setLegalIdCardType(str(v));
				case "legal_id_card" -> e.setLegalIdCard(str(v));
				case "legal_cert_start_date" -> e.setLegalCertStartDate(str(v));
				case "legal_cert_end_date" -> e.setLegalCertEndDate(str(v));
				case "legal_mobile" -> e.setLegalMobile(str(v));
				case "contact_name" -> e.setContactName(str(v));
				case "contact_mobile" -> e.setContactMobile(str(v));
				case "contact_email" -> e.setContactEmail(str(v));
				case "bank_acct_name" -> e.setBankAcctName(str(v));
				case "bank_id" -> e.setBankId(str(v));
				case "bank_name" -> e.setBankName(str(v));
				case "bank_acct_num" -> e.setBankAcctNum(str(v));
				case "bank_prov" -> e.setBankProv(str(v));
				case "bank_prov_name" -> e.setBankProvName(str(v));
				case "bank_area" -> e.setBankArea(str(v));
				case "bank_area_name" -> e.setBankAreaName(str(v));
				case "solo_name" -> e.setSoloName(str(v));
				case "solo_business_address" -> e.setSoloBusinessAddress(str(v));
				case "solo_reg_address" -> e.setSoloRegAddress(str(v));
				case "solo_fixed_telephone" -> e.setSoloFixedTelephone(str(v));
				case "business_scope" -> e.setBusinessScope(str(v));
				case "occupation" -> e.setOccupation(str(v));
				case "user_name" -> e.setUserName(str(v));
				case "id_card_type" -> e.setIdCardType(str(v));
				case "id_card" -> e.setIdCard(str(v));
				case "user_mobile" -> e.setUserMobile(str(v));
				case "hf_order_id" -> e.setHfOrderId(str(v));
				case "hf_order_date" -> e.setHfOrderDate(str(v));
				case "hf_apply_id" -> e.setHfApplyId(str(v));
				case "status" -> e.setStatus(str(v));
				case "business_code_img" -> e.setBusinessCodeImg(str(v));
				case "business_code_img_local" -> e.setBusinessCodeImgLocal(str(v));
				case "institution_code_img" -> e.setInstitutionCodeImg(str(v));
				case "institution_code_img_local" -> e.setInstitutionCodeImgLocal(str(v));
				case "tax_code_img" -> e.setTaxCodeImg(str(v));
				case "tax_code_img_local" -> e.setTaxCodeImgLocal(str(v));
				case "social_credit_code_img" -> e.setSocialCreditCodeImg(str(v));
				case "social_credit_code_img_local" -> e.setSocialCreditCodeImgLocal(str(v));
				case "legal_card_imgz" -> e.setLegalCardImgz(str(v));
				case "legal_card_imgz_local" -> e.setLegalCardImgzLocal(str(v));
				case "legal_card_imgf" -> e.setLegalCardImgf(str(v));
				case "legal_card_imgf_local" -> e.setLegalCardImgfLocal(str(v));
				case "bank_acct_img" -> e.setBankAcctImg(str(v));
				case "bank_acct_img_local" -> e.setBankAcctImgLocal(str(v));
				case "resp_code" -> e.setRespCode(str(v));
				case "resp_desc" -> e.setRespDesc(str(v));
				case "bank_branch" -> e.setBankBranch(str(v));
				case "bank_acct_num_imgz" -> e.setBankAcctNumImgz(str(v));
				case "bank_acct_num_imgf" -> e.setBankAcctNumImgf(str(v));
				case "bank_acct_num_imgz_local" -> e.setBankAcctNumImgzLocal(str(v));
				case "bank_acct_num_imgf_local" -> e.setBankAcctNumImgfLocal(str(v));
				case "contact_cert_num" -> e.setContactCertNum(str(v));
				case "open_license_no" -> e.setOpenLicenseNo(str(v));
				default -> {
					// ignore unknown keys (e.g. derived *_full_url from read path)
				}
			}
		}
	}

	private static String str(Object v) {
		return String.valueOf(v);
	}

	private static Long toLongBox(Object v) {
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		return Long.parseLong(s);
	}
}
