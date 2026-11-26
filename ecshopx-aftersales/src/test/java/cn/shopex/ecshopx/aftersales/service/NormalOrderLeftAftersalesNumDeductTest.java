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

package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NormalOrderLeftAftersalesNumDeductTest {

	@Mock private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("扣减成功时使用原子 SQL 并传入申请数量与门槛")
	void deduct_whenAffected_usesAtomicSqlWithApplyNumGuard() {
		when(jdbcTemplate.update(
						eq(NormalOrderLeftAftersalesNumDeduct.SQL), eq(2), eq(10L), eq(100L), eq(2)))
				.thenReturn(1);

		NormalOrderLeftAftersalesNumDeduct.deductOrThrow(jdbcTemplate, 10L, 100L, 2);

		ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate)
				.update(sqlCap.capture(), eq(2), eq(10L), eq(100L), eq(2));
		String sql = sqlCap.getValue();
		assertThat(sql).contains("left_aftersales_num = left_aftersales_num - ?");
		assertThat(sql).contains("left_aftersales_num >= ?");
		assertThat(sql).doesNotContain("left_aftersales_num = ?");
	}

	@Test
	@DisplayName("影响行数为 0 时抛可申请售后数量不足")
	void deduct_whenNoRowAffected_throwsInsufficient() {
		when(jdbcTemplate.update(
						eq(NormalOrderLeftAftersalesNumDeduct.SQL), eq(1), eq(10L), eq(100L), eq(1)))
				.thenReturn(0);

		assertThatThrownBy(
						() -> NormalOrderLeftAftersalesNumDeduct.deductOrThrow(jdbcTemplate, 10L, 100L, 1))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("可申请售后数量不足");
	}
}
