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

package cn.shopex.ecshopx.common.port.promotions;

/**
 * 对位 PHP {@code BargainNormalOrderService::changeOrderActivityStatus}：回写助力用户单与活动下单数。
 */
public interface BargainOrderActivityStatusPort {

	/**
	 * @param state 1=标记已下单（order_num+1）；0=退款/取消回退（is_ordered→0，order_num-1）
	 */
	void changeOrderActivityStatus(long userId, long bargainId, int state);
}
