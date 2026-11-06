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

package cn.shopex.ecshopx.wsugc.service.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/** ECX-10943：话题多语言删除必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class TopicMultiLangDeleteServiceScopeTest {

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Test
	void deleteMultiLangForTopics_scopesEveryShardByCompanyId() {
		TopicMultiLangDeleteService service = new TopicMultiLangDeleteService(namedParameterJdbcTemplate);

		service.deleteMultiLangForTopics(100L, List.of(10L));

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
		verify(namedParameterJdbcTemplate, times(6)).update(sqlCaptor.capture(), paramsCaptor.capture());
		assertThat(sqlCaptor.getAllValues()).allMatch(sql -> sql.contains("company_id = :company_id"));
		assertThat(paramsCaptor.getAllValues())
				.allSatisfy(params -> assertThat(params.getValue("company_id")).isEqualTo(100L));
	}

	@Test
	void deleteMultiLangForTopics_emptyIdsSkipsDelete() {
		TopicMultiLangDeleteService service = new TopicMultiLangDeleteService(namedParameterJdbcTemplate);

		service.deleteMultiLangForTopics(100L, List.of());

		verify(namedParameterJdbcTemplate, never()).update(anyString(), any(SqlParameterSource.class));
	}
}
