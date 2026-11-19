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

package cn.shopex.ecshopx.thirdparty.service.saaserp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultSaasErpStoreTradeAftersaleCancelPort implements SaasErpStoreTradeAftersaleCancelPort {

	private static final Logger log = LoggerFactory.getLogger(DefaultSaasErpStoreTradeAftersaleCancelPort.class);

	private static final String METHOD_REFUND_ADD = "store.trade.refund.add";
	private static final String METHOD_STATUS_UPDATE = "store.trade.aftersale.status.update";

	private final SaasErpMatrixOutboundClient matrixOutboundClient;

	public DefaultSaasErpStoreTradeAftersaleCancelPort(SaasErpMatrixOutboundClient matrixOutboundClient) {
		this.matrixOutboundClient = matrixOutboundClient;
	}

	@Override
	public void callStoreTradeAftersaleCancel(long companyId, Map<String, Object> requestBody) {
		if (companyId <= 0 || requestBody == null || requestBody.isEmpty()) {
			return;
		}
		String type = text(requestBody.get("aftersales_type"));
		boolean onlyRefund = "ONLY_REFUND".equalsIgnoreCase(type);
		String method = onlyRefund ? METHOD_REFUND_ADD : METHOD_STATUS_UPDATE;
		Map<String, Object> body = new LinkedHashMap<>(requestBody);
		if (!onlyRefund) {
			body.put("status", 5);
		}
		try {
			Map<String, Object> result = matrixOutboundClient.call(companyId, method, body);
			log.debug(
					"aftersale cancel via {} companyId={} resultKeys={}",
					method,
					companyId,
					result == null ? 0 : result.keySet());
		} catch (RuntimeException e) {
			log.debug("aftersale cancel via {} failed companyId={} msg={}", method, companyId, e.getMessage());
		}
	}

	private static String text(Object o) {
		if (o == null) {
			return "";
		}
		String s = o.toString().trim();
		return StringUtils.hasText(s) ? s : "";
	}
}
