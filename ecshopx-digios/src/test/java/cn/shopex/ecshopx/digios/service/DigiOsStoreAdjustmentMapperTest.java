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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DigiOsStoreAdjustmentMapperTest {

	@Test
	void mapDetails_outboundNegativeQty() {
		var details = DigiOsStoreAdjustmentMapper.mapDetails(
				List.of(Map.of("sku_id", "SKU1", "qty", 3)), true);
		assertEquals(1, details.size());
		assertEquals("SKU1", details.get(0).get("skuCd"));
		assertEquals(-3, details.get(0).get("adjQty"));
	}

	@Test
	void chunkDetails_splitsByMax() {
		List<Map<String, Object>> lines = List.of(
				Map.<String, Object>of("skuCd", "a", "adjQty", 1),
				Map.<String, Object>of("skuCd", "b", "adjQty", 2),
				Map.<String, Object>of("skuCd", "c", "adjQty", 3));
		var chunks = DigiOsStoreAdjustmentMapper.chunkDetails(lines, 2);
		assertEquals(2, chunks.size());
		assertEquals(2, chunks.get(0).size());
		assertEquals(1, chunks.get(1).size());
	}

	@Test
	void buildPayload_hasAutoFlags() {
		var payload = DigiOsStoreAdjustmentMapper.buildPayload(
				"nm1", "code_000008", "SHOP01", List.of(Map.of("skuCd", "x", "adjQty", -1)));
		assertEquals("nm1", payload.get("nm"));
		assertTrue(Boolean.TRUE.equals(payload.get("autoConfirm")));
		assertTrue(Boolean.TRUE.equals(payload.get("autoAudit")));
		assertEquals(DigiOsStoreAdjustmentMapper.CRT_METHOD, payload.get("crtMethod"));
	}
}
