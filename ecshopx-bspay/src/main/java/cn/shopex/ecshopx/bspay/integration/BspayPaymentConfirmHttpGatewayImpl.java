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

package cn.shopex.ecshopx.bspay.integration;

import cn.shopex.ecshopx.common.cron.payment.BspayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.port.payment.BspayPaymentSettingsReadPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import com.huifu.bspay.sdk.opps.core.request.V2TradePaymentDelaytransConfirmRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BspayPaymentConfirmHttpGatewayImpl implements BspayPaymentConfirmHttpGateway {

	private static final Object BSPAY_MUTEX = new Object();

	private final BspayPaymentSettingsReadPort bspayPaymentSettingsReadPort;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.bspay.prod-mode:true}")
	private boolean bspayProdMode;

	public BspayPaymentConfirmHttpGatewayImpl(
			BspayPaymentSettingsReadPort bspayPaymentSettingsReadPort, ObjectMapper objectMapper) {
		this.bspayPaymentSettingsReadPort = bspayPaymentSettingsReadPort;
		this.objectMapper = objectMapper;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> callDelaytransConfirm(Map<String, Object> data) {
		long companyId = ((Number) Objects.requireNonNull(data.get("company_id"))).longValue();
		Map<String, Object> cfg = bspayPaymentSettingsReadPort.requireSettingMap(companyId);
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
		V2TradePaymentDelaytransConfirmRequest request = new V2TradePaymentDelaytransConfirmRequest();
		request.setReqDate(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
		request.setReqSeqId(str(data.get("req_seq_id")));
		request.setHuifuId(str(data.get("huifu_id")));
		String orgReqDate = str(data.get("org_req_date"));
		String orgReqSeqId = str(data.get("org_req_seq_id"));
		Object acctInfos = data.get("acct_infos");
		String bunch;
		try {
			Map<String, Object> bunchMap = new LinkedHashMap<>();
			bunchMap.put("acct_infos", acctInfos);
			bunch = objectMapper.writeValueAsString(bunchMap);
		} catch (Exception e) {
			bunch = "{}";
		}
		request.addExtendInfo("org_req_date", orgReqDate);
		request.addExtendInfo("org_req_seq_id", orgReqSeqId);
		request.addExtendInfo("acct_split_bunch", bunch);
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
				Map<String, Object> resp = BasePayClient.request(request, merKey, false);
				return resp == null ? Map.of() : resp;
			} catch (BasePayException e) {
				Map<String, Object> inner = new LinkedHashMap<>();
				inner.put("trans_stat", "F");
				inner.put("resp_desc", e.getMessage());
				return Map.of("data", inner);
			} catch (IllegalAccessException e) {
				Map<String, Object> inner = new LinkedHashMap<>();
				inner.put("trans_stat", "F");
				inner.put("resp_desc", e.getMessage());
				return Map.of("data", inner);
			} catch (Exception e) {
				String msg = e.getMessage();
				Map<String, Object> inner = new LinkedHashMap<>();
				inner.put("trans_stat", "F");
				inner.put("resp_desc", StringUtils.hasText(msg) ? msg : "请求失败");
				return Map.of("data", inner);
			}
		}
	}

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}
}
