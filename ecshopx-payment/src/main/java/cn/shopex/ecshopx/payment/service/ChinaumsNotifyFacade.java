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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.chinaumspay.service.notify.ChinaumsAsyncNotifyVerificationService;
import cn.shopex.ecshopx.chinaumspay.service.notify.ChinaumsVerifiedNotifyParams;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.payment.integration.chinaums.ChinaumsNotifyOrderSidePort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ChinaumsNotifyFacade {

	private static final Logger log = LoggerFactory.getLogger(ChinaumsNotifyFacade.class);

	private final ChinaumsAsyncNotifyVerificationService chinaumsAsyncNotifyVerificationService;
	private final ChinaumsNotifyOrderSidePort chinaumsNotifyOrderSidePort;

	public ChinaumsNotifyFacade(
			ChinaumsAsyncNotifyVerificationService chinaumsAsyncNotifyVerificationService,
			ChinaumsNotifyOrderSidePort chinaumsNotifyOrderSidePort) {
		this.chinaumsAsyncNotifyVerificationService = chinaumsAsyncNotifyVerificationService;
		this.chinaumsNotifyOrderSidePort = chinaumsNotifyOrderSidePort;
	}

	public ResponseEntity<String> handle(HttpServletRequest request) {
		Map<String, Object> objectMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		Map<String, String> flat = NotifyHttpParameterFlatten.flattenObjectMap(objectMap);
		logChinaumsResponseSummary(flat);
		try {
			ChinaumsVerifiedNotifyParams p = chinaumsAsyncNotifyVerificationService.verify(flat);
			String rawStatus = p.status() == null ? "" : p.status();
			String statusForUpdate = "TRADE_SUCCESS".equals(rawStatus) ? "SUCCESS" : rawStatus;
			if ("TRADE_REFUND".equals(rawStatus)) {
				return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("SUCCESS");
			}
			log.info(
					"chinaums:params: status={} out_trade_no={} trade_no={}",
					statusForUpdate,
					p.outTradeNo(),
					p.tradeNo());
			Map<String, Object> options = new LinkedHashMap<>();
			options.put("pay_type", p.payType());
			options.put("transaction_id", p.tradeNo() == null ? "" : p.tradeNo());
			chinaumsNotifyOrderSidePort.applyTradePaymentAfterChinaums(p.outTradeNo(), statusForUpdate, options);
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("SUCCESS");
		} catch (Exception e) {
			String msg = e.getMessage();
			log.info("chinaums:e:{}", msg != null ? msg : "");
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("FAILED");
		}
	}

	private static void logChinaumsResponseSummary(Map<String, String> rawFlat) {
		List<String> keys = new ArrayList<>(rawFlat.keySet());
		keys.removeIf(k -> "sign".equalsIgnoreCase(k));
		log.info("chinaums:response: keys={}", keys);
	}
}
