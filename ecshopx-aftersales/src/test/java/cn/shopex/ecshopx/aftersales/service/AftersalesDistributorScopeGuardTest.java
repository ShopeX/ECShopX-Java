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

package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AftersalesDistributorScopeGuardTest {

	@Test
	void storeOperator_cannotReviewOtherShopAftersales() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		assertThrows(
				ForbiddenException.class,
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(200L, jwt));
	}

	@Test
	void storeOperator_canReviewOwnShopAftersales() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		assertDoesNotThrow(
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(100L, jwt));
	}

	@Test
	void scopedList_unionJwtShop_allowsListedAndRejectsOthers() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "staff");
		jwt.put("distributor_id", 11L);
		jwt.put("distributor_ids", List.of(Map.of("distributor_id", "12"), Map.of("shop_id", 13L)));
		assertDoesNotThrow(
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(11L, jwt));
		assertDoesNotThrow(
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(12L, jwt));
		assertDoesNotThrow(
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(13L, jwt));
		assertThrows(
				ForbiddenException.class,
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(99L, jwt));
	}

	@Test
	void admin_allowsAnyShop() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "admin");
		jwt.put("distributor_id", 1L);
		assertDoesNotThrow(
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(999L, jwt));
	}

	@Test
	void emptyAllowSet_allowsAnyShop() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "staff");
		jwt.put("distributor_id", 0L);
		assertDoesNotThrow(
				() -> AftersalesDistributorScopeGuard.assertAftersalesDistributorAllowed(888L, jwt));
	}
}
