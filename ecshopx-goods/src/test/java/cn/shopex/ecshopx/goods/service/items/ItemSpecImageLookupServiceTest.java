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

package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemSpecImageLookupServiceTest {

	@Mock
	private ItemRelAttributesRepository itemRelAttributesRepository;

	private ItemSpecImageLookupService service;

	@BeforeEach
	void setUp() {
		service = new ItemSpecImageLookupService(itemRelAttributesRepository);
	}

	@Test
	void firstUrlFromImageField_parsesJsonArray() {
		assertEquals(
				"https://cdn.example/spec-a.png",
				ItemSpecImageLookupService.firstUrlFromImageField("[\"https://cdn.example/spec-a.png\",\"b.png\"]"));
	}

	@Test
	void firstSpecImageUrlByItemId_returnsFirstNonEmptyPerItem() {
		ItemRelAttributes empty = new ItemRelAttributes();
		empty.setItemId(101L);
		empty.setImageUrl("");
		ItemRelAttributes withImg = new ItemRelAttributes();
		withImg.setItemId(101L);
		withImg.setImageUrl("[\"https://cdn.example/sku101.png\"]");
		ItemRelAttributes other = new ItemRelAttributes();
		other.setItemId(202L);
		other.setImageUrl("https://cdn.example/sku202.png");

		when(itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(
						eq(1L), eq(List.of(101L, 202L)), eq("item_spec")))
				.thenReturn(List.of(empty, withImg, other));

		Map<Long, String> map = service.firstSpecImageUrlByItemId(1L, List.of(101L, 202L));
		assertEquals("https://cdn.example/sku101.png", map.get(101L));
		assertEquals("https://cdn.example/sku202.png", map.get(202L));
	}

	@Test
	void applySpecImageUrl_putsFieldOnRows() {
		ItemRelAttributes rel = new ItemRelAttributes();
		rel.setItemId(55L);
		rel.setImageUrl("https://cdn.example/spec.png");
		when(itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(
						eq(9L), eq(List.of(55L)), eq("item_spec")))
				.thenReturn(List.of(rel));

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 55L);
		row.put("pics", "https://cdn.example/main.png");
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);

		service.applySpecImageUrl(9L, rows);
		assertEquals("https://cdn.example/spec.png", row.get("spec_image_url"));
	}

	@Test
	void resolveDisplayPic_prefersSpecOverPicsFallback() {
		ItemRelAttributes rel = new ItemRelAttributes();
		rel.setItemId(7L);
		rel.setImageUrl("[\"https://cdn.example/spec.png\"]");
		when(itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(
						eq(3L), eq(List.of(7L)), eq("item_spec")))
				.thenReturn(List.of(rel));

		String pic = service.resolveDisplayPic(3L, 7L, "https://cdn.example/main.png");
		assertEquals("https://cdn.example/spec.png", pic);
	}

	@Test
	void resolveDisplayPic_fallsBackWhenNoSpec() {
		when(itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(
						eq(3L), eq(List.of(8L)), eq("item_spec")))
				.thenReturn(List.of());

		String pic = service.resolveDisplayPic(3L, 8L, List.of("https://cdn.example/main.png"));
		assertEquals("https://cdn.example/main.png", pic);
	}
}
