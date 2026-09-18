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
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import com.huifu.bspay.sdk.opps.core.request.V2TradeSettlementEncashmentRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BsPaySettlementEncashmentSdkClient {

	private static final Object BSPAY_MUTEX = new Object();

	private final BsPayPaymentSettingService bsPayPaymentSettingService;

	@Value("${ecshopx.bspay.prod-mode:true}")
	private boolean bspayProdMode;

	public BsPaySettlementEncashmentSdkClient(BsPayPaymentSettingService bsPayPaymentSettingService) {
		this.bsPayPaymentSettingService = bsPayPaymentSettingService;
	}

	public Map<String, Object> extractResponseDataLayer(Map<String, Object> resp) {
		return extractBsPayDataPayload(resp);
	}

	public Map<String, Object> handle(long companyId, Map<String, Object> requestParams)
			throws BasePayException, IllegalAccessException {
		Map<String, Object> cfg = bsPayPaymentSettingService.requireSettingMap(companyId);
		String sysId = str(cfg.get("sys_id"));
		String productId = str(cfg.get("product_id"));
		String rsaMerchPrivate = str(cfg.get("rsa_merch_private_key"));
		String rsaHuifuPublic = str(cfg.get("rsa_huifu_public_key"));
		if (!StringUtils.hasText(sysId)
				|| !StringUtils.hasText(productId)
				|| !StringUtils.hasText(rsaMerchPrivate)
				|| !StringUtils.hasText(rsaHuifuPublic)) {
			throw new ResourceException("请先配置支付信息");
		}

		V2TradeSettlementEncashmentRequest request = new V2TradeSettlementEncashmentRequest();
		request.setReqDate(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
		request.setReqSeqId(str(requestParams.get("req_seq_id")));
		request.setCashAmt(str(requestParams.get("cash_amt")));
		request.setHuifuId(str(requestParams.get("huifu_id")));
		request.setIntoAcctDateType(str(requestParams.get("into_acct_date_type")));
		request.setTokenNo(str(requestParams.get("token_no")));
		request.addExtendInfo("enchashment_channel", str(requestParams.get("enchashment_channel")));
		request.addExtendInfo("remark", str(requestParams.get("remark")));
		request.addExtendInfo("notify_url", str(requestParams.get("notify_url")));

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

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}

	/**
	 * 兼容斗拱 SDK 返回体 {@code { data: { resp_code, ... } }} 与 {@code { data: { data: { resp_code, ... }}}}.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> extractBsPayDataPayload(Map<String, Object> resp) {
		if (resp == null) {
			return null;
		}
		Object layer1 = resp.get("data");
		if (!(layer1 instanceof Map<?, ?> m1)) {
			return null;
		}
		Object inner = m1.get("data");
		if (inner instanceof Map<?, ?> m2 && m2.containsKey("resp_code")) {
			return (Map<String, Object>) m2;
		}
		if (m1.containsKey("resp_code")) {
			return (Map<String, Object>) m1;
		}
		return null;
	}
}
