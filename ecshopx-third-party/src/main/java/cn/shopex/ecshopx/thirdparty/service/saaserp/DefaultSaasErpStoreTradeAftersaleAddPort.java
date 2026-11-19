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
public class DefaultSaasErpStoreTradeAftersaleAddPort implements SaasErpStoreTradeAftersaleAddPort {

	private static final Logger log = LoggerFactory.getLogger(DefaultSaasErpStoreTradeAftersaleAddPort.class);

	private static final String METHOD_ADD = "store.trade.aftersale.add";
	private static final String METHOD_OME_STATUS = "ome.aftersale.status_update";

	private final SaasErpMatrixOutboundClient matrixOutboundClient;

	public DefaultSaasErpStoreTradeAftersaleAddPort(SaasErpMatrixOutboundClient matrixOutboundClient) {
		this.matrixOutboundClient = matrixOutboundClient;
	}

	@Override
	public void callStoreTradeAftersaleAdd(long companyId, Map<String, Object> requestBody) {
		call(companyId, METHOD_ADD, requestBody);
	}

	@Override
	public void callOmeAftersaleStatusUpdate(long companyId, Map<String, Object> requestBody) {
		call(companyId, METHOD_OME_STATUS, requestBody);
	}

	private void call(long companyId, String method, Map<String, Object> requestBody) {
		if (companyId <= 0 || requestBody == null || requestBody.isEmpty()) {
			return;
		}
		try {
			Map<String, Object> result = matrixOutboundClient.call(companyId, method, requestBody);
			log.debug("{} companyId={} resultKeys={}", method, companyId, result == null ? 0 : result.keySet());
		} catch (RuntimeException e) {
			log.debug("{} failed companyId={} msg={}", method, companyId, e.getMessage());
		}
	}
}
