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
public class DefaultSaasErpStoreTradeAftersaleCancelPort implements SaasErpStoreTradeAftersaleCancelPort {

	private static final Logger log = LoggerFactory.getLogger(DefaultSaasErpStoreTradeAftersaleCancelPort.class);

	@Override
	public void callStoreTradeAftersaleCancel(Map<String, Object> requestBody) {
		if (requestBody == null || requestBody.isEmpty()) {
			return;
		}
		try {
			log.debug("store.trade.aftersale.cancel body={}", requestBody);
		} catch (RuntimeException e) {
			log.debug("store.trade.aftersale.cancel logging failed: {}", e.getMessage());
		}
	}
}
