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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；data 数组行（2 固定键），来源
 * {@code OpenapiEnabledLogisticsListPortImpl#listEnabled}：首项恒为 OTHER/其他，
 * 后续项取平台物流字典（logistics 表，种子 seed/logistics/kuaidi.json）中该租户维度已开启的行，
 * 仅保留 corp_code/corp_name（null 值字符串化为空串），按字典行序输出。
 */
@Schema(name = "EcxLogisticsEnabledRow")
public class EcxLogisticsEnabledRowDoc {

	@Schema(
			description = "物流公司代码；首项恒为 OTHER（其他，非真实快递公司）；其余为物流字典 corp_code（如 SF/YTO）",
			example = "SF")
	public String corp_code;

	@Schema(
			description = "物流公司名称；首项恒为「其他」；其余为字典 corp_name（null 转空串）",
			example = "顺丰快递")
	public String corp_name;
}