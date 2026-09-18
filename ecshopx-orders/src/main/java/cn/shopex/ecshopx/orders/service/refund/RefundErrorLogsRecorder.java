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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.orders.domain.RefundErrorLogs;
import cn.shopex.ecshopx.orders.mapper.RefundErrorLogsMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 对位 PHP {@code PaymentsService::saveRefundError}。 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefundErrorLogsRecorder {

	private final RefundErrorLogsMapper refundErrorLogsMapper;
	private final ObjectMapper objectMapper;

	public void saveRefundError(
			long companyId, String wxaAppid, Map<String, Object> refundData, Map<String, Object> result) {
		try {
			String dataJson = objectMapper.writeValueAsString(refundData);
			int now = (int) (System.currentTimeMillis() / 1000L);
			RefundErrorLogs row = new RefundErrorLogs();
			row.setCompanyId(companyId);
			row.setOrderId(toLong(refundData.get("order_id")));
			row.setWxaAppid(wxaAppid);
			row.setDataJson(dataJson);
			row.setStatus(String.valueOf(result.getOrDefault("status", "FAIL")));
			row.setErrorCode(stringOrEmpty(result.get("error_code")));
			row.setErrorDesc(stringOrEmpty(result.get("error_desc")));
			row.setIsResubmit(Boolean.FALSE);
			row.setCreateTime(now);
			row.setUpdateTime(now);
			row.setMerchantId(toLongOrDefault(refundData.get("merchant_id"), 0L));
			row.setSupplierId(toLongOrDefault(refundData.get("supplier_id"), 0L));
			row.setDistributorId(toLongOrDefault(refundData.get("distributor_id"), 0L));
			refundErrorLogsMapper.insert(row);
		} catch (JsonProcessingException e) {
			log.warn(
					"saveRefundError serialize failed companyId={} orderId={}",
					companyId,
					refundData.get("order_id"),
					e);
		} catch (Exception e) {
			log.warn(
					"saveRefundError insert failed companyId={} orderId={}",
					companyId,
					refundData.get("order_id"),
					e);
		}
	}

	private static Long toLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long toLongOrDefault(Object v, long defaultValue) {
		Long parsed = toLong(v);
		return parsed == null ? defaultValue : parsed;
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
