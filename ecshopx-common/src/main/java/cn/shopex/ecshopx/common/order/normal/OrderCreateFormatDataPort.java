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

package cn.shopex.ecshopx.common.order.normal;

public interface OrderCreateFormatDataPort {

	void format(NormalOrderCreateParams p);

	/**
	 * 储值（pay_type=deposit）余额校验：仅在真实创建订单路径调用。
	 * 结算页预览（wxapp getOrderTempInfo / shopadmin checkout）不得调用，
	 * 余额不足由提交订单时拦截（ECX-10469）。
	 */
	void assertDepositBalance(NormalOrderCreateParams p);

	void applyCheckoutCouponAfterFreight(NormalOrderCreateParams p);
}
