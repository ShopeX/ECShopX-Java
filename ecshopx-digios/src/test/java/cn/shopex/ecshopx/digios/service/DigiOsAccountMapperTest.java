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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DigiOsAccountMapperTest {

	@Test
	void map_requiresLoginNameAndMobile() {
		assertNull(DigiOsAccountMapper.map(Map.of("login_name", "u1")));
		var m = DigiOsAccountMapper.map(Map.of("login_name", "staff001", "mobile", "13800138000", "username", "张三"));
		assertEquals("staff001", m.get("acc"));
		assertEquals("张三", m.get("nickNm"));
		assertEquals("13800138000", m.get("mobilePhone"));
	}

	@Test
	void map_fallsBackNickToAcc() {
		var m = DigiOsAccountMapper.map(Map.of("login_name", "staff001", "mobile", "13800138000"));
		assertEquals("staff001", m.get("nickNm"));
	}

	@Test
	void mapPos_usesEmployeeNoAndName() {
		assertNull(DigiOsAccountMapper.mapPos(Map.of("employee_no", "E01")));
		var m =
				DigiOsAccountMapper.mapPos(
						Map.of("employee_no", "E01", "mobile", "13900001111", "name", "李四", "email", "a@b.c"));
		assertEquals("E01", m.get("acc"));
		assertEquals("李四", m.get("nickNm"));
		assertEquals("13900001111", m.get("mobilePhone"));
		assertEquals("a@b.c", m.get("email"));
	}

	@Test
	void mapPos_fallsBackNickToEmployeeNo() {
		var m = DigiOsAccountMapper.mapPos(Map.of("employee_no", "E01", "mobile", "13900001111"));
		assertEquals("E01", m.get("nickNm"));
	}
}
