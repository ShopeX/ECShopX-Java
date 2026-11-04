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

package cn.shopex.ecshopx.payment.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentSettingDistributorAccessGuardTest {

	@Mock
	private DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;

	private PaymentSettingDistributorAccessGuard guard;

	@BeforeEach
	void setUp() {
		guard = new PaymentSettingDistributorAccessGuard(distributorGetInfoSimpleByDistributorIdPort);
	}

	@Test
	void admin_mayReadPlatformSettingWithoutDistributorLookup() {
		assertTrue(guard.mayRead(10L, 0L, "admin", 0L));
		verify(distributorGetInfoSimpleByDistributorIdPort, never())
				.getInfoSimpleByDistributorId(anyLong(), anyLong());
	}

	@Test
	void admin_rejectsDistributorNotInCompany() {
		when(distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(10L, 99L))
				.thenReturn(Map.of());
		assertFalse(guard.mayRead(10L, 99L, "admin", 0L));
	}

	@Test
	void admin_allowsDistributorInCompany() {
		when(distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(10L, 5L))
				.thenReturn(Map.of("distributor_id", 5L));
		assertTrue(guard.mayRead(10L, 5L, "admin", 0L));
	}

	@Test
	void storeOperator_rejectsOtherDistributorId() {
		assertFalse(guard.mayRead(10L, 99L, "distributor", 5L));
		verify(distributorGetInfoSimpleByDistributorIdPort, never())
				.getInfoSimpleByDistributorId(anyLong(), anyLong());
	}

	@Test
	void storeOperator_rejectsPlatformZero() {
		assertFalse(guard.mayRead(10L, 0L, "distributor", 5L));
	}

	@Test
	void storeOperator_allowsOwnDistributorWhenInCompany() {
		when(distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(eq(10L), eq(5L)))
				.thenReturn(Map.of("distributor_id", 5L));
		assertTrue(guard.mayRead(10L, 5L, "distributor", 5L));
	}

	@Test
	void resolveWriteDistributorId_forcesJwtStoreForDistributorOperator() {
		assertEquals(5L, PaymentSettingDistributorAccessGuard.resolveWriteDistributorId("distributor", 99L, 5L));
		assertEquals(5L, PaymentSettingDistributorAccessGuard.resolveWriteDistributorId("distributor", 0L, 5L));
		assertEquals(99L, PaymentSettingDistributorAccessGuard.resolveWriteDistributorId("admin", 99L, 5L));
	}
}
