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

package cn.shopex.ecshopx.goods.mapper;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条作用于 {@code items} 别名 {@code i} 的动态条件（列名已在构建阶段白名单校验）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemsWxappFacetCondition {

	private String column;
	private String kind;
	private Object singleValue;
	private List<Object> listValues;

	public static ItemsWxappFacetCondition eq(String column, Object value) {
		return new ItemsWxappFacetCondition(column, "EQ", value, null);
	}

	public static ItemsWxappFacetCondition neq(String column, Object value) {
		return new ItemsWxappFacetCondition(column, "NEQ", value, null);
	}

	public static ItemsWxappFacetCondition inList(String column, List<Object> values) {
		return new ItemsWxappFacetCondition(column, "IN", null, values);
	}

	public static ItemsWxappFacetCondition likeContains(String column, String raw) {
		return new ItemsWxappFacetCondition(column, "LIKE", raw, null);
	}

	public static ItemsWxappFacetCondition gt(String column, Object value) {
		return new ItemsWxappFacetCondition(column, "GT", value, null);
	}

	public static ItemsWxappFacetCondition lt(String column, Object value) {
		return new ItemsWxappFacetCondition(column, "LT", value, null);
	}

	public static ItemsWxappFacetCondition gte(String column, Object value) {
		return new ItemsWxappFacetCondition(column, "GTE", value, null);
	}

	public static ItemsWxappFacetCondition lte(String column, Object value) {
		return new ItemsWxappFacetCondition(column, "LTE", value, null);
	}
}
