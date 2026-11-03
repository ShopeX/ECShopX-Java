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

package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class PosStoreAccountRulesTest {

	@Test
	void employeeNoAllowsLettersAndDigits() {
		assertEquals("A01b2", PosStoreAccountRules.requireEmployeeNo(" A01b2 "));
	}

	@Test
	void employeeNoRejectsSpecialAndChinese() {
		assertEquals(
				"员工号仅支持英文字母和数字",
				assertThrows(BadRequestException.class, () -> PosStoreAccountRules.requireEmployeeNo("A-01"))
						.getMessage());
		assertThrows(BadRequestException.class, () -> PosStoreAccountRules.requireEmployeeNo("A_01"));
		assertThrows(BadRequestException.class, () -> PosStoreAccountRules.requireEmployeeNo("A.01"));
		assertThrows(BadRequestException.class, () -> PosStoreAccountRules.requireEmployeeNo("A 01"));
		assertThrows(BadRequestException.class, () -> PosStoreAccountRules.requireEmployeeNo("员工01"));
	}

	@Test
	void nameAllowsChineseLettersDigits_emptyOptional() {
		assertEquals("", PosStoreAccountRules.optionalName("  "));
		assertEquals("张三", PosStoreAccountRules.optionalName(" 张三 "));
		assertEquals("Tom01", PosStoreAccountRules.requireName("Tom01"));
	}

	@Test
	void nameRejectsSpecialChars() {
		assertEquals(
				"姓名仅支持中文、英文字母和数字",
				assertThrows(BadRequestException.class, () -> PosStoreAccountRules.optionalName("张-三"))
						.getMessage());
		assertThrows(BadRequestException.class, () -> PosStoreAccountRules.optionalName("张 三"));
		assertThrows(BadRequestException.class, () -> PosStoreAccountRules.optionalName("Tom_01"));
		assertThrows(BadRequestException.class, () -> PosStoreAccountRules.requireName(""));
	}
}
