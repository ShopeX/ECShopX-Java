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

import cn.shopex.ecshopx.common.port.aftersales.AftersalesAutoRefuseWxaTemplatePort;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 小程序模板 I/O 占位；业务编排与 templateData 组装在 AftersalesService。
 */
@Component
@Slf4j
public class AftersalesAutoRefuseWxaTemplatePortImpl implements AftersalesAutoRefuseWxaTemplatePort {

	@Override
	public void sendSellerRefuseBuyer(Map<String, Object> templateData) {
		log.info(
				"wxa auto refuse template (placeholder) aftersales_bn={} order_id={} refuse_reason={}",
				templateData.get("aftersales_bn"),
				templateData.get("order_id"),
				templateData.get("refuse_reason"));
	}
}
