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

package cn.shopex.ecshopx.aftersales.api.admin.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AftersalesControllerAdminListPaginationTest {

	@Test
	void pageSize100IsAccepted() {
		assertNull(AftersalesController.validateAdminListPagination("1", "100"));
	}

	@Test
	void pageSize50StillAccepted() {
		assertNull(AftersalesController.validateAdminListPagination("1", "50"));
	}

	@Test
	void pageSizeOver100Rejected() {
		String msg = AftersalesController.validateAdminListPagination("1", "101");
		assertTrue(msg != null && msg.contains("pageSize 不能超过 100"), msg);
	}

	@Test
	void blankPageSizeRejected() {
		assertEquals("pageSize 字段必填", AftersalesController.validateAdminListPagination("1", null));
	}
}
