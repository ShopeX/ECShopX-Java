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

package cn.shopex.ecshopx.adapay.api.admin.v1.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；支付宝行业类目树节点，对应运行时
 * {@code AdapayAlipayIndustryCategoryTreeNodeDto}（SnakeCase 序列化）。
 * 自引用 children 在文档侧以 {@code List<Object>} + ref 表达，避免 springdoc 递归栈溢出。
 */
@Schema(name = "AdapayAlipayIndustryCatNode", description = "支付宝行业类目树节点（snake_case 序列化）")
public class AdapayAlipayIndustryCatNodeDoc {

	@Schema(description = "分类 id（adapay_alipay_industry_category.id）", example = "1")
	public Long id;

	@Schema(description = "分类名称（种子数据部分名称含尾随空格，原样返回）", example = "零售业 ")
	public String category_name;

	@Schema(description = "父级分类 id，0 为顶级", example = "0")
	public Long parent_id;

	@Schema(description = "分类等级：1 顶级 / 2 中间层 / 3 叶子（种子数据为三级树）", example = "1")
	public Integer category_level;

	@Schema(
			description = "支付宝行业分类 ID；一、二级节点为 null，仅三级叶子有值",
			nullable = true,
			example = "207")
	public Long alipay_cls_id;

	@Schema(
			description = "支付宝经营类目（MCC 风格编码字符串）；一、二级节点为 null，仅三级叶子有值",
			nullable = true,
			example = "5311")
	public String alipay_category_id;

	@ArraySchema(
			schema = @Schema(ref = "#/components/schemas/AdapayAlipayIndustryCatNode"),
			arraySchema =
					@Schema(
							description =
									"子分类列表（同结构递归）。category_level=3 时字段整体省略（NON_NULL）；"
											+ "其余层级恒有该键，无子分类时为 []"))
	public List<Object> children;
}