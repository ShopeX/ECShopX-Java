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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10872：话题置顶必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class TopicSetTopServiceScopeTest {

	@Mock
	private TopicMapper topicMapper;

	private TopicSetTopService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Topic.class);
	}

	@BeforeEach
	void setUp() {
		service = new TopicSetTopService(topicMapper);
	}

	@Test
	void setTop_clearScopedByCompanyId() {
		when(topicMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(topicMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(() -> service.setTop(100L, List.of(9L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Topic>> clearCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(topicMapper).update(isNull(), clearCaptor.capture());
		assertThat(clearCaptor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(topicMapper, never()).updateById(any(Topic.class));
	}

	@Test
	void setTop_rejectsTopicOutsideCompany() {
		when(topicMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(topicMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(() -> service.setTop(100L, List.of(42L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Topic>> selectCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(topicMapper).selectOne(selectCaptor.capture());
		assertThat(selectCaptor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(topicMapper, never()).updateById(any(Topic.class));
	}

	@Test
	void setTop_pinsOwnCompanyTopic() {
		Topic row = new Topic();
		row.setTopicId(42L);
		row.setCompanyId(100L);
		row.setIsTop(0);
		row.setPOrder(0);

		when(topicMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(topicMapper.selectOne(any(Wrapper.class))).thenReturn(row);
		when(topicMapper.updateById(any(Topic.class))).thenReturn(1);

		Map<String, Object> out = service.setTop(100L, List.of(42L));

		assertThat(out.get("message")).isEqualTo("置顶成功");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> allUpdate = (List<Map<String, Object>>) out.get("allUpdate");
		assertThat(allUpdate).hasSize(1);
		assertThat(allUpdate.get(0).get("is_top")).isEqualTo(1);
		assertThat(allUpdate.get(0).get("p_order")).isEqualTo(-1);

		verify(topicMapper).updateById(row);
		assertThat(row.getIsTop()).isEqualTo(1);
		assertThat(row.getPOrder()).isEqualTo(-1);
	}
}
