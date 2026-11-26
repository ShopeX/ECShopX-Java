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
import cn.shopex.ecshopx.goods.domain.SearchHotword;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SearchHotwordStatusTest {

	@BeforeAll
	static void initMybatisLambdaCache() {
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SearchHotword.class);
	}

	@Test
	void parseFilter_acceptsTriState() {
		assertThat(SearchHotwordStatus.parseFilter(null)).isNull();
		assertThat(SearchHotwordStatus.parseFilter("")).isNull();
		assertThat(SearchHotwordStatus.parseFilter("0")).isEqualTo(0);
		assertThat(SearchHotwordStatus.parseFilter("1")).isEqualTo(1);
		assertThat(SearchHotwordStatus.parseFilter("2")).isEqualTo(2);
		assertThatThrownBy(() -> SearchHotwordStatus.parseFilter("3"))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void applyListFilter_writesTimePredicates() {
		LambdaQueryWrapper<SearchHotword> active = new LambdaQueryWrapper<>();
		SearchHotwordStatus.applyListFilter(active, SearchHotwordStatus.ACTIVE, 100L);
		assertThat(active.getSqlSegment()).contains("start_time").contains("end_time");

		LambdaQueryWrapper<SearchHotword> notStarted = new LambdaQueryWrapper<>();
		SearchHotwordStatus.applyListFilter(notStarted, SearchHotwordStatus.NOT_STARTED, 100L);
		assertThat(notStarted.getSqlSegment()).contains("start_time");

		LambdaQueryWrapper<SearchHotword> ended = new LambdaQueryWrapper<>();
		SearchHotwordStatus.applyListFilter(ended, SearchHotwordStatus.ENDED, 100L);
		assertThat(ended.getSqlSegment()).contains("end_time");
	}
}
