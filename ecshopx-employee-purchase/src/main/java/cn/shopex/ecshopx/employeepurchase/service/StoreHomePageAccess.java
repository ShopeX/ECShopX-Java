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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.StoreHomePage;

/** 内购模版经销商隔离校验 */
public final class StoreHomePageAccess {

	private StoreHomePageAccess() {}

	public static void assertRowMatchesDealer(StoreHomePage row, int authDistributorId) {
		if (authDistributorId > 0 && (row.getDistributorId() == null ? 0 : row.getDistributorId()) != authDistributorId) {
			throw new ResourceException("未查询到数据");
		}
	}
}
