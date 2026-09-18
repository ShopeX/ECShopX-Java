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

package cn.shopex.ecshopx.orders.service.front.wxapp.strategy;

import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderCreateContext;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderTypeCreateStrategy;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappPhysicalNormalOrderCreateSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("wxappOrderTypeCreateNormalDrug")
public class WxappNormalDrugOrderCreateStrategy implements WxappOrderTypeCreateStrategy {

	private final WxappPhysicalNormalOrderCreateSupport wxappPhysicalNormalOrderCreateSupport;

	public WxappNormalDrugOrderCreateStrategy(WxappPhysicalNormalOrderCreateSupport wxappPhysicalNormalOrderCreateSupport) {
		this.wxappPhysicalNormalOrderCreateSupport = wxappPhysicalNormalOrderCreateSupport;
	}

	@Override
	public Map<String, Object> create(WxappOrderCreateContext ctx) {
		return wxappPhysicalNormalOrderCreateSupport.create(ctx, "normal_drug");
	}

	@Override
	public Map<String, Object> getOrderTempInfo(WxappOrderCreateContext ctx) {
		return wxappPhysicalNormalOrderCreateSupport.getOrderTempInfo(ctx, "normal_drug");
	}
}
