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

package cn.shopex.ecshopx.aftersales.integration;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefuseWxaNotifyService;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAutoRefuseWxaTemplatePort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AftersalesAutoRefuseWxaTemplatePortImpl implements AftersalesAutoRefuseWxaTemplatePort {

	private static final String AUTO_REFUSE_REASON = "未收到商品自动驳回";

	private final AftersalesRefuseWxaNotifyService aftersalesRefuseWxaNotifyService;

	public AftersalesAutoRefuseWxaTemplatePortImpl(
			AftersalesRefuseWxaNotifyService aftersalesRefuseWxaNotifyService) {
		this.aftersalesRefuseWxaNotifyService = aftersalesRefuseWxaNotifyService;
	}

	@Override
	public void sendSellerRefuseBuyer(Map<String, Object> templateData) {
		Map<String, Object> copy =
				templateData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(templateData);
		if (!StringUtils.hasText(stringVal(copy.get("refuse_reason")))) {
			copy.put("refuse_reason", AUTO_REFUSE_REASON);
		}
		aftersalesRefuseWxaNotifyService.sendSellerRefuseBuyer(copy);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
