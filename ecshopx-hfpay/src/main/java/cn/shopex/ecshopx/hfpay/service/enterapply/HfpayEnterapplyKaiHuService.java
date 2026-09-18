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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayEnterapplyKaiHuService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfpayEnterapplyReadService readService;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayOrderApplyIdGenerator orderApplyIdGenerator;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final HfpayEnterapplyUpdateApplyService updateApplyService;
	private final String bgRetUrl;

	public HfpayEnterapplyKaiHuService(
			HfpayEnterapplyReadService readService,
			HfPayPaymentSettingService paymentSettingService,
			HfPayOrderApplyIdGenerator orderApplyIdGenerator,
			HfPayAcouJsonPostClient acouJsonPostClient,
			HfpayEnterapplyUpdateApplyService updateApplyService,
			@Value("${ecshopx.hfpay.bg-ret-url:}") String bgRetUrl) {
		this.readService = readService;
		this.paymentSettingService = paymentSettingService;
		this.orderApplyIdGenerator = orderApplyIdGenerator;
		this.acouJsonPostClient = acouJsonPostClient;
		this.updateApplyService = updateApplyService;
		this.bgRetUrl = bgRetUrl == null ? "" : bgRetUrl;
	}

	public Map<String, Object> kaiHu(long companyId, Map<String, Object> merged) {
		long distributorId = parseDistributorId(merged.get("distributor_id"));
		Map<String, Object> data = readService.getEnterapply(companyId, distributorId);
		if (data == null || data.isEmpty()) {
			throw new ResourceException("请先提交资质信息");
		}
		String status = str(data.get("status")).trim();
		if ("2".equals(status) || "3".equals(status)) {
			throw new ResourceException("请勿重复开户");
		}
		String hfOrderId = str(data.get("hf_order_id")).trim();
		data.put("operate_type", hfOrderId.isEmpty() ? "A" : "M");

		Map<String, Object> setting = paymentSettingService.loadForCompany(companyId);
		String applyType = str(data.get("apply_type")).trim();

		Map<String, Object> apiResult;
		switch (applyType) {
			case "1" -> {
				String attachNos = buildCorpAttachNos(data);
				apiResult = acouJsonPostClient.corp01(setting, buildCorpPayload(data, setting, attachNos));
			}
			case "2" -> {
				String attachNos = buildSoloAttachNos(data);
				apiResult = acouJsonPostClient.solo01(setting, buildSoloPayload(data, setting, attachNos));
			}
			case "3" -> {
				data.put("card_num", str(data.get("bank_acct_num")));
				apiResult = acouJsonPostClient.bind01(setting, buildBindPayload(data, setting));
			}
			default -> throw new ResourceException("该类型暂不支持开户");
		}

		String respCode = str(apiResult.get("resp_code")).trim();
		if (!"C00000".equals(respCode) && !"C00001".equals(respCode) && !"C00002".equals(respCode)) {
			Object desc = apiResult.get("resp_desc");
			throw new ResourceException(desc == null ? "" : String.valueOf(desc));
		}

		LinkedHashMap<String, Object> editData = new LinkedHashMap<>();
		boolean hasOrderId = apiResult.containsKey("order_id") && apiResult.get("order_id") != null;
		boolean hasOrderDate = apiResult.containsKey("order_date") && apiResult.get("order_date") != null;
		if (hasOrderId || hasOrderDate) {
			editData.put("hf_order_id", apiResult.get("order_id"));
			editData.put("hf_order_date", apiResult.get("order_date"));
			Object applyId = apiResult.get("apply_id");
			editData.put("hf_apply_id", applyId == null ? "" : String.valueOf(applyId));
		}
		if ("C00001".equals(respCode) || "C00002".equals(respCode)) {
			editData.put("status", "2");
		}
		if ("C00000".equals(respCode)) {
			editData.put("user_cust_id", apiResult.get("user_cust_id"));
			editData.put("acct_id", apiResult.get("acct_id"));
			editData.put("status", "3");
		}

		LinkedHashMap<String, Object> editFilter = new LinkedHashMap<>();
		editFilter.put("hfpay_enterapply_id", data.get("hfpay_enterapply_id"));
		editFilter.put("company_id", companyId);
		return updateApplyService.updateApply(editFilter, editData);
	}

	private static long parseDistributorId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("缺少 distributor_id");
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("缺少 distributor_id");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 格式错误");
		}
	}

	private String buildCorpAttachNos(Map<String, Object> data) {
		StringBuilder sb = new StringBuilder();
		sb.append(str(data.get("legal_card_imgz")))
				.append(",")
				.append(str(data.get("legal_card_imgf")))
				.append(",")
				.append(str(data.get("bank_acct_img")));
		String licenseType = str(data.get("corp_license_type")).trim();
		switch (licenseType) {
			case "1" -> sb.append(",")
					.append(str(data.get("business_code_img")))
					.append(",")
					.append(str(data.get("institution_code_img")))
					.append(",")
					.append(str(data.get("tax_code_img")));
			case "2" -> sb.append(",").append(str(data.get("social_credit_code_img")));
			default -> {
			}
		}
		return sb.toString();
	}

	private static String buildSoloAttachNos(Map<String, Object> data) {
		return str(data.get("business_code_img"))
				+ ","
				+ str(data.get("legal_card_imgz"))
				+ ","
				+ str(data.get("legal_card_imgf"))
				+ ","
				+ str(data.get("bank_acct_num_imgz"))
				+ ","
				+ str(data.get("bank_acct_num_imgz"));
	}

	private LinkedHashMap<String, Object> buildCorpPayload(
			Map<String, Object> data,
			Map<String, Object> setting,
			String attachNos) {
		String orderDate = LocalDate.now(ZoneId.systemDefault()).format(DAY);
		String orderId = orderApplyIdGenerator.nextOrderId();
		String hfApplyStored = str(data.get("hf_apply_id")).trim();
		String applyId = StringUtils.hasText(hfApplyStored) ? hfApplyStored : orderApplyIdGenerator.nextApplyId();

		LinkedHashMap<String, Object> p = new LinkedHashMap<>();
		p.put("version", "10");
		p.put("mer_cust_id", str(setting.get("mer_cust_id")).trim());
		p.put("order_date", orderDate);
		p.put("order_id", orderId);
		p.put("apply_id", applyId);
		p.put("operate_type", str(data.get("operate_type")));
		p.put("corp_license_type", str(data.get("corp_license_type")));
		p.put("corp_name", str(data.get("corp_name")));
		p.put("controlling_shareholder", str(data.get("controlling_shareholder")));
		p.put("legal_name", str(data.get("legal_name")));
		p.put("legal_id_card_type", str(data.get("legal_id_card_type")));
		p.put("legal_id_card", str(data.get("legal_id_card")));
		p.put("legal_cert_start_date", str(data.get("legal_cert_start_date")));
		p.put("legal_cert_end_date", str(data.get("legal_cert_end_date")));
		p.put("legal_mobile", str(data.get("legal_mobile")));
		p.put("contact_name", str(data.get("contact_name")));
		p.put("contact_mobile", str(data.get("contact_mobile")));
		p.put("contact_email", str(data.get("contact_email")));
		p.put("bank_acct_name", str(data.get("bank_acct_name")));
		p.put("bank_id", str(data.get("bank_id")));
		p.put("bank_acct_num", str(data.get("bank_acct_num")));
		p.put("bank_prov", str(data.get("bank_prov")));
		p.put("bank_area", str(data.get("bank_area")));
		p.put("bank_branch", str(data.get("bank_branch")));
		p.put("bg_ret_url", bgRetUrl);
		p.put("attach_nos", attachNos);
		p.put("mer_priv", "corp01");

		String licenseType = str(data.get("corp_license_type")).trim();
		switch (licenseType) {
			case "1" -> {
				p.put("business_code", str(data.get("business_code")));
				p.put("institution_code", str(data.get("institution_code")));
				p.put("tax_code", str(data.get("tax_code")));
			}
			case "2" -> p.put("social_credit_code", str(data.get("social_credit_code")));
			default -> throw new ResourceException("未知的企业证照类型");
		}
		return p;
	}

	private LinkedHashMap<String, Object> buildSoloPayload(
			Map<String, Object> data,
			Map<String, Object> setting,
			String attachNos) {
		String orderDate = LocalDate.now(ZoneId.systemDefault()).format(DAY);
		LinkedHashMap<String, Object> p = new LinkedHashMap<>();
		p.put("version", 10);
		p.put("mer_cust_id", str(setting.get("mer_cust_id")).trim());
		p.put("order_date", orderDate);
		p.put("order_id", orderApplyIdGenerator.nextOrderId());
		p.put("apply_id", orderApplyIdGenerator.nextApplyId());
		p.put("operate_type", str(data.get("operate_type")));
		p.put("solo_name", str(data.get("solo_name")));
		p.put("business_code", str(data.get("business_code")));
		p.put("license_start_date", str(data.get("license_start_date")));
		p.put("license_end_date", str(data.get("license_end_date")));
		p.put("solo_business_address", str(data.get("solo_business_address")));
		p.put("solo_reg_address", str(data.get("solo_reg_address")));
		p.put("solo_fixed_telephone", str(data.get("solo_fixed_telephone")));
		p.put("business_scope", str(data.get("business_scope")));
		p.put("legal_name", str(data.get("legal_name")));
		p.put("legal_id_card_type", str(data.get("legal_id_card_type")));
		p.put("legal_id_card", str(data.get("legal_id_card")));
		p.put("legal_cert_start_date", str(data.get("legal_cert_start_date")));
		p.put("legal_cert_end_date", str(data.get("legal_cert_end_date")));
		p.put("legal_mobile", str(data.get("legal_mobile")));
		p.put("contact_name", str(data.get("contact_name")));
		p.put("contact_mobile", str(data.get("contact_mobile")));
		p.put("contact_email", str(data.get("contact_email")));
		p.put("occupation", str(data.get("occupation")));
		p.put("open_license_no", str(data.get("open_license_no")));
		p.put("contact_cert_num", str(data.get("contact_cert_num")));
		p.put("bg_ret_url", bgRetUrl);
		p.put("attach_nos", attachNos);
		p.put("mer_priv", "solo01");
		return p;
	}

	private LinkedHashMap<String, Object> buildBindPayload(Map<String, Object> data, Map<String, Object> setting) {
		String orderDate = LocalDate.now(ZoneId.systemDefault()).format(DAY);
		LinkedHashMap<String, Object> p = new LinkedHashMap<>();
		p.put("version", "10");
		p.put("mer_cust_id", str(setting.get("mer_cust_id")).trim());
		p.put("order_date", orderDate);
		p.put("order_id", orderApplyIdGenerator.nextOrderId());
		p.put("card_num", str(data.get("card_num")));
		String cardType = str(data.get("card_type"));
		if (!StringUtils.hasText(cardType)) {
			cardType = "1";
		}
		p.put("card_type", cardType);
		switch (cardType) {
			case "0" -> {
				p.put("user_cust_id", str(data.get("user_cust_id")));
				p.put("bank_id", str(data.get("bank_id")));
			}
			case "1" -> p.put("user_cust_id", str(data.get("user_cust_id")));
			default -> throw new ResourceException("未知的绑卡类型");
		}
		if (!StringUtils.hasText(str(p.get("user_cust_id")))) {
			p.put("user_name", str(data.get("user_name")));
			p.put("id_card", str(data.get("id_card")));
			p.put("user_mobile", str(data.get("user_mobile")));
		}
		return p;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
