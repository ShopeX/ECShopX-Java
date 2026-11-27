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
 * 仅 OpenAPI 文档；成功信封。Handler 返回 Map（或 mobile 查无会员时的空 List），
 * {@code OpenapiResponseBodyAdvice} 包装为 {@code OpenapiEnvelope.success(data)}（message 固定「成功」）。
 * data 结构与增量订单搜索接口完全一致（复用 {@link EcxOrdersIncrGetDataDoc}：
 * formatOrderListStruct 4 固定键 + 行 22 键 + 优惠明细 5 键）；本接口按 create_time 过滤与排序。
 * mobile 查无会员时 data 为 []（见空数组示例）。
 */
@Schema(name = "EcxOrdersSoldGetSuccessEnvelope")
public class EcxOrdersSoldGetSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Advice 包装固定「成功」）", example = "成功")
	public String message;

	@Schema(description = "分页数据（结构同增量订单搜索；mobile 查无会员时为空数组 []，非本结构）")
	public EcxOrdersIncrGetDataDoc data;
}