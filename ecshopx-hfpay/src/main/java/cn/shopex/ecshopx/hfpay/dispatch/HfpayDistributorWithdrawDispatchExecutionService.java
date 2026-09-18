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

package cn.shopex.ecshopx.hfpay.dispatch;

import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class HfpayDistributorWithdrawDispatchExecutionService {

	private static final DateTimeFormatter ORDER_DAY = DateTimeFormatter.BASIC_ISO_DATE;
	private static final String DEV_MAC = "D4-81-D7-F0-42-F8";

	private final HfpayCashRecordMapper cashRecordMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final ObjectMapper objectMapper;
	private final ZoneId businessZoneId;
	private final String bgRetUrl;

	public HfpayDistributorWithdrawDispatchExecutionService(
			HfpayCashRecordMapper cashRecordMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfPayAcouJsonPostClient acouJsonPostClient,
			ObjectMapper objectMapper,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp,
			@Value("${ecshopx.hfpay.bg-ret-url:}") String bgRetUrl) {
		this.cashRecordMapper = cashRecordMapper;
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
		long id = longFrom(entities.get("hfpay_cash_record_id"));
		if (id <= 0L) {
			return;
		}
		HfpayCashRecord data = cashRecordMapper.selectById(id);
		if (data == null) {
			return;
		}
		Integer transAmt = data.getTransAmt();
		if (transAmt == null || transAmt < 1) {
			return;
		}

		String transAmtYuan =
				BigDecimal.valueOf(transAmt.longValue()).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();

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

		Map<String, Object> setting = paymentSettingService.loadForCompany(data.getCompanyId());
		String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();
		String orderDate = LocalDate.now(businessZoneId).format(ORDER_DAY);

		LinkedHashMap<String, Object> cashPayload = new LinkedHashMap<>();
		cashPayload.put("version", 10);
		cashPayload.put("mer_cust_id", merCustId);
		cashPayload.put("user_cust_id", data.getUserCustId());
		cashPayload.put("order_date", orderDate);
		cashPayload.put("order_id", data.getOrderId());
		cashPayload.put("trans_amt", transAmtYuan);
		cashPayload.put("bind_card_id", data.getBindCardId());
		cashPayload.put("cash_type", data.getCashType());
		cashPayload.put("bg_ret_url", bgRetUrl);
		cashPayload.put("mer_priv", "cash01_distributor");
		cashPayload.put("dev_info_json", devInfoJson);

		Map<String, Object> result = acouJsonPostClient.cash01(setting, cashPayload);

		String respCode = result.get("resp_code") == null ? "" : String.valueOf(result.get("resp_code")).trim();
		int cashStatus = "C00001".equals(respCode) ? 1 : 3;

		Integer feeAmtFen = null;
		Object feeRaw = result.get("fee_amt");
		if (feeRaw != null && StringUtils.hasText(String.valueOf(feeRaw).trim())) {
			try {
				BigDecimal feeYuan = new BigDecimal(String.valueOf(feeRaw).trim());
				feeAmtFen = feeYuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
			} catch (Exception ignored) {
				feeAmtFen = null;
			}
		}

		HfpayCashRecord upd = new HfpayCashRecord();
		upd.setHfpayCashRecordId(data.getHfpayCashRecordId());
		upd.setCashStatus(cashStatus);
		upd.setFeeAmt(feeAmtFen);
		upd.setRespCode(respCode.isEmpty() ? null : respCode);
		Object desc = result.get("resp_desc");
		upd.setRespDesc(desc == null ? null : String.valueOf(desc));
		Object hfOid = result.get("order_id");
		upd.setHfOrderId(hfOid == null ? null : String.valueOf(hfOid));
		Object hfOdate = result.get("order_date");
		upd.setHfOrderDate(hfOdate == null ? null : String.valueOf(hfOdate));

		cashRecordMapper.updateById(upd);
	}

	private static long longFrom(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
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
