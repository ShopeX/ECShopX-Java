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
 * 仅 OpenAPI 文档；{@code data.list} 单条券模板，字段来源
 * {@code OpenapiThirdApiV1CouponGetCouponListService#executeOpenapiGetCouponList}（select 六列，created 倒序）。
 */
@Schema(name = "EcxCouponListItem")
public class EcxCouponListItemDoc {

	@Schema(description = "卡券模板 ID（discount_cards.card_id）", example = "3")
	public Long card_id;

	@Schema(description = "卡券名；数据库为 null 时返回空串", example = "满减券")
	public String title;

	@Schema(description = "卡券使用说明；数据库为 null 时返回空串", example = "")
	public String description;

	@Schema(description = "折扣券打折额度（百分比）；数据库为 null 时返回 0", example = "0")
	public Integer discount;

	@Schema(
			description =
					"适用店铺 ID 串，逗号包裹格式（如 \",1,2,\"）；\",\" 表示全部店铺；数据库为 null 时返回 \",\"",
			example = ",")
	public String distributor_id;

	@Schema(
			description = "卡券类型：discount 折扣券；cash 代金券；gift 兑换券；new_gift 兑换券(新)",
			example = "cash")
	public String card_type;
}