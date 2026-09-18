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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderTypeRegistry {

	private final Map<String, WxappOrderTypeCreateStrategy> strategyBySpringBeanName;

	public WxappOrderTypeRegistry(Map<String, WxappOrderTypeCreateStrategy> strategyBySpringBeanName) {
		this.strategyBySpringBeanName = strategyBySpringBeanName;
	}

	public Map<String, Object> create(WxappOrderCreateContext ctx) {
		String raw = String.valueOf(ctx.getParams().getOrDefault("order_type", ""));
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("无此类型订单！");
		}
		String key = raw.trim().toLowerCase(Locale.ROOT);
		String bean = resolveBeanName(key);
		if (!StringUtils.hasText(bean)) {
			throw new ResourceException("无此类型订单！");
		}
		WxappOrderTypeCreateStrategy strategy = strategyBySpringBeanName.get(bean);
		if (strategy == null) {
			throw new ResourceException("无此类型订单！");
		}
		return strategy.create(ctx);
	}

	public Map<String, Object> getOrderTempInfo(WxappOrderCreateContext ctx) {
		String raw = String.valueOf(ctx.getParams().getOrDefault("order_type", ""));
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("无此类型订单！");
		}
		String key = raw.trim().toLowerCase(Locale.ROOT);
		String bean = resolveBeanName(key);
		if (!StringUtils.hasText(bean)) {
			throw new ResourceException("无此类型订单！");
		}
		WxappOrderTypeCreateStrategy strategy = strategyBySpringBeanName.get(bean);
		if (strategy == null) {
			throw new ResourceException("无此类型订单！");
		}
		return strategy.getOrderTempInfo(ctx);
	}

	private static String resolveBeanName(String orderTypeLower) {
		return switch (orderTypeLower) {
			case "service" -> "wxappOrderTypeCreateService";
			case "bargain" -> "wxappOrderTypeCreateBargain";
			case "normal_bargain" -> "wxappOrderTypeCreateNormalBargain";
			case "normal" -> "wxappOrderTypeCreateNormal";
			case "supplier_order" -> "wxappOrderTypeCreateSupplierOrder";
			case "service_groups", "groups" -> "wxappOrderTypeCreateServiceGroups";
			case "normal_groups" -> "wxappOrderTypeCreateNormalGroups";
			case "membercard" -> "wxappOrderTypeCreateMembercard";
			case "normal_seckill" -> "wxappOrderTypeCreateNormalSeckill";
			case "service_seckill" -> "wxappOrderTypeCreateServiceSeckill";
			case "normal_drug" -> "wxappOrderTypeCreateNormalDrug";
			case "normal_shopguide" -> "wxappOrderTypeCreateNormalShopguide";
			case "normal_pointsmall" -> "wxappOrderTypeCreateNormalPointsmall";
			case "normal_excard" -> "wxappOrderTypeCreateNormalExcard";
			case "normal_community" -> "wxappOrderTypeCreateNormalCommunity";
			case "normal_shopadmin" -> "wxappOrderTypeCreateNormalShopadmin";
			case "normal_employee_purchase" -> "wxappOrderTypeCreateNormalEmployeePurchase";
			default -> null;
		};
	}
}
