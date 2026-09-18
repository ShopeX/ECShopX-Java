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

package cn.shopex.ecshopx.payment.service.orderquery;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import com.huifu.bspay.sdk.opps.core.request.V3TradePaymentScanpayQueryRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BsPayPayOrderInfoQueryService {

	private static final Object BSPAY_ORDER_QUERY_MUTEX = new Object();

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.bspay.prod-mode:true}")
	private boolean bspayProdMode;

	public BsPayPayOrderInfoQueryService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * @param bspayOrgReqDate 原交易请求日期（yyyyMMdd），与落库的 {@code trade.bspay_req_date} 一致
	 */
	public String queryPayOrderInfoJson(long companyId, String tradeId, String transactionId, String bspayOrgReqDate) {
		Map<String, Object> cfg = loadBsPaySetting(companyId);
		if (cfg.isEmpty()) {
			throw new BadRequestException("请先配置支付信息", 400);
		}
		String sysId = Objects.toString(cfg.get("sys_id"), "").trim();
		String productId = Objects.toString(cfg.get("product_id"), "").trim();
		String rsaMerchPrivate = Objects.toString(cfg.get("rsa_merch_private_key"), "").trim();
		String rsaHuifuPublic = Objects.toString(cfg.get("rsa_huifu_public_key"), "").trim();
		if (!StringUtils.hasText(sysId)
				|| !StringUtils.hasText(productId)
				|| !StringUtils.hasText(rsaMerchPrivate)
				|| !StringUtils.hasText(rsaHuifuPublic)) {
			throw new BadRequestException("请先配置支付信息", 400);
		}
		String orgReqDate = bspayOrgReqDate == null ? "" : bspayOrgReqDate.trim();
		if (!StringUtils.hasText(orgReqDate)) {
			throw new BadRequestException("缺少原交易信息，无法查询斗拱订单", 400);
		}
		V3TradePaymentScanpayQueryRequest req = new V3TradePaymentScanpayQueryRequest();
		req.setHuifuId(sysId);
		req.setOrgReqDate(orgReqDate);
		req.setOrgReqSeqId(tradeId);
		if (StringUtils.hasText(transactionId)) {
			req.setOrgHfSeqId(transactionId.trim());
		}
		String merKey = String.valueOf(companyId);
		Map<String, Object> resp;
		synchronized (BSPAY_ORDER_QUERY_MUTEX) {
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
				throw new BadRequestException(StringUtils.hasText(msg) ? msg : "斗拱查询失败", 400);
			} catch (IllegalAccessException e) {
				throw new BadRequestException("斗拱查询失败", 400);
			} catch (Exception e) {
				throw new BadRequestException("斗拱查询失败", 400);
			}
		}
		Object dataPayload = extractJsonPayload(resp);
		JsonNode node = objectMapper.valueToTree(dataPayload == null ? Map.of() : dataPayload);
		try {
			return AdaBsPayOrderQueryJsonMapper.get().writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("斗拱查询失败", 400);
		}
	}

	private Map<String, Object> loadBsPaySetting(long companyId) {
		String key = "bspaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<>() {});
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

	@SuppressWarnings("unchecked")
	private static Object extractJsonPayload(Map<String, Object> resp) {
		if (resp == null) {
			return new LinkedHashMap<>();
		}
		Object layer1 = resp.get("data");
		if (!(layer1 instanceof Map<?, ?> m1)) {
			return resp;
		}
		Object inner = m1.get("data");
		if (inner != null) {
			return inner;
		}
		return m1;
	}
}
