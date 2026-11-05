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
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-10906：店铺端不得通过 distributor_name 扩大 AdaPay 交易导出范围。 */
class AdapayTradeDistributorNameScopeGuardTest {

	@Test
	void distributorOperator_isRejected() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);

		assertThrows(
				ForbiddenException.class,
				() -> AdapayTradeDistributorNameScopeGuard.assertAllowed(jwt));
	}

	@Test
	void distributorOperator_isRejected_regardlessOfCase() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", " Distributor ");

		assertThrows(
				ForbiddenException.class,
				() -> AdapayTradeDistributorNameScopeGuard.assertAllowed(jwt));
	}

	@Test
	void adminStaffAndDealerOperators_areAllowed() {
		for (String type : new String[] {"admin", "staff", "dealer"}) {
			Map<String, Object> jwt = new LinkedHashMap<>();
			jwt.put("operator_type", type);
			assertDoesNotThrow(() -> AdapayTradeDistributorNameScopeGuard.assertAllowed(jwt));
		}
	}

	@Test
	void missingOperatorType_isAllowed() {
		assertDoesNotThrow(() -> AdapayTradeDistributorNameScopeGuard.assertAllowed(new LinkedHashMap<>()));
		assertDoesNotThrow(() -> AdapayTradeDistributorNameScopeGuard.assertAllowed(null));
	}
}
