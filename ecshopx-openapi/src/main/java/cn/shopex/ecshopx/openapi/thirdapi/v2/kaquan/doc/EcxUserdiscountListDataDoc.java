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
 * 仅 OpenAPI 文档；成功信封 data。
 * {@code OpenapiThirdApiV2DiscountCardGetUserDiscountListService#execute}：
 * kaquan_user_discount 按租户+会员（rel_distributor_ids='all' 平台券，code 可选精确过滤），
 * end_date 升序、id 降序分页；行经 PresentationService 精简为 7 键。
 */
@Schema(name = "EcxUserdiscountListData")
public class EcxUserdiscountListDataDoc {

	@Schema(description = "会员平台券总数（COUNT DISTINCT id）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页：1 是 / 0 否（ceil(total/page_size) ≤ page 即 1）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显")
	public EcxUserdiscountListPagerDoc pager;

	@ArraySchema(
			arraySchema = @Schema(description = "会员优惠券行（end_date 升序、id 降序，7 键精简形态）；无数据为 []"),
			schema = @Schema(implementation = EcxUserdiscountListItemDoc.class))
	public List<EcxUserdiscountListItemDoc> list;
}