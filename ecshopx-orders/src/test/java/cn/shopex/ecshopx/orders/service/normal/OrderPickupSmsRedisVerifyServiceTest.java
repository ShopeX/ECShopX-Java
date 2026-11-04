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

package cn.shopex.ecshopx.orders.service.normal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OrderPickupSmsRedisVerifyServiceTest {

	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private ValueOperations<String, String> valueOps;
	@Mock private OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;

	@Test
	@DisplayName("admin-pickupcode 命中 12 位时校验通过并删除键")
	void verify_twelveDigitStoredInAdminPickup_ok() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("admin-pickupcode:100|13800000000")).thenReturn("760773986531");

		OrderPickupSmsRedisVerifyService svc =
				new OrderPickupSmsRedisVerifyService(companysRedisTemplate, orderZitiQrCodeRedisService);

		assertDoesNotThrow(() -> svc.verifyAndConsumePickupCode(100L, "13800000000", "760773986531"));
		verify(companysRedisTemplate).delete("admin-pickupcode:100|13800000000");
		verify(orderZitiQrCodeRedisService).deleteZitiCodeOrderMapping("760773986531");
	}

	@Test
	@DisplayName("admin-pickupcode 未命中但 orderziticode 映射到本单时校验通过")
	void verify_twelveDigitViaOrderZitiQrMapping_ok() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("admin-pickupcode:100|13800000000")).thenReturn(null);
		when(orderZitiQrCodeRedisService.resolveOrderIdNullable("760773986531")).thenReturn(100L);

		OrderPickupSmsRedisVerifyService svc =
				new OrderPickupSmsRedisVerifyService(companysRedisTemplate, orderZitiQrCodeRedisService);

		assertDoesNotThrow(() -> svc.verifyAndConsumePickupCode(100L, "13800000000", "760773986531"));
		verify(orderZitiQrCodeRedisService).deleteZitiCodeOrderMapping("760773986531");
	}

	@Test
	@DisplayName("两套 Redis 均无法核验时失败")
	void verify_neitherMapping_fails() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("admin-pickupcode:100|13800000000")).thenReturn(null);
		when(orderZitiQrCodeRedisService.resolveOrderIdNullable(eq("615171"))).thenReturn(null);

		OrderPickupSmsRedisVerifyService svc =
				new OrderPickupSmsRedisVerifyService(companysRedisTemplate, orderZitiQrCodeRedisService);

		assertThrows(
				ResourceException.class, () -> svc.verifyAndConsumePickupCode(100L, "13800000000", "615171"));
	}
}
