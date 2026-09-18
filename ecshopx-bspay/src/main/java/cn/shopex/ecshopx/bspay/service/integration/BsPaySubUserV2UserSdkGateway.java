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

package cn.shopex.ecshopx.bspay.service.integration;

import cn.shopex.ecshopx.bspay.service.BsPayPaymentSettingService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import com.huifu.bspay.sdk.opps.core.request.BaseRequest;
import com.huifu.bspay.sdk.opps.core.request.V2UserBasicdataEntModifyRequest;
import com.huifu.bspay.sdk.opps.core.request.V2UserBasicdataEntRequest;
import com.huifu.bspay.sdk.opps.core.request.V2UserBasicdataIndvModifyRequest;
import com.huifu.bspay.sdk.opps.core.request.V2UserBasicdataIndvRequest;
import com.huifu.bspay.sdk.opps.core.request.V2UserBusiModifyRequest;
import com.huifu.bspay.sdk.opps.core.request.V2UserBusiOpenRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BsPaySubUserV2UserSdkGateway {

	private static final Object BSPAY_MUTEX = new Object();

	private final BsPayPaymentSettingService bsPayPaymentSettingService;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.bspay.prod-mode:true}")
	private boolean bspayProdMode;

	public BsPaySubUserV2UserSdkGateway(
			BsPayPaymentSettingService bsPayPaymentSettingService, ObjectMapper objectMapper) {
		this.bsPayPaymentSettingService = bsPayPaymentSettingService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> basicdataEnt(long companyId, Map<String, Object> data) throws BasePayException, IllegalAccessException {
		V2UserBasicdataEntRequest req = new V2UserBasicdataEntRequest();
		req.setReqSeqId(str(data.get("req_seq_id")));
		req.setReqDate(todayYmd());
		req.setRegName(str(data.get("reg_name")));
		req.setLicenseCode(str(data.get("license_code")));
		req.setLicenseValidityType(str(data.get("license_validity_type")));
		req.setLicenseBeginDate(str(data.get("license_begin_date")));
		req.setLicenseEndDate(str(data.get("license_end_date")));
		req.setRegProvId(str(data.get("reg_prov_id")));
		req.setRegAreaId(str(data.get("reg_area_id")));
		req.setRegDistrictId(str(data.get("reg_district_id")));
		req.setRegDetail(str(data.get("reg_detail")));
		req.setLegalName(str(data.get("legal_name")));
		req.setLegalCertType("00");
		req.setLegalCertNo(str(data.get("legal_cert_no")));
		req.setLegalCertValidityType(str(data.get("legal_cert_validity_type")));
		req.setLegalCertBeginDate(str(data.get("legal_cert_begin_date")));
		req.setLegalCertEndDate(str(data.get("legal_cert_end_date")));
		req.setContactName(str(data.get("contact_name")));
		req.setContactMobile(str(data.get("contact_mobile")));
		addSmsExtend(req);
		return post(companyId, req);
	}

	public Map<String, Object> basicdataEntModify(long companyId, Map<String, Object> data)
			throws BasePayException, IllegalAccessException {
		V2UserBasicdataEntModifyRequest req = new V2UserBasicdataEntModifyRequest();
		req.setReqSeqId(str(data.get("req_seq_id")));
		req.setReqDate(todayYmd());
		req.setHuifuId(str(data.get("huifu_id")));
		req.addExtendInfo("reg_name", str(data.get("reg_name")));
		req.addExtendInfo("license_code", str(data.get("license_code")));
		req.addExtendInfo("license_validity_type", str(data.get("license_validity_type")));
		req.addExtendInfo("license_begin_date", str(data.get("license_begin_date")));
		req.addExtendInfo("license_end_date", str(data.get("license_end_date")));
		req.addExtendInfo("reg_prov_id", str(data.get("reg_prov_id")));
		req.addExtendInfo("reg_area_id", str(data.get("reg_area_id")));
		req.addExtendInfo("reg_district_id", str(data.get("reg_district_id")));
		req.addExtendInfo("reg_detail", str(data.get("reg_detail")));
		req.addExtendInfo("legal_name", str(data.get("legal_name")));
		req.addExtendInfo("legal_cert_type", "00");
		req.addExtendInfo("legal_cert_no", str(data.get("legal_cert_no")));
		req.addExtendInfo("legal_cert_validity_type", str(data.get("legal_cert_validity_type")));
		req.addExtendInfo("legal_cert_begin_date", str(data.get("legal_cert_begin_date")));
		req.addExtendInfo("contact_name", str(data.get("contact_name")));
		req.addExtendInfo("contact_mobile", str(data.get("contact_mobile")));
		addSmsExtend(req);
		return post(companyId, req);
	}

	public Map<String, Object> basicdataIndv(long companyId, Map<String, Object> data)
			throws BasePayException, IllegalAccessException {
		V2UserBasicdataIndvRequest req = new V2UserBasicdataIndvRequest();
		req.setReqSeqId(str(data.get("req_seq_id")));
		req.setReqDate(todayYmd());
		req.setName(str(data.get("name")));
		req.setCertType("00");
		req.setCertNo(str(data.get("cert_no")));
		req.setCertValidityType(str(data.get("cert_validity_type")));
		req.setCertBeginDate(str(data.get("cert_begin_date")));
		req.addExtendInfo("cert_end_date", str(data.get("cert_end_date")));
		req.setMobileNo(str(data.get("mobile_no")));
		addSmsExtend(req);
		return post(companyId, req);
	}

	public Map<String, Object> basicdataIndvModify(long companyId, Map<String, Object> data)
			throws BasePayException, IllegalAccessException {
		V2UserBasicdataIndvModifyRequest req = new V2UserBasicdataIndvModifyRequest();
		req.setReqSeqId(str(data.get("req_seq_id")));
		req.setReqDate(todayYmd());
		req.setHuifuId(str(data.get("huifu_id")));
		req.addExtendInfo("name", str(data.get("name")));
		req.addExtendInfo("cert_type", "00");
		req.addExtendInfo("cert_no", str(data.get("cert_no")));
		req.addExtendInfo("cert_validity_type", str(data.get("cert_validity_type")));
		req.addExtendInfo("cert_begin_date", str(data.get("cert_begin_date")));
		req.addExtendInfo("cert_end_date", str(data.get("cert_end_date")));
		req.addExtendInfo("mobile_no", str(data.get("mobile_no")));
		addSmsExtend(req);
		return post(companyId, req);
	}

	public Map<String, Object> busiOpen(long companyId, Map<String, Object> data)
			throws BasePayException, IllegalAccessException {
		return busiCall(companyId, data, true);
	}

	public Map<String, Object> busiModify(long companyId, Map<String, Object> data)
			throws BasePayException, IllegalAccessException {
		return busiCall(companyId, data, false);
	}

	private Map<String, Object> busiCall(long companyId, Map<String, Object> data, boolean open)
			throws BasePayException, IllegalAccessException {
		BaseRequest req;
		if (open) {
			V2UserBusiOpenRequest r = new V2UserBusiOpenRequest();
			r.setReqSeqId(str(data.get("req_seq_id")));
			r.setReqDate(todayYmd());
			r.setUpperHuifuId(str(data.get("upper_huifu_id")));
			r.setHuifuId(str(data.get("huifu_id")));
			req = r;
		} else {
			V2UserBusiModifyRequest r = new V2UserBusiModifyRequest();
			r.setReqSeqId(str(data.get("req_seq_id")));
			r.setReqDate(todayYmd());
			r.setUpperHuifuId(str(data.get("upper_huifu_id")));
			r.setHuifuId(str(data.get("huifu_id")));
			req = r;
		}
		Map<String, Object> cardInfo = new LinkedHashMap<>();
		cardInfo.put("card_type", str(data.get("card_type")));
		cardInfo.put("card_name", str(data.get("card_name")));
		cardInfo.put("card_no", str(data.get("card_no")));
		cardInfo.put("prov_id", str(data.get("prov_id")));
		cardInfo.put("area_id", str(data.get("area_id")));
		cardInfo.put("bank_code", str(data.get("bank_code")));
		cardInfo.put("branch_name", str(data.get("branch_name")));
		cardInfo.put("cert_type", "00");
		cardInfo.put("cert_no", str(data.get("cert_no")));
		cardInfo.put("cert_validity_type", str(data.get("cert_validity_type")));
		cardInfo.put("cert_begin_date", str(data.get("cert_begin_date")));
		cardInfo.put("cert_end_date", str(data.get("cert_end_date")));
		cardInfo.put("mp", str(data.get("mp")));
		try {
			req.addExtendInfo("card_info", objectMapper.writeValueAsString(cardInfo));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("card_info json", e);
		}
		req.addExtendInfo("cash_config", buildCashConfigJson());
		return post(companyId, req);
	}

	private static void addSmsExtend(BaseRequest req) {
		req.addExtendInfo("sms_send_flag", "Y");
		req.addExtendInfo("expand_id", "");
	}

	private Map<String, Object> post(long companyId, BaseRequest request)
			throws BasePayException, IllegalAccessException {
		Map<String, Object> cfg = bsPayPaymentSettingService.requireSettingMap(companyId);
		String sysId = str(cfg.get("sys_id"));
		String productId = str(cfg.get("product_id"));
		String rsaMerchPrivate = str(cfg.get("rsa_merch_private_key"));
		String rsaHuifuPublic = str(cfg.get("rsa_huifu_public_key"));
		if (!StringUtils.hasText(productId)
				|| !StringUtils.hasText(rsaMerchPrivate)
				|| !StringUtils.hasText(rsaHuifuPublic)) {
			throw new ResourceException("请先配置支付信息");
		}
		String merKey = String.valueOf(companyId);
		synchronized (BSPAY_MUTEX) {
			try {
				BasePay.prodMode = bspayProdMode ? BasePay.MODE_PROD : BasePay.MODE_TEST;
				BasePay.debug = false;
				MerConfig mc = new MerConfig();
				mc.setSysId(sysId);
				mc.setProcutId(productId);
				mc.setRsaPrivateKey(rsaMerchPrivate);
				mc.setRsaPublicKey(rsaHuifuPublic);
				BasePay.addMerConfig(mc, merKey);
				return BasePayClient.request(request, merKey, false);
			} catch (BasePayException e) {
				throw e;
			} catch (IllegalAccessException e) {
				throw e;
			} catch (Exception e) {
				String msg = e.getMessage();
				throw new BasePayException(StringUtils.hasText(msg) ? msg : "请求失败");
			}
		}
	}

	private static String todayYmd() {
		return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
	}

	private String buildCashConfigJson() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("cash_type", "T1");
		row.put("fix_amt", "0.00");
		row.put("fee_rate", "0.00");
		row.put("weekday_fix_amt", "0.00");
		row.put("weekday_fee_rate", "0.00");
		row.put("out_fee_flag", "2");
		row.put("is_priority_receipt", "N");
		row.put("out_fee_acct_type", "01");
		try {
			return objectMapper.writeValueAsString(new Object[] {row});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("cash_config", e);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}
}
