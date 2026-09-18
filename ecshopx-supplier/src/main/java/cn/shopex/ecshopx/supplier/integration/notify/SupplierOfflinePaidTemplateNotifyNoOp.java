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

package cn.shopex.ecshopx.supplier.integration.notify;

import cn.shopex.ecshopx.common.supplier.SupplierOfflinePaidTemplateNotifyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("supplierOfflinePaidTemplateNotifyNoOp")
@ConditionalOnProperty(
		prefix = "ecshopx.supplier.offline-paid-wxa-notify",
		name = "http-enabled",
		havingValue = "false",
		matchIfMissing = true)
public class SupplierOfflinePaidTemplateNotifyNoOp implements SupplierOfflinePaidTemplateNotifyPort {

	private static final Logger log = LoggerFactory.getLogger(SupplierOfflinePaidTemplateNotifyNoOp.class);

	@Override
	public void notifySingle(long companyId, String wxOpenid, java.util.Map<String, Object> msgData) {
		log.debug(
				"Offline paid template notify skipped (no-op): companyId={}, openid={}, msgDataKeys={}",
				companyId,
				wxOpenid,
				msgData == null ? 0 : msgData.size());
	}
}
