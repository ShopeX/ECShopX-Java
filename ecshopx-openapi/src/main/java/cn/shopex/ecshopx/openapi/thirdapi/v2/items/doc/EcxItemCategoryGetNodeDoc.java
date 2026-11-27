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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；分类树节点（9 固定键 + 条件键 children）。
 * {@code OpenapiThirdApiV2ItemCategoryListService#formatCategory}：标量字符串化（null→""），
 * created/updated 格式化（库值 null 时为 null）；children 仅非空时出现，递归同构。
 */
@Schema(name = "EcxItemCategoryGetNode")
public class EcxItemCategoryGetNodeDoc {

	@Schema(description = "分类 ID（字符串形态；null 回退空串）", example = "1")
	public String category_id;

	@Schema(description = "分类名称（zh-CN 分表非空覆盖；null 回退空串）", example = "聚合测试类目")
	public String category_name;

	@Schema(description = "层级（字符串形态；一级 1，子级递增）", example = "1")
	public String category_level;

	@Schema(description = "上级分类 ID（字符串形态；一级为 \"0\"）", example = "0")
	public String parent_id;

	@Schema(description = "路径（祖先 ID 逗号串，含自身；null 回退空串）", example = "1")
	public String path;

	@Schema(description = "排序值（字符串形态）", example = "1")
	public String sort;

	@Schema(description = "分类图片 URL（null 回退空串）", example = "/fixtures/logo.png")
	public String image_url;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（系统时区；库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String created;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String updated;

	// 递归树：字段类型勿用自身（Jackson/springdoc 会 StackOverflow）；契约用 $ref 指向同名 schema
	@ArraySchema(
			arraySchema = @Schema(description = "子分类节点（条件键：仅有子级时出现，递归同构；叶子节点无此键）"),
			schema = @Schema(ref = "#/components/schemas/EcxItemCategoryGetNode"))
	public List<Object> children;
}
