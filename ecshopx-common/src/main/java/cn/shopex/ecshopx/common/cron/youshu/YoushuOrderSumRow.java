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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 有数 {@code /data-api/v1/order/add_order_sum} 单条 order 行；键名与 PHP 数组一致。
 */
public record YoushuOrderSumRow(
		@JsonProperty("ref_date") String refDate,
		@JsonProperty("give_order_amount_sum") double giveOrderAmountSum,
		@JsonProperty("give_order_num_sum") long giveOrderNumSum,
		@JsonProperty("payment_amount_sum") double paymentAmountSum,
		@JsonProperty("payed_num_sum") long payedNumSum) {}
