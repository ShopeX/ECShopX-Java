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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10874：话题审核必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class TopicVerifyServiceScopeTest {

	@Mock
	private TopicMapper topicMapper;

	private TopicVerifyService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Topic.class);
	}

	@BeforeEach
	void setUp() {
		service = new TopicVerifyService(topicMapper);
	}

	@Test
	void verify_updateScopedByCompanyId() {
		when(topicMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("status", 1);
		input.put("topic_id", 42L);
		Map<String, Object> jwt = Map.of("company_id", 100L);

		Map<String, Object> out = service.verify(input, jwt, false);

		assertThat(out.get("message")).isEqualTo("审核成功");
		assertThat(out.get("topic_id")).isEqualTo(42L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Topic>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(topicMapper).update(isNull(), captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
	}

	@Test
	void verify_rejectsWhenNoRowUpdatedForCompany() {
		when(topicMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("status", 1);
		input.put("topic_id", 42L);
		Map<String, Object> jwt = Map.of("company_id", 100L);

		assertThatThrownBy(() -> service.verify(input, jwt, false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");
	}

	@Test
	void verify_rejectsWhenPartialBatchMissesCompanyRows() {
		when(topicMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("status", 4);
		input.put("topic_id", java.util.List.of(1L, 2L));
		Map<String, Object> jwt = Map.of("company_id", 100L);

		assertThatThrownBy(() -> service.verify(input, jwt, false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");
	}
}
