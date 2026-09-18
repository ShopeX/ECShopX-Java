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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.service.integration.BsPaySettlementEncashmentSdkClient;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BsPayWithdrawExecuteService {

	private static final DateTimeFormatter REQ_SEQ_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private final BsPayPaymentSettingService bsPayPaymentSettingService;
	private final BsPaySettlementEncashmentSdkClient bsPaySettlementEncashmentSdkClient;

	@Value("${ecshopx.bspay.notify-url:}")
	private String notifyUrlBase;

	public Map<String, Object> doWithdraw(Map<String, Object> params) {
		Object cid = params.get("company_id");
		if (cid == null) {
			throw new ResourceException("汇付配置不存在，请先配置汇付支付");
		}
		long companyId =
				cid instanceof Number ? ((Number) cid).longValue() : Long.parseLong(cid.toString().trim());

		Map<String, Object> config = bsPayPaymentSettingService.requireSettingMap(companyId);
		if (!bspayConfigIsOpen(config.get("is_open"))) {
			throw new ResourceException("汇付支付未开启");
		}

		String tokenNo = Objects.toString(params.get("token_no"), "").trim();
		if (!StringUtils.hasText(tokenNo)) {
			throw new ResourceException("取现卡序列号不能为空，请先绑定银行卡");
		}

		String reqSeqId = generateWithdrawReqSeqId();
		BigDecimal cashAmtBd =
				BigDecimal.valueOf(((Number) params.get("amount")).longValue())
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		String cashAmt = cashAmtBd.toPlainString();

		String intoAcct = Objects.toString(params.get("withdraw_type"), "").trim();
		if (!StringUtils.hasText(intoAcct)) {
			intoAcct = "T1";
		}

		String notifyUrl = buildWithdrawNotifyUrl();

		Map<String, Object> mergedMap = new LinkedHashMap<>();
		mergedMap.put("req_seq_id", reqSeqId);
		mergedMap.put("cash_amt", cashAmt);
		mergedMap.put("huifu_id", Objects.toString(params.get("huifu_id"), "").trim());
		mergedMap.put("into_acct_date_type", intoAcct);
		mergedMap.put("token_no", tokenNo);
		mergedMap.put("enchashment_channel", "00");
		mergedMap.put("remark", "手动提现申请");
		mergedMap.put("notify_url", notifyUrl);

		Map<String, Object> resData;
		try {
			resData = bsPaySettlementEncashmentSdkClient.handle(companyId, mergedMap);
		} catch (BasePayException e) {
			throw new ResourceException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new ResourceException(e.getMessage());
		}

		if (resData != null && resData.containsKey("msg") && resData.get("msg") != null) {
			String errorMsg = Objects.toString(resData.get("msg"), "").trim();
			if (!StringUtils.hasText(errorMsg)) {
				errorMsg = "汇付API调用失败";
			}
			throw new ResourceException(errorMsg);
		}

		Map<String, Object> result = bsPaySettlementEncashmentSdkClient.extractResponseDataLayer(resData);
		if (result == null) {
			result = new LinkedHashMap<>();
		}

		String respCode = Objects.toString(result.get("resp_code"), "");
		String transStat = Objects.toString(result.get("trans_stat"), "");
		String respDesc = Objects.toString(result.get("resp_desc"), "汇付取现失败");

		if ("00000000".equals(respCode) && ("S".equals(transStat) || "P".equals(transStat))) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("success", Boolean.TRUE);
			out.put("hf_seq_id", Objects.toString(result.get("hf_seq_id"), ""));
			out.put("req_seq_id", Objects.toString(result.get("req_seq_id"), ""));
			out.put("trans_stat", transStat);
			out.put("data", result);
			return out;
		}

		String errorMsg;
		if ("00000000".equals(respCode) && "F".equals(transStat)) {
			errorMsg = "取现处理失败：" + respDesc;
		} else {
			errorMsg = respDesc;
		}
		throw new ResourceException(errorMsg);
	}

	private static boolean bspayConfigIsOpen(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim().toLowerCase(Locale.ROOT);
		if (s.isEmpty() || "0".equals(s) || "false".equals(s)) {
			return false;
		}
		return true;
	}

	private String buildWithdrawNotifyUrl() {
		String base = notifyUrlBase == null ? "" : notifyUrlBase.trim();
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/withdraw.bspay";
	}

	private static String generateWithdrawReqSeqId() {
		String ts = LocalDateTime.now().format(REQ_SEQ_TS);
		int r = ThreadLocalRandom.current().nextInt(1000, 10000);
		return "W" + ts + r;
	}
}
