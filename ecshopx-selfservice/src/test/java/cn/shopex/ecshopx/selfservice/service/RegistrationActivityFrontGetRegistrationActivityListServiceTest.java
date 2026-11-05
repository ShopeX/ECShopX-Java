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

package cn.shopex.ecshopx.selfservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegistrationActivityFrontGetRegistrationActivityListServiceTest {

	@Test
	void frontActivityListOrderByLastClause_statusAll_ordersOngoingThenWaitingThenEnded() {
		String sql = RegistrationActivityFrontGetRegistrationActivityListService.frontActivityListOrderByLastClause(
				0, 1_700_000_000);
		assertThat(sql).isNotNull();
		assertThat(sql).contains("ORDER BY CASE");
		assertThat(sql).contains("start_time > 1700000000 THEN 1");
		assertThat(sql).contains("end_time < 1700000000 THEN 2");
		assertThat(sql).contains("ELSE 0 END ASC");
		assertThat(sql).contains("activity_id DESC");
	}

	@Test
	void frontActivityListOrderByLastClause_filteredStatus_usesDefaultActivityIdDesc() {
		assertThat(RegistrationActivityFrontGetRegistrationActivityListService.frontActivityListOrderByLastClause(
						1, 1_700_000_000))
				.isNull();
		assertThat(RegistrationActivityFrontGetRegistrationActivityListService.frontActivityListOrderByLastClause(
						2, 1_700_000_000))
				.isNull();
	}
}
