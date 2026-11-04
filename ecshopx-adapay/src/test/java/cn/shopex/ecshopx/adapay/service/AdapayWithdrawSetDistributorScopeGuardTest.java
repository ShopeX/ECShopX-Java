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

package cn.shopex.ecshopx.adapay.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdapayWithdrawSetDistributorScopeGuardTest {

	@Test
	void storeOperator_cannotTargetOtherDistributor() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		assertThrows(
				ForbiddenException.class,
				() -> AdapayWithdrawSetDistributorScopeGuard.assertTargetAllowed(200L, jwt));
	}

	@Test
	void storeOperator_canTargetOwnJwtDistributor() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		assertDoesNotThrow(() -> AdapayWithdrawSetDistributorScopeGuard.assertTargetAllowed(100L, jwt));
	}

	@Test
	void scopedList_allowsListedShopAndRejectsOthers() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "staff");
		jwt.put("distributor_id", 0L);
		jwt.put(
				"distributor_ids",
				List.of(Map.of("distributor_id", "11"), Map.of("shop_id", 12L)));
		assertDoesNotThrow(() -> AdapayWithdrawSetDistributorScopeGuard.assertTargetAllowed(11L, jwt));
		assertDoesNotThrow(() -> AdapayWithdrawSetDistributorScopeGuard.assertTargetAllowed(12L, jwt));
		assertThrows(
				ForbiddenException.class,
				() -> AdapayWithdrawSetDistributorScopeGuard.assertTargetAllowed(99L, jwt));
	}

	@Test
	void admin_allowsAnyTarget() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "admin");
		jwt.put("distributor_id", 1L);
		assertDoesNotThrow(() -> AdapayWithdrawSetDistributorScopeGuard.assertTargetAllowed(999L, jwt));
	}

	@Test
	void emptyDistributorIds_withoutJwtShop_allowsAnyTarget() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "staff");
		jwt.put("distributor_id", 0L);
		assertDoesNotThrow(() -> AdapayWithdrawSetDistributorScopeGuard.assertTargetAllowed(888L, jwt));
	}
}
