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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1CouponUserConsumeCardService#openApiUserConsumeCardUpdate} 返回 {@code Map.of("status", true)}。
 * 记录在事务内二次查询不存在时返回 {@code Map.of("status", false)}（信封仍为 success/E0000）。
 */
@Schema(name = "EcxCouponVerifyData", description = "状态更新结果", example = "{\"status\":true}")
public class EcxCouponVerifyDataDoc {

	@Schema(
			description =
					"更新结果：true 已更新；false 为事务内二次查询未找到该券（并发删除等边缘情况，信封仍为成功）",
			example = "true")
	public Boolean status;
}