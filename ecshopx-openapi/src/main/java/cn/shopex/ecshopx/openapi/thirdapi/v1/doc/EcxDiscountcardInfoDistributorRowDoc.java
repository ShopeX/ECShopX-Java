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
 * 仅 OpenAPI 文档；{@code data.distributor_info} 单条，来源
 * {@code DiscountCardKaquanDetailFacadeService#distributorToSnakeRow}（ID 非 null 才写键）。
 */
@Schema(name = "EcxDiscountcardInfoDistributorRow")
public class EcxDiscountcardInfoDistributorRowDoc {

	@Schema(description = "店铺 ID", example = "5")
	public Long distributor_id;

	@Schema(description = "租户/公司 ID", example = "100")
	public Long company_id;

	@Schema(description = "店铺名称", nullable = true)
	public String name;

	@Schema(description = "联系电话", nullable = true)
	public String mobile;

	@Schema(description = "详细地址", nullable = true)
	public String address;

	@Schema(description = "店铺 logo", nullable = true)
	public String logo;

	@Schema(description = "省", nullable = true)
	public String province;

	@Schema(description = "市", nullable = true)
	public String city;

	@Schema(description = "区", nullable = true)
	public String area;

	@Schema(description = "门店 ID", nullable = true)
	public Long shop_id;

	@Schema(description = "店铺是否有效（字符串 true/false）", example = "true")
	public String is_valid;

	@Schema(description = "创建时间（Unix 秒）", nullable = true)
	public Long created;

	@Schema(description = "更新时间（Unix 秒）", nullable = true)
	public Long updated;
}