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

package cn.shopex.ecshopx.thirdparty.service.shopexcrm;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderConfirmReceiptShopexCrmSyncExecutionService {

	private static final String SQL_PAY_STATUS =
			"SELECT pay_status FROM orders_normal_orders WHERE company_id = ? AND order_id = ? LIMIT 1";

	private final ShopexCrmSyncSingleOrderPort shopexCrmSyncSingleOrderPort;
	private final JdbcTemplate jdbcTemplate;
	private final String crmCrmSync;

	public NormalOrderConfirmReceiptShopexCrmSyncExecutionService(
			ShopexCrmSyncSingleOrderPort shopexCrmSyncSingleOrderPort,
			JdbcTemplate jdbcTemplate,
			@Value("${crm.crm-sync:}") String crmCrmSync) {
		this.shopexCrmSyncSingleOrderPort = shopexCrmSyncSingleOrderPort;
		this.jdbcTemplate = jdbcTemplate;
		this.crmCrmSync = crmCrmSync;
	}

	public void executeAfterNormalOrderConfirmReceipt(Map<String, Object> busPayload) {
		if (crmCrmSync == null || crmCrmSync.isBlank()) {
			return;
		}
		if (busPayload == null || busPayload.isEmpty()) {
			return;
		}
		Object rawCompany = busPayload.get("company_id");
		Object rawOrder = busPayload.get("order_id");
		if (rawCompany == null || rawOrder == null) {
			return;
		}
		long companyId = ((Number) rawCompany).longValue();
		String payStatus;
		try {
			payStatus = jdbcTemplate.queryForObject(SQL_PAY_STATUS, String.class, companyId, rawOrder);
		} catch (EmptyResultDataAccessException e) {
			return;
		}
		if (payStatus == null || !"PAYED".equals(payStatus.trim())) {
			return;
		}
		shopexCrmSyncSingleOrderPort.syncSingleOrder(companyId, rawOrder);
	}
}
