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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；成功信封 data（{@code OpenapiThirdApiV2TradeListService#formatListStruct}）：
 * trade 表仅按租户 company_id 等值过滤（无状态/来源过滤），time_start（字符串列）倒序分页。
 */
@Schema(name = "EcxTradesGetData")
public class EcxTradesGetDataDoc {

	@Schema(description = "该租户交易单总数（company_id 等值 count，不随分页变化）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页：1 是 / 0 否（ceil(total/page_size) ≤ page 即 1）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显")
	public EcxTradesGetPagerDoc pager;

	@ArraySchema(
			arraySchema = @Schema(description = "交易单行（time_start 倒序，LIMIT page_size OFFSET (page-1)*page_size）；无数据为 []"),
			schema = @Schema(implementation = EcxTradesGetRowDoc.class))
	public List<EcxTradesGetRowDoc> list;
}