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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；list 行（5 键）。{@code OpenapiThirdApiV2ItemBrandListService}：
 * items_attributes（attribute_type=brand）created 倒序，zh-CN 分表覆盖 attribute_name。
 */
@Schema(name = "EcxItemBrandGetItem")
public class EcxItemBrandGetItemDoc {

	@Schema(description = "品牌 ID（items_attributes.attribute_id，整型）", example = "1")
	public Long brand_id;

	@Schema(description = "品牌名称（zh-CN 分表非空覆盖）", example = "FixtureBrand")
	public String brand_name;

	@Schema(description = "品牌 Logo URL（可为 null）", nullable = true, example = "/fixtures/logo.png")
	public String image_url;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（系统时区；库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String created;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String updated;
}