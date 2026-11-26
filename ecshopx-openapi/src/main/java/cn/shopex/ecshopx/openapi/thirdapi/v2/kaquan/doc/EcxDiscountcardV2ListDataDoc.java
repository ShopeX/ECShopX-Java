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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；成功信封 data。{@code OpenapiThirdApiV2DiscountCardGetDiscountCardListService#execute}：
 * kaquan_discount_cards 按租户全量（无类型/状态过滤），created 倒序分页。
 */
@Schema(name = "EcxDiscountcardV2ListData")
public class EcxDiscountcardV2ListDataDoc {

	@Schema(description = "租户卡券总数", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页：1 是 / 0 否（ceil(total/page_size) ≤ page 即 1）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显")
	public EcxDiscountcardV2ListPagerDoc pager;

	@ArraySchema(
			arraySchema = @Schema(description = "卡券行（created 倒序，15 键精简形态）；无数据为 []"),
			schema = @Schema(implementation = EcxDiscountcardV2ListItemDoc.class))
	public List<EcxDiscountcardV2ListItemDoc> list;
}