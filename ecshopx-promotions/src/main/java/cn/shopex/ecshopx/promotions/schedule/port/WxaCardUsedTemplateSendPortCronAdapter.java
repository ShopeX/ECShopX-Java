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

package cn.shopex.ecshopx.promotions.schedule.port;

import cn.shopex.ecshopx.common.cron.WxaCardUsedTemplateSendCronPort;
import cn.shopex.ecshopx.wechat.service.WxaCardUsedTemplateSendPort;
import java.util.Map;

/** test-cron：将 common 窄 Port 适配为 wechat 模块的 {@link WxaCardUsedTemplateSendPort}。 */
public final class WxaCardUsedTemplateSendPortCronAdapter implements WxaCardUsedTemplateSendPort {

	private final WxaCardUsedTemplateSendCronPort cronPort;

	public WxaCardUsedTemplateSendPortCronAdapter(WxaCardUsedTemplateSendCronPort cronPort) {
		this.cronPort = cronPort;
	}

	@Override
	public void send(long companyId, long userId, String templateScene, Map<String, String> keywordData) {
		cronPort.send(companyId, userId, templateScene, keywordData);
	}
}
