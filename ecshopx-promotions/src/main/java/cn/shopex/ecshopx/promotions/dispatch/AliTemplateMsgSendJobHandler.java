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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.ali.service.alitemplate.AliTemplateMsgService;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AliTemplateMsgSendJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(AliTemplateMsgSendJobHandler.class);

	private final AliTemplateMsgService aliTemplateMsgService;

	public AliTemplateMsgSendJobHandler(AliTemplateMsgService aliTemplateMsgService) {
		this.aliTemplateMsgService = aliTemplateMsgService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			boolean forceFire = true;
			Object rawFf = payload.get("is_force_fire");
			if (rawFf instanceof Boolean b) {
				forceFire = b;
			}
			Map<String, Object> biz = new LinkedHashMap<>(payload);
			biz.remove("is_force_fire");
			aliTemplateMsgService.send(biz, forceFire);
		} catch (RuntimeException e) {
			log.debug("AliTemplateMsgSend job failed: {}", e.toString());
		}
	}
}
