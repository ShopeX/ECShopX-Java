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

package cn.shopex.ecshopx.companys.service.operatorcart;

import cn.shopex.ecshopx.common.exception.ResourceException;

/** Aligns with PHP {@code OperatorFastBuyStockValidator}. */
public final class OperatorFastBuyStockValidator {

	private OperatorFastBuyStockValidator() {
	}

	public static void validate(int shopStock, int platformStock, int num) {
		validate(shopStock, platformStock, num, false);
	}

	public static void validate(int shopStock, int platformStock, int num, boolean skipShopStockCashierGate) {
		if (num <= 0) {
			throw new ResourceException("购买数量有误");
		}
		if (!skipShopStockCashierGate && shopStock > 0) {
			throw new ResourceException("当前商品门店有库存，请从收银加购");
		}
		if (platformStock < num) {
			throw new ResourceException("库存不足");
		}
	}
}
