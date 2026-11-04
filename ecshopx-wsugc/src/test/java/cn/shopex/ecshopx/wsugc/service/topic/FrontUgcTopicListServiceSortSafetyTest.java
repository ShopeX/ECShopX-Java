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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FrontUgcTopicListServiceSortSafetyTest {

	@Mock
	private TopicMapper topicMapper;

	@Mock
	private TopicOutsideLangReadService topicOutsideLangReadService;

	@Mock
	private TopicDetailService topicDetailService;

	private FrontUgcTopicListService service;

	@BeforeEach
	void setUp() {
		service = new FrontUgcTopicListService(topicMapper, topicOutsideLangReadService, topicDetailService);
		when(topicMapper.selectCount(any())).thenReturn(0L);
	}

	@Test
	void maliciousSort_doesNotInjectRawSqlFragment() throws Exception {
		service.buildH5TopicList(1L, 1, 10, null, "topic_id; DROP TABLE wsugc_topic -- desc");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Topic>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(topicMapper).selectCount(captor.capture());
		String lastSql = readLastSql(captor.getValue());
		assertFalse(lastSql.toUpperCase().contains("DROP"));
		assertFalse(lastSql.contains(";"));
		assertTrue(lastSql.isBlank());
	}

	@Test
	void validCreatedDesc_doesNotUseQuotedLastOrderBy() throws Exception {
		service.buildH5TopicList(1L, 1, 10, null, "created desc");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Topic>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(topicMapper).selectCount(captor.capture());
		String lastSql = readLastSql(captor.getValue());
		assertFalse(lastSql.contains("ORDER BY `"));
		assertTrue(lastSql.isBlank());
	}

	private static String readLastSql(Wrapper<?> wrapper) throws Exception {
		Class<?> c = wrapper.getClass();
		while (c != null) {
			try {
				Field f = c.getDeclaredField("lastSql");
				f.setAccessible(true);
				Object shared = f.get(wrapper);
				if (shared == null) {
					return "";
				}
				Method m = shared.getClass().getMethod("getStringValue");
				Object v = m.invoke(shared);
				return v == null ? "" : String.valueOf(v);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		return "";
	}
}
