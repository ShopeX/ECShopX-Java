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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

/** ECX-10943：B 端删除话题必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class TopicDeleteServiceScopeTest {

	@Mock
	private TopicMultiLangDeleteService topicMultiLangDeleteService;

	@Mock
	private TopicMapper topicMapper;

	private TopicDeleteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Topic.class);
	}

	@BeforeEach
	void setUp() {
		service = new TopicDeleteService(topicMultiLangDeleteService, topicMapper);
	}

	@Test
	void deleteTopics_mixedTenantIds_deletesOnlyOwnCompany_andStillReportsSuccess() {
		Topic owned = new Topic();
		owned.setTopicId(10L);
		owned.setCompanyId(100L);
		when(topicMapper.selectList(any(Wrapper.class))).thenReturn(List.of(owned));

		Map<String, Object> out = service.deleteTopics(Map.of("topic_id", List.of(10L, 99L)), 100L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		assertThat(out.get("topic_id")).isEqualTo(List.of(10L, 99L));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Topic>> scopeCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(topicMapper).selectList(scopeCaptor.capture());
		assertThat(scopeCaptor.getValue().getSqlSegment()).containsIgnoringCase("company_id");

		verify(topicMultiLangDeleteService).deleteMultiLangForTopics(eq(100L), eq(List.of(10L)));
		verify(topicMapper).deleteBatchIds(eq(List.of(10L)));
	}

	@Test
	void deleteTopics_foreignOnly_skipsPhysicalDelete_andStillReportsSuccess() {
		when(topicMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

		Map<String, Object> out = service.deleteTopics(Map.of("topic_id", 99L), 100L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		assertThat(out.get("topic_id")).isEqualTo(99L);
		verify(topicMultiLangDeleteService, never()).deleteMultiLangForTopics(anyLong(), anyList());
		verify(topicMapper, never()).deleteBatchIds(anyCollection());
	}
}
