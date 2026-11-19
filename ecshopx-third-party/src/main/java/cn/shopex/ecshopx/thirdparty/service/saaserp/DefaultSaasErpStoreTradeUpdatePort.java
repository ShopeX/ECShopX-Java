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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultSaasErpStoreTradeUpdatePort implements SaasErpStoreTradeUpdatePort {

	private static final Logger log = LoggerFactory.getLogger(DefaultSaasErpStoreTradeUpdatePort.class);

	private static final String METHOD = "store.trade.update";

	private final SaasErpMatrixOutboundClient matrixOutboundClient;

	public DefaultSaasErpStoreTradeUpdatePort(SaasErpMatrixOutboundClient matrixOutboundClient) {
		this.matrixOutboundClient = matrixOutboundClient;
	}

	@Override
	public void callStoreTradeUpdate(long companyId, Map<String, Object> requestBody) {
		if (companyId <= 0 || requestBody == null || requestBody.isEmpty()) {
			return;
		}
		try {
			Map<String, Object> result = matrixOutboundClient.call(companyId, METHOD, requestBody);
			log.debug(
					"{} companyId={} resultKeys={}",
					METHOD,
					companyId,
					result == null ? 0 : result.keySet());
		} catch (RuntimeException e) {
			log.debug("{} failed companyId={} msg={}", METHOD, companyId, e.getMessage());
		}
	}
}
