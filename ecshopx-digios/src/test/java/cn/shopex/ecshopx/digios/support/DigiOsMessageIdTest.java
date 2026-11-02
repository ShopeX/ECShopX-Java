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

package cn.shopex.ecshopx.digios.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DigiOsMessageIdTest {

	@Test
	void newMessageId_is32UppercaseHexWithoutDash() {
		String id = DigiOsMessageId.newMessageId();
		assertEquals(32, id.length());
		assertFalse(id.contains("-"));
		assertTrue(id.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')));
	}
}
