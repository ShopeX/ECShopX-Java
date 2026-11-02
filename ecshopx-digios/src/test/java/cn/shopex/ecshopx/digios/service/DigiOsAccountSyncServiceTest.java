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

package cn.shopex.ecshopx.digios.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.digios.client.DigiOsGatewayClient;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigiOsAccountSyncServiceTest {

	@Mock
	private DigiOsGatewayClient gatewayClient;

	@Test
	void syncCreate_skipsDistributor() {
		DigiOsAccountSyncService service = new DigiOsAccountSyncService(gatewayClient);
		boolean ok =
				service.syncCreate(
						1L,
						Map.of(
								"operator_type",
								"distributor",
								"operator_id",
								9L,
								"login_name",
								"shop01",
								"mobile",
								"13800138000"));
		assertTrue(ok);
		verify(gatewayClient, never()).put(anyLong(), any(), any());
	}

	@Test
	void syncCreatePos_skipsWhenMobileEmpty() {
		DigiOsAccountSyncService service = new DigiOsAccountSyncService(gatewayClient);
		boolean ok = service.syncCreatePos(1L, Map.of("id", 3L, "employee_no", "E01", "name", "张三"));
		assertFalse(ok);
		verify(gatewayClient, never()).put(anyLong(), any(), any());
	}

	@Test
	void syncCreatePos_upsertsWithEmployeeNo() {
		DigiOsAccountSyncService service = new DigiOsAccountSyncService(gatewayClient);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("usrId", "1001");
		data.put("acc", "E01");
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		when(gatewayClient.put(eq(1L), eq("account.upsert"), any()))
				.thenReturn(new DigiOsGatewayResult(true, 200, body, null, null));
		boolean ok =
				service.syncCreatePos(
						1L,
						Map.of("id", 3L, "employee_no", "E01", "name", "张三", "mobile", "13900001111"));
		assertTrue(ok);
		verify(gatewayClient).put(eq(1L), eq("account.upsert"), any());
	}
}
