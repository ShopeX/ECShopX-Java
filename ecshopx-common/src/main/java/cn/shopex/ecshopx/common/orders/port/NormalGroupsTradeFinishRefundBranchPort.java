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

package cn.shopex.ecshopx.common.orders.port;

/**
 * 拼团已成团场景下，判断当前支付成功流水是否命中「禁用团员立即退款」分支（避免 orders 模块直接依赖
 * promotions 实体）。
 */
public interface NormalGroupsTradeFinishRefundBranchPort {

	/**
	 * @return 当已成团且当前团员为 disabled、且可视为拼团支付成功路径命中时为 true
	 */
	boolean shouldRefundDisabledMemberInFormedTeam(long companyId, long orderId, long userId);
}
