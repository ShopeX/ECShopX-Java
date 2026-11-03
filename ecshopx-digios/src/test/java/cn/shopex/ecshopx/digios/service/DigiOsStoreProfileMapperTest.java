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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DigiOsStoreProfileMapperTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void mapStatus_openForTrueAndOne() {
		assertEquals(DigiOsStoreProfileMapper.STATUS_OPEN, DigiOsStoreProfileMapper.mapStatus("true"));
		assertEquals(DigiOsStoreProfileMapper.STATUS_CLOSED, DigiOsStoreProfileMapper.mapStatus("false"));
	}

	@Test
	void formatOpHours_parsesDashAndJson() {
		assertEquals("09:00,21:00", DigiOsStoreProfileMapper.formatOpHours("09:00-21:00", "10:00,22:00", objectMapper));
		assertEquals(
				"08:30,20:30",
				DigiOsStoreProfileMapper.formatOpHours(
						"[[1,2,\"08:30\",\"20:30\"]]", "10:00,22:00", objectMapper));
	}

	@Test
	void composeAddress_joinsHouseNumber() {
		assertEquals(
				"南京路 100号",
				DigiOsStoreProfileMapper.composeAddress(Map.of("address", "南京路", "house_number", "100号")));
	}
}
