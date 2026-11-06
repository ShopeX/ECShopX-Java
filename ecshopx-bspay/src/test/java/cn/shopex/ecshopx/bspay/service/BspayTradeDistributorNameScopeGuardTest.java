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

package cn.shopex.ecshopx.bspay.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-10939：仅门店身份禁止使用店名覆盖数据范围。 */
class BspayTradeDistributorNameScopeGuardTest {

	@Test
	void distributorOperator_isRejectedRegardlessOfCase() {
		assertThatThrownBy(() -> BspayTradeDistributorNameScopeGuard.assertAllowed(
						Map.of("operator_type", " Distributor ")))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("无权筛选其他店铺");
	}

	@Test
	void otherOperatorsAndMissingType_areAllowed() {
		assertThatCode(() -> {
					BspayTradeDistributorNameScopeGuard.assertAllowed(Map.of("operator_type", "admin"));
					BspayTradeDistributorNameScopeGuard.assertAllowed(Map.of("operator_type", "merchant"));
					BspayTradeDistributorNameScopeGuard.assertAllowed(Map.of());
					BspayTradeDistributorNameScopeGuard.assertAllowed(null);
				})
				.doesNotThrowAnyException();
	}
}
