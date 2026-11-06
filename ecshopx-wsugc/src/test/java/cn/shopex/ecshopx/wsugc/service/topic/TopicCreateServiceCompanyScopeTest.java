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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** ECX-10942：话题创建/编辑必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TopicCreateServiceCompanyScopeTest {

	@Mock
	private TopicMapper topicMapper;

	private TopicCreateService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Topic.class);
	}

	@BeforeEach
	void setUp() {
		service = new TopicCreateService(topicMapper);
	}

	private static Map<String, Object> inputFor(String topicName, Long topicId) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("topic_name", topicName);
		if (topicId != null) {
			input.put("topic_id", topicId);
		}
		return input;
	}

	private static Map<String, Object> operatorJwt(long companyId) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_id", 22L);
		jwt.put("company_id", companyId);
		return jwt;
	}

	private static Topic topic(long topicId, long companyId, String name) {
		Topic topic = new Topic();
		topic.setTopicId(topicId);
		topic.setCompanyId(companyId);
		topic.setTopicName(name);
		return topic;
	}

	@Test
	void editingForeignCompanyTopic_isRejectedWithoutUpdate() {
		when(topicMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());
		when(topicMapper.selectById(77L)).thenReturn(topic(77L, 99L, "优选"));

		assertThatThrownBy(() -> service.createOrUpdate(inputFor("新名称", 77L), operatorJwt(11L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该话题");

		verify(topicMapper, never()).updateById(any(Topic.class));
	}

	@Test
	void sameNameWithinOwnTenant_isRejected() {
		when(topicMapper.selectList(any(Wrapper.class)))
				.thenReturn(java.util.List.of(topic(50L, 11L, "优选")));

		assertThatThrownBy(() -> service.createOrUpdate(inputFor("优选", null), operatorJwt(11L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("同名话题已存在");

		verify(topicMapper, never()).insert(any(Topic.class));
	}

	@Test
	void sameNameOnForeignTenantTopic_createSucceedsWithCallerCompany() {
		when(topicMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());
		Map<String, Object> input = inputFor("优选", null);
		input.put("company_id", 99L);

		Map<String, Object> out = service.createOrUpdate(input, operatorJwt(11L));

		assertThat(out.get("message")).isEqualTo("创建话题成功");
		ArgumentCaptor<Topic> inserted = ArgumentCaptor.forClass(Topic.class);
		verify(topicMapper).insert(inserted.capture());
		assertThat(inserted.getValue().getCompanyId()).isEqualTo(11L);
	}

	@Test
	void selfUpdateWithSameName_updatesNormally() {
		when(topicMapper.selectList(any(Wrapper.class)))
				.thenReturn(java.util.List.of(topic(50L, 11L, "优选")));
		when(topicMapper.selectById(50L)).thenReturn(topic(50L, 11L, "优选"));

		Map<String, Object> out = service.createOrUpdate(inputFor("优选", 50L), operatorJwt(11L));

		assertThat(out.get("message")).isEqualTo("更新话题成功");
		verify(topicMapper).updateById(any(Topic.class));
	}

	@Test
	void missingTopic_isRejectedWithExistingMessage() {
		when(topicMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());
		when(topicMapper.selectById(404L)).thenReturn(null);

		assertThatThrownBy(() -> service.createOrUpdate(inputFor("名称", 404L), operatorJwt(11L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");

		verify(topicMapper, never()).updateById(any(Topic.class));
	}

	@Test
	void topicWithoutCompany_isRejectedWithoutUpdate() {
		when(topicMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());
		Topic unloaded = topic(77L, 99L, "优选");
		unloaded.setCompanyId(null);
		when(topicMapper.selectById(77L)).thenReturn(unloaded);

		assertThatThrownBy(() -> service.createOrUpdate(inputFor("新名称", 77L), operatorJwt(11L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该话题");

		verify(topicMapper, never()).updateById(any(Topic.class));
	}
}
