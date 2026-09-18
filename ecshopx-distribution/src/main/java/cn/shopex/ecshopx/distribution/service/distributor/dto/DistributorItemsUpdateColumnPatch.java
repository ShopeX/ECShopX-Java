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

package cn.shopex.ecshopx.distribution.service.distributor.dto;

import java.util.Optional;

public record DistributorItemsUpdateColumnPatch(
		Optional<Boolean> isCanSale,
		Optional<Boolean> isTotalStore,
		Optional<Long> store,
		Optional<Long> price) {

	public static DistributorItemsUpdateColumnPatch empty() {
		return new DistributorItemsUpdateColumnPatch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
	}

	public boolean anyPresent() {
		return isCanSale.isPresent()
				|| isTotalStore.isPresent()
				|| store.isPresent()
				|| price.isPresent();
	}
}
