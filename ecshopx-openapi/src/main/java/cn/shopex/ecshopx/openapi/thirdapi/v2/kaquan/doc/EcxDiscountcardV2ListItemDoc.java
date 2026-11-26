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
 * 仅 OpenAPI 文档；list 行（15 键，恒为精简形态）。
 * {@code OpenapiDiscountCardV2ListPresentationService#toSlimRow}：
 * 与 v1 列表不同——无完整行形态、金额分→元（HALF_UP 取整字符串）、时间键为 begin_time/end_time（Unix 秒字符串化，不格式化）。
 */
@Schema(name = "EcxDiscountcardV2ListItem")
public class EcxDiscountcardV2ListItemDoc {

	@Schema(description = "卡券模板 ID（整型化；非法按 0）", example = "3")
	public Integer card_id;

	@Schema(description = "卡券类型：cash 代金券 / discount 折扣券 / gift 兑换券 / new_gift 兑换券(新)；null 回退 \"0\"", example = "cash")
	public String card_type;

	@Schema(description = "卡券名（null 回退空串）", example = "满减券")
	public String title;

	@Schema(description = "卡券使用说明（字符串化；null 回退 \"0\"——PHP 旧契约）", example = "0")
	public String description;

	@Schema(description = "折扣券打折额度（百分比，字符串化不换算；null 回退 \"0\"）", example = "0")
	public String discount;

	@Schema(description = "代金券减免金额（元，分÷100 HALF_UP 取整字符串）", example = "0")
	public String reduce_cost;

	@Schema(description = "有效期类型：DATE_TYPE_FIX_TIME_RANGE 固定区间 / DATE_TYPE_FIX_TERM 领取后固定天数；null 回退空串", example = "DATE_TYPE_FIX_TERM")
	public String date_type;

	@Schema(description = "有效期开始（begin_date Unix 秒字符串化，不格式化日期；null 回退空串）", example = "")
	public String begin_time;

	@Schema(description = "有效期结束（end_date Unix 秒字符串化；null 回退空串）", example = "")
	public String end_time;

	@Schema(description = "有效期有效天数（字符串化；null 回退空串）", example = "30")
	public String fixed_term;

	@Schema(description = "代金券起用金额（元，分÷100 HALF_UP 取整字符串）", example = "0")
	public String least_cost;

	@Schema(description = "代金券最高消费限额（元，分÷100 HALF_UP 取整字符串）", example = "999999")
	public String most_cost;

	@Schema(
			description =
					"适用范围（由 use_bound 映射）：0/其他→true 全场；1→false 指定商品；2→category 分类；"
							+ "3→tag 标签；4→brand 品牌",
			allowableValues = {"true", "false", "category", "tag", "brand"},
			example = "true")
	public String use_all_items;

	@ArraySchema(
			arraySchema =
					@Schema(
							description =
									"适用范围明细字符串数组：use_bound=1 为关联商品名列表（kaquan_rel_items item_type=normal → items.item_name）；"
											+ "=2 为 apply_scope 逗号段排序后以 / 连接的单元素；=3/4 为 apply_scope 逗号拆分原样；"
											+ "全场/空为 []"),
			schema = @Schema(type = "string"))
	public List<String> rel_data;

	@Schema(description = "剩余可发放数：max(quantity - 已领取数, 0)（已领取按 user_discount 聚合）", example = "100")
	public Integer left_quantity;
}