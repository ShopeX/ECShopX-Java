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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源 {@code OpenapiThirdApiV2DistributorListService#formatListStruct}。 */
@Schema(name = "EcxDistributorListData")
public class EcxDistributorListDataDoc {

	@Schema(description = "符合条件的总记录数", example = "42")
	public Long total_count;

	@Schema(description = "是否末页：1 是，0 否（由 total_count 与 page/page_size 计算）", example = "0")
	public Integer is_last_page;

	@Schema(description = "分页元信息")
	public PagerDoc pager;

	@ArraySchema(arraySchema = @Schema(description = "店铺列表；当前实现未按 page 切片，返回全部匹配行"))
	public List<EcxDistributorListItemDoc> list;

	@Schema(name = "EcxDistributorListPager")
	public static class PagerDoc {

		@Schema(description = "当前页码", example = "1")
		public Integer page;

		@Schema(description = "每页条数", example = "20")
		public Integer page_size;
	}
}