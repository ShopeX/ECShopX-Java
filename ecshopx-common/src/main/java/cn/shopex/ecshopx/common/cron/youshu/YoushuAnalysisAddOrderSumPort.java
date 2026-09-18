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

package cn.shopex.ecshopx.common.cron.youshu;

/**
 * 有数 analysis：{@code POST /data-api/v1/order/add_order_sum}，{@code orders} 为单元素数组。
 */
public interface YoushuAnalysisAddOrderSumPort {

	void addOrderSum(String dataSourceId, YoushuOrderSumRow orderSum, YoushuOpenApiCredentials credentials);
}
