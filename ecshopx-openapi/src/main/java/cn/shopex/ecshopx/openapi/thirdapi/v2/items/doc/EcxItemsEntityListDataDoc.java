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
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 分页结构，来源
 * {@code OpenapiThirdApiV2MemberTagListService#formatListStruct}（4 固定键）。
 * is_last_page 为整型 0/1（ceil(total_count/page_size) ≤ page → 1）。
 */
@Schema(name = "EcxItemsEntityListData")
public class EcxItemsEntityListDataDoc {

	@Schema(description = "满足过滤条件的 SPU 总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public EcxItemBrandGetPagerDoc pager;

	@Schema(description = "SPU 行数组（item_id 降序；无结果为空数组）")
	public List<EcxItemsEntityListRowDoc> list;
}