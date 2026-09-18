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

package cn.shopex.ecshopx.goods.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 分类树节点（与 Admin API 表单 JSON 字段一致，蛇形命名）。
 */
@Data
public class CategoryTreeNode {

	private Long categoryId;

	/** 当 JSON 中出现 {@code category_id} 键时为 true（含 null），用于校验规则。 */
	private boolean categoryIdKeyPresent;

	private String categoryName;

	private String categoryCode;

	private Boolean isMainCategory;

	private Integer isShowFront;

	private Long sort;

	private JsonNode goodsParams;

	private JsonNode goodsSpec;

	private String imageUrl;

	private Long categoryIdTaobao;

	private Long parentIdTaobao;

	private JsonNode taobaoCategoryInfo;

	private Long customizePageId;

	private boolean customizePageIdKeyPresent;

	private List<CategoryTreeNode> children;

	/** 递归写入时由 SaveService 注入，对应合并后的 parent_id。 */
	private long mergedParentId;

	/** 合并后的行是否包含 category_id 键（用于 distributor 解析）。 */
	private boolean mergedCategoryIdKeyPresent;

	/** 合并后的行是否包含 parent_id 键。 */
	private boolean mergedParentIdKeyPresent;

	/** 调用解析前待写入的 distributor_id（通常为 JWT 中的值）。 */
	private long pendingDistributorIdBeforeResolve;

	public List<CategoryTreeNode> getChildrenOrEmpty() {
		return children != null ? children : List.of();
	}

	public static List<CategoryTreeNode> fromJsonArray(JsonNode array) {
		if (array == null || !array.isArray()) {
			return List.of();
		}
		List<CategoryTreeNode> out = new ArrayList<>();
		for (JsonNode el : array) {
			if (el.isObject()) {
				out.add(fromJsonObject(el));
			}
		}
		return out;
	}

	public static CategoryTreeNode fromJsonObject(JsonNode n) {
		CategoryTreeNode o = new CategoryTreeNode();
		if (n.has("category_id")) {
			o.categoryIdKeyPresent = true;
			JsonNode id = n.get("category_id");
			if (id != null && !id.isNull() && id.isNumber()) {
				o.categoryId = id.longValue();
			} else if (id != null && !id.isNull() && id.isTextual()) {
				String s = id.asText();
				if (!s.isEmpty()) {
					o.categoryId = Long.parseLong(s);
				}
			}
		}
		if (n.has("category_name")) {
			JsonNode v = n.get("category_name");
			if (v != null && !v.isNull()) {
				o.categoryName = v.asText();
			}
		}
		if (n.has("category_code")) {
			JsonNode v = n.get("category_code");
			if (v != null && !v.isNull()) {
				o.categoryCode = v.asText();
			}
		}
		if (n.has("is_main_category")) {
			JsonNode v = n.get("is_main_category");
			if (v != null && !v.isNull()) {
				o.isMainCategory = v.asBoolean();
			}
		}
		if (n.has("is_show_front")) {
			JsonNode v = n.get("is_show_front");
			if (v != null && !v.isNull()) {
				if (v.isNumber()) {
					o.isShowFront = v.intValue();
				} else {
					o.isShowFront = v.asBoolean() ? 1 : 0;
				}
			}
		}
		if (n.has("sort")) {
			JsonNode v = n.get("sort");
			if (v != null && !v.isNull()) {
				if (v.isNumber()) {
					o.sort = v.longValue();
				} else if (v.isTextual()) {
					try {
						o.sort = Long.parseLong(v.asText().trim());
					} catch (NumberFormatException ignored) {
						// 保持 null，由后续校验处理
					}
				}
			}
		}
		if (n.has("goods_params")) {
			o.goodsParams = n.get("goods_params");
		}
		if (n.has("goods_spec")) {
			o.goodsSpec = n.get("goods_spec");
		}
		if (n.has("image_url")) {
			JsonNode v = n.get("image_url");
			if (v != null && !v.isNull()) {
				o.imageUrl = v.asText();
			}
		}
		if (n.has("category_id_taobao")) {
			JsonNode v = n.get("category_id_taobao");
			if (v != null && !v.isNull() && v.isNumber()) {
				o.categoryIdTaobao = v.longValue();
			}
		}
		if (n.has("parent_id_taobao")) {
			JsonNode v = n.get("parent_id_taobao");
			if (v != null && !v.isNull() && v.isNumber()) {
				o.parentIdTaobao = v.longValue();
			}
		}
		if (n.has("taobao_category_info")) {
			o.taobaoCategoryInfo = n.get("taobao_category_info");
		}
		if (n.has("customize_page_id")) {
			o.setCustomizePageIdKeyPresent(true);
			JsonNode v = n.get("customize_page_id");
			if (v != null && !v.isNull() && v.isNumber()) {
				o.customizePageId = v.longValue();
			}
		}
		if (n.has("children") && n.get("children").isArray()) {
			o.children = new ArrayList<>();
			for (JsonNode c : n.get("children")) {
				if (c.isObject()) {
					o.children.add(fromJsonObject(c));
				}
			}
		}
		return o;
	}
}
