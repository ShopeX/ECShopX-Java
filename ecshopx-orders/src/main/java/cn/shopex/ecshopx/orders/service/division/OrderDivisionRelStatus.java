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

package cn.shopex.ecshopx.orders.service.division;

/**
 * 与 PHP {@code OrdersRelChinaumspayDivisionService} 常量化名一致；表字段为 {@code
 * varchar}。
 */
public final class OrderDivisionRelStatus {

	public static final String READY = "0";
	public static final String UPLOADED = "1";
	public static final String SKIP = "2";

	private OrderDivisionRelStatus() {}
}
