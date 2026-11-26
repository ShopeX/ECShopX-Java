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

package cn.shopex.ecshopx.members.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MemberTagSegmentRuleOrderItemParamsSupport")
class MemberTagSegmentRuleOrderItemParamsSupportTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void enrichCondition_expandsOrderItemParamsWithNameAndPics() {
		List<Object> condition = sampleCondition(List.of(101L, 102L));
		Map<Long, MemberTagSegmentRuleOrderItemParamsSupport.ItemView> byId = new LinkedHashMap<>();
		byId.put(101L, new MemberTagSegmentRuleOrderItemParamsSupport.ItemView("滑雪服", List.of("a.jpg")));
		byId.put(102L, new MemberTagSegmentRuleOrderItemParamsSupport.ItemView("手套", List.of("b.jpg")));

		MemberTagSegmentRuleOrderItemParamsSupport.enrichConditionInPlace(condition, byId);

		@SuppressWarnings("unchecked")
		Map<String, Object> orderItem = findOrderItem(condition);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> params = (List<Map<String, Object>>) orderItem.get("params");
		assertEquals(2, params.size());
		assertEquals(101L, params.get(0).get("item_id"));
		assertEquals("滑雪服", params.get(0).get("item_name"));
		assertEquals(List.of("a.jpg"), params.get(0).get("pics"));
		assertEquals(102L, params.get(1).get("item_id"));
		assertEquals("手套", params.get(1).get("item_name"));
	}

	@Test
	void enrichCondition_missingItemKeepsIdWithEmptyNameAndPics() {
		List<Object> condition = sampleCondition(List.of(999L));
		MemberTagSegmentRuleOrderItemParamsSupport.enrichConditionInPlace(condition, Map.of());

		@SuppressWarnings("unchecked")
		Map<String, Object> orderItem = findOrderItem(condition);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> params = (List<Map<String, Object>>) orderItem.get("params");
		assertEquals(1, params.size());
		assertEquals(999L, params.get(0).get("item_id"));
		assertEquals("", params.get(0).get("item_name"));
		assertInstanceOf(List.class, params.get(0).get("pics"));
		assertTrue(((List<?>) params.get(0).get("pics")).isEmpty());
	}

	@Test
	void collectItemIds_readsNumericParams() {
		List<Object> condition = sampleCondition(List.of(1L, 2L, 0L));
		assertEquals(List.of(1L, 2L), MemberTagSegmentRuleOrderItemParamsSupport.collectItemIds(condition));
	}

	@Test
	void extractItemIds_acceptsNumberAndObject() throws Exception {
		JsonNode params =
				objectMapper.readTree("[101,{\"item_id\":102,\"item_name\":\"x\",\"pics\":[]},\"103\"]");
		assertEquals(
				List.of(101L, 102L, 103L),
				MemberTagSegmentRuleOrderItemParamsSupport.extractItemIds(params));
	}

	private static List<Object> sampleCondition(List<Long> itemIds) {
		List<Object> params = new ArrayList<>(itemIds);
		Map<String, Object> orderItem = new LinkedHashMap<>();
		orderItem.put("type", "orderItem");
		orderItem.put("params", params);
		Map<String, Object> orderRoot = new LinkedHashMap<>();
		orderRoot.put("type", "order");
		orderRoot.put("sub", List.of(orderItem));
		List<Object> condition = new ArrayList<>();
		condition.add(orderRoot);
		return condition;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> findOrderItem(List<Object> condition) {
		Map<String, Object> orderRoot = (Map<String, Object>) condition.get(0);
		List<Map<String, Object>> sub = (List<Map<String, Object>>) orderRoot.get("sub");
		return sub.get(0);
	}
}
