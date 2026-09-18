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

package cn.shopex.ecshopx.payment.service.orderrefund;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import com.huifu.bspay.sdk.opps.core.request.V3TradePaymentScanpayRefundRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 斗拱扫码交易退款（网关 {@code PaymentScanpayRefund} / {@code V2TradePaymentScanpayRefundRequest}）。
 */
@Component
public class BsPayAftersalesOnlineRefundRunner {

	private static final Object BSPAY_MUTEX = new Object();

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.bspay.prod-mode:true}")
	private boolean bspayProdMode;

	public BsPayAftersalesOnlineRefundRunner(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> refund(
			long companyId,
			String tradeId,
			String orgReqDate,
			long refundBn,
			int refundFeeFen) {
		Map<String, Object> cfg = loadBsPaySetting(companyId);
		if (cfg.isEmpty()) {
			return fail("请先配置支付信息");
		}
		String sysId = Objects.toString(cfg.get("sys_id"), "").trim();
		String productId = Objects.toString(cfg.get("product_id"), "").trim();
		String rsaMerchPrivate = Objects.toString(cfg.get("rsa_merch_private_key"), "").trim();
		String rsaHuifuPublic = Objects.toString(cfg.get("rsa_huifu_public_key"), "").trim();
		if (!StringUtils.hasText(sysId)
				|| !StringUtils.hasText(productId)
				|| !StringUtils.hasText(rsaMerchPrivate)
				|| !StringUtils.hasText(rsaHuifuPublic)) {
			return fail("请先配置支付信息");
		}
		if (!StringUtils.hasText(tradeId) || !StringUtils.hasText(orgReqDate)) {
			return fail("缺少原交易信息，无法发起斗拱退款");
		}
		String ordAmt =
				BigDecimal.valueOf(refundFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
		String reqDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		String reqSeqId = refundBn + "_" + (10000 + (int) (Math.random() * 80000)) + System.currentTimeMillis();

		V3TradePaymentScanpayRefundRequest req = new V3TradePaymentScanpayRefundRequest();
		req.setReqDate(reqDate);
		req.setReqSeqId(reqSeqId);
		req.setHuifuId(sysId);
		req.setOrdAmt(ordAmt);
		req.setOrgReqDate(orgReqDate);
		req.addExtendInfo("org_req_seq_id", tradeId);

		String merKey = String.valueOf(companyId);
		Map<String, Object> resp;
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
				resp = BasePayClient.request(req, merKey, false);
			} catch (BasePayException e) {
				String msg = e.getMessage();
				return fail(StringUtils.hasText(msg) ? msg : "斗拱退款失败");
			} catch (IllegalAccessException e) {
				return fail("斗拱退款失败");
			} catch (Exception e) {
				return fail("斗拱退款失败");
			}
		}

		Map<String, Object> data = extractBsPayDataPayload(resp);
		if (data == null) {
			return fail("斗拱退款失败");
		}
		String respCode = Objects.toString(data.get("resp_code"), "");
		String transStat = Objects.toString(data.get("trans_stat"), "");
		if (!"00000000".equals(respCode) && !"00000100".equals(respCode)) {
			String desc = Objects.toString(data.get("resp_desc"), "斗拱退款失败");
			return fail(StringUtils.hasText(desc) ? desc : "斗拱退款失败");
		}
		if ("F".equalsIgnoreCase(transStat)) {
			String desc = Objects.toString(data.get("resp_desc"), "斗拱退款失败");
			return fail(StringUtils.hasText(desc) ? desc : "斗拱退款失败");
		}
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", reqSeqId);
		return ok;
	}

	private Map<String, Object> loadBsPaySetting(long companyId) {
		String key = "bspaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			Map<String, Object> m = objectMapper.readValue(raw, new TypeReference<>() {});
			return m != null ? m : Map.of();
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static String sha1Hex(String companyId) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(companyId.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}

	private static Map<String, Object> fail(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", "FAIL");
		m.put("error_desc", msg);
		return m;
	}

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
