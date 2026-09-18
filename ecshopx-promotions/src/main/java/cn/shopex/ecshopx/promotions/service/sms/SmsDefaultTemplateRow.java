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

package cn.shopex.ecshopx.promotions.service.sms;

import java.util.Map;

/**
 * 内置默认短信模板的一行（与历史默认单条结构对应）。
 * builtinIsOpen：默认行中的 is_open 元数据；插入 DB 时 is_open 以请求为准。
 */
public record SmsDefaultTemplateRow(
		String content,
		String tmplType,
		String smsType,
		String tmplName,
		Map<String, Object> sendTimeDesc,
		Boolean builtinIsOpen) {}
