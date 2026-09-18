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

package cn.shopex.ecshopx.common.cron;

import java.util.Map;

/**
 * 计划活动 test-cron 侧微信模板发送窄 Port（与 {@code WxaCardUsedTemplateSendPort} 方法签名对齐），避免 ecshopx-common 依赖
 * wechat 模块；生产由 wechat 模块提供完整 Port，promotions 在 test-cron 下用适配器 + Noop 覆盖。
 */
public interface WxaCardUsedTemplateSendCronPort {

	void send(long companyId, long userId, String templateScene, Map<String, String> keywordData);
}
