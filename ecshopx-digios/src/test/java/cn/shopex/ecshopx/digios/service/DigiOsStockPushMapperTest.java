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

package cn.shopex.ecshopx.digios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DigiOsStockPushMapperTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void parseValidJsonString() {
		var rows = DigiOsStockPushMapper.parseAndValidate(
				"[{\"sku_id\":\"A1\",\"stock\":10,\"shop_code\":\"S01\"}]", 200, objectMapper);
		assertEquals(1, rows.size());
		assertEquals("A1", rows.get(0).get("sku_id"));
		assertEquals(10, rows.get(0).get("stock"));
		assertEquals("S01", rows.get(0).get("shop_code"));
	}

	@Test
	void rejectsMissingShopCode() {
		assertThrows(
				ResourceException.class,
				() -> DigiOsStockPushMapper.parseAndValidate(
						"[{\"sku_id\":\"A1\",\"stock\":1}]", 200, objectMapper));
	}

	@Test
	void rejectsOverMaxRows() {
		assertThrows(
				ResourceException.class,
				() -> DigiOsStockPushMapper.parseAndValidate(
						List.of(Map.of("sku_id", "a", "stock", 1, "shop_code", "s")), 0, objectMapper));
	}

	@Test
	void parseStockStrictBounds() {
		assertEquals(0, DigiOsStockPushMapper.parseStockStrict("0"));
		assertThrows(ResourceException.class, () -> DigiOsStockPushMapper.parseStockStrict("-1"));
	}
}
