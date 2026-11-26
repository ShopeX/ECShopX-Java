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

package cn.shopex.ecshopx.goods.service.hotword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** ECX-11390：展示时间入参解析与展示串格式化（Asia/Shanghai）。 */
class SearchHotwordTimeTest {

	@Test
	void parse_acceptsEpochSeconds() {
		assertThat(SearchHotwordTime.parse(1_800_000_000, "start_time")).isEqualTo(1_800_000_000);
		assertThat(SearchHotwordTime.parse("1800000000", "start_time")).isEqualTo(1_800_000_000);
	}

	@Test
	void parse_acceptsLocalDateTimeAsShanghai() {
		Integer parsed = SearchHotwordTime.parse("2027-01-01 00:00:00", "start_time");

		assertThat(parsed).isNotNull();
		assertThat(SearchHotwordTime.format(parsed)).isEqualTo("2027-01-01 00:00:00");
	}

	@Test
	void parse_acceptsMinuteAndDateOnlyForms() {
		assertThat(SearchHotwordTime.format(SearchHotwordTime.parse("2027-01-01 08:30", "start_time")))
				.isEqualTo("2027-01-01 08:30:00");
		assertThat(SearchHotwordTime.format(SearchHotwordTime.parse("2027-01-01", "start_time")))
				.isEqualTo("2027-01-01 00:00:00");
	}

	@Test
	void parse_blankReturnsNull() {
		assertThat(SearchHotwordTime.parse(null, "start_time")).isNull();
		assertThat(SearchHotwordTime.parse("  ", "start_time")).isNull();
	}

	@Test
	void parse_rejectsInvalidValues() {
		assertThatThrownBy(() -> SearchHotwordTime.parse("not-a-time", "start_time"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("start_time");
		assertThatThrownBy(() -> SearchHotwordTime.parse(-1, "end_time"))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void format_rendersShanghaiWallClock() {
		long epoch = LocalDateTime.of(2027, 1, 1, 0, 0, 0).atZone(SearchHotwordTime.ZONE).toEpochSecond();

		assertThat(SearchHotwordTime.format((int) epoch)).isEqualTo("2027-01-01 00:00:00");
		assertThat(SearchHotwordTime.format(null)).isNull();
	}
}
