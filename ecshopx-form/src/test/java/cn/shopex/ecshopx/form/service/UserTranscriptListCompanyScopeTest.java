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

package cn.shopex.ecshopx.form.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserTranscriptListCompanyScopeTest {

	@Test
	void apply_stripsClientCompanyKeys_andForcesJwtCompany() {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("company_id", 999L);
		filters.put("company_id|neq", 100L);
		filters.put("company_id|in", "1,2");
		filters.put("user_id", 55L);
		filters.put("transcript_name|contains", "期末");

		Map<String, Object> scoped = UserTranscriptListCompanyScope.apply(filters, 100L);

		assertThat(scoped.get("company_id")).isEqualTo(100L);
		assertThat(scoped).doesNotContainKeys("company_id|neq", "company_id|in");
		assertThat(scoped.get("user_id")).isEqualTo(55L);
		assertThat(scoped.get("transcript_name|contains")).isEqualTo("期末");
		assertThat(filters.get("company_id")).isEqualTo(999L);
	}

	@Test
	void apply_forcesCompanyWhenClientOmitsIt() {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("shop_id", 3L);

		Map<String, Object> scoped = UserTranscriptListCompanyScope.apply(filters, 42L);

		assertThat(scoped.get("company_id")).isEqualTo(42L);
		assertThat(scoped.get("shop_id")).isEqualTo(3L);
	}
}
