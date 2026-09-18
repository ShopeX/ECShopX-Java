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

package cn.shopex.ecshopx.common.order;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Locale;

/**
 * 订单创建路由 slug 与 DB 落库字段的映射工具。
 * <p>
 * slug 是 API 入参 {@code order_type}（如 {@code normal_seckill}），用于路由到具体下单策略；
 * 而 {@code orders.order_type} 列只取粗类（{@code normal} / {@code service} / {@code bargain}），
 * {@code orders.order_class} 列取业务细类（{@code seckill} / {@code groups} / {@code shopadmin} 等）。
 * <p>
 * 该映射是 wxapp / 后台代客两条创建链路与 cron、列表筛选 SQL 之间的契约：
 * 例如秒杀超时取消依赖 {@code order_type='normal' AND order_class='seckill'}，
 * 不能把 {@code normal_seckill} 直接写入 {@code order_type} 列。
 */
public final class OrderTypeSlugMapper {

	public record Resolved(String orderType, String orderClass) {}

	private OrderTypeSlugMapper() {}

	public static Resolved resolve(String slug) {
		if (slug == null) {
			throw new ResourceException("无此类型订单！");
		}
		String key = slug.trim().toLowerCase(Locale.ROOT);
		return switch (key) {
			case "normal" -> new Resolved("normal", "normal");
			case "service" -> new Resolved("service", "normal");
			case "bargain" -> new Resolved("bargain", "bargain");
			case "normal_bargain" -> new Resolved("normal", "bargain");
			case "normal_seckill" -> new Resolved("normal", "seckill");
			case "service_seckill" -> new Resolved("service", "seckill");
			case "normal_drug" -> new Resolved("normal", "drug");
			case "normal_shopguide" -> new Resolved("normal", "shopguide");
			case "normal_pointsmall" -> new Resolved("normal", "pointsmall");
			case "normal_excard" -> new Resolved("normal", "excard");
			case "normal_community" -> new Resolved("normal", "community");
			case "normal_shopadmin" -> new Resolved("normal", "shopadmin");
			case "normal_employee_purchase" -> new Resolved("normal", "employee_purchase");
			case "normal_groups" -> new Resolved("normal", "groups");
			case "service_groups", "groups" -> new Resolved("service", "groups");
			default -> throw new ResourceException("无此类型订单！");
		};
	}
}
