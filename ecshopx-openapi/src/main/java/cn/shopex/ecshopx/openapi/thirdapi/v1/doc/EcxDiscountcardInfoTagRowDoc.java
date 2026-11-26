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
 * 仅 OpenAPI 文档；{@code data.tag_list} 单条，来源
 * {@code DiscountCardKaquanDetailFacadeService#itemsTagToRow}（tag_id/company_id/distributor_id 非 null 才写键）。
 */
@Schema(name = "EcxDiscountcardInfoTagRow")
public class EcxDiscountcardInfoTagRowDoc {

	@Schema(description = "商品标签 ID", example = "1")
	public Long tag_id;

	@Schema(description = "租户/公司 ID", example = "100")
	public Long company_id;

	@Schema(description = "标签名称", nullable = true)
	public String tag_name;

	@Schema(description = "标签背景色", example = "#ff1939")
	public String tag_color;

	@Schema(description = "所属店铺 ID；0 表示平台标签", example = "0")
	public Long distributor_id;

	@Schema(description = "字体颜色", example = "#ffffff")
	public String font_color;

	@Schema(description = "标签描述", nullable = true)
	public String description;

	@Schema(description = "标签图标", nullable = true)
	public String tag_icon;

	@Schema(description = "前台是否展示", nullable = true)
	public Integer front_show;

	@Schema(description = "创建时间（Unix 秒）", nullable = true)
	public Integer created;

	@Schema(description = "更新时间（Unix 秒）", nullable = true)
	public Integer updated;

	@Schema(description = "所属店铺名称；平台标签固定「平台」", example = "平台")
	public String distributor_name;

	@Schema(description = "是否平台标签（distributor_id≤0 时 true）", example = "true")
	public Boolean is_platform;
}