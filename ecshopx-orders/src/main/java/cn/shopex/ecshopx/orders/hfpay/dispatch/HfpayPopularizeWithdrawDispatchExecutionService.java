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

package cn.shopex.ecshopx.orders.hfpay.dispatch;

import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import cn.shopex.ecshopx.orders.domain.MerchantPaymentTrade;
import cn.shopex.ecshopx.orders.mapper.MerchantPaymentTradeMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class HfpayPopularizeWithdrawDispatchExecutionService {

	private static final DateTimeFormatter ORDER_DAY = DateTimeFormatter.BASIC_ISO_DATE;
	private static final String DEV_MAC = "D4-81-D7-F0-42-F8";

	private final MerchantPaymentTradeMapper merchantPaymentTradeMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final ObjectMapper objectMapper;
	private final ZoneId businessZoneId;
	private final String bgRetUrl;

	public HfpayPopularizeWithdrawDispatchExecutionService(
			MerchantPaymentTradeMapper merchantPaymentTradeMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfPayAcouJsonPostClient acouJsonPostClient,
			ObjectMapper objectMapper,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp,
			@Value("${ecshopx.hfpay.bg-ret-url:}") String bgRetUrl) {
		this.merchantPaymentTradeMapper = merchantPaymentTradeMapper;
		this.paymentSettingService = paymentSettingService;
		this.acouJsonPostClient = acouJsonPostClient;
		this.objectMapper = objectMapper;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
		this.bgRetUrl = bgRetUrl == null ? "" : bgRetUrl;
	}

	@SuppressWarnings("unchecked")
	public void executeFromDispatchPayload(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Object rawEntities = payload.get("entities");
		if (!(rawEntities instanceof Map)) {
			return;
		}
		Map<String, Object> entities = (Map<String, Object>) rawEntities;
		Object rawId = entities.get("merchant_trade_id");
		if (rawId == null) {
			return;
		}
		String merchantTradeId = String.valueOf(rawId).trim();
		if (!StringUtils.hasText(merchantTradeId)) {
			return;
		}

		MerchantPaymentTrade trade = merchantPaymentTradeMapper.selectById(merchantTradeId);
		if (trade == null) {
			return;
		}
		Long amountFen = trade.getAmount();
		if (amountFen == null || amountFen < 1L) {
			return;
		}
		if (!StringUtils.hasText(trade.getUserCustId()) || !StringUtils.hasText(trade.getBindCardId())) {
			return;
		}

		String transAmtYuan = BigDecimal.valueOf(amountFen).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();

		String devInfoJson;
		try {
			LinkedHashMap<String, String> dev = new LinkedHashMap<>();
			dev.put("ipAddr", resolveClientIp());
			dev.put("devType", "1");
			dev.put("MAC", DEV_MAC);
			devInfoJson = objectMapper.writeValueAsString(dev);
		} catch (JsonProcessingException e) {
			devInfoJson = "{\"ipAddr\":\"\",\"devType\":\"1\",\"MAC\":\"" + DEV_MAC + "\"}";
		}

		Map<String, Object> setting = paymentSettingService.loadForCompany(trade.getCompanyId());
		String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();
		String orderDate = LocalDate.now(businessZoneId).format(ORDER_DAY);
		String cashType = StringUtils.hasText(trade.getHfCashType()) ? trade.getHfCashType().trim() : "T1";

		LinkedHashMap<String, Object> cashPayload = new LinkedHashMap<>();
		cashPayload.put("version", 10);
		cashPayload.put("mer_cust_id", merCustId);
		cashPayload.put("user_cust_id", trade.getUserCustId());
		cashPayload.put("order_date", orderDate);
		cashPayload.put("order_id", trade.getMerchantTradeId());
		cashPayload.put("trans_amt", transAmtYuan);
		cashPayload.put("bind_card_id", trade.getBindCardId());
		cashPayload.put("cash_type", cashType);
		cashPayload.put("bg_ret_url", bgRetUrl);
		cashPayload.put("mer_priv", "cash01_popularize");
		cashPayload.put("dev_info_json", devInfoJson);

		Map<String, Object> result = acouJsonPostClient.cash01(setting, cashPayload);

		String respCode = result.get("resp_code") == null ? "" : String.valueOf(result.get("resp_code")).trim();
		String nextStatus = "C00001".equals(respCode) ? "PAYING" : "FAIL";
		int nowTs = (int) Instant.now().getEpochSecond();

		MerchantPaymentTrade upd = new MerchantPaymentTrade();
		upd.setMerchantTradeId(trade.getMerchantTradeId());
		upd.setStatus(nextStatus);
		Object desc = result.get("resp_desc");
		upd.setErrorCode(respCode.isEmpty() ? null : respCode);
		upd.setErrorDesc(desc == null ? null : String.valueOf(desc));
		Object hfOid = result.get("order_id");
		upd.setHfOrderId(hfOid == null ? null : String.valueOf(hfOid));
		Object hfOdate = result.get("order_date");
		upd.setHfOrderDate(hfOdate == null ? null : String.valueOf(hfOdate));
		upd.setUpdateTime(nowTs);

		merchantPaymentTradeMapper.updateById(upd);
	}

	private static String resolveClientIp() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return "";
		}
		HttpServletRequest req = attrs.getRequest();
		if (req == null) {
			return "";
		}
		String xff = req.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			String first = xff.split(",")[0].trim();
			if (StringUtils.hasText(first)) {
				return first;
			}
		}
		String remote = req.getRemoteAddr();
		return remote == null ? "" : remote;
	}
}
