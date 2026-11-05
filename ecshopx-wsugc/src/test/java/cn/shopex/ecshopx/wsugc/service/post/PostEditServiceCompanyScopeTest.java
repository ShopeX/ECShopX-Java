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

package cn.shopex.ecshopx.wsugc.service.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.PostBadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostTopicMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10919：B 端笔记编辑必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class PostEditServiceCompanyScopeTest {

	@Mock
	private PostMapper postMapper;

	@Mock
	private PostTopicMapper postTopicMapper;

	@Mock
	private PostBadgeMapper postBadgeMapper;

	private PostEditService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Post.class);
	}

	@BeforeEach
	void setUp() {
		service = new PostEditService(postMapper, postTopicMapper, postBadgeMapper, new PostTopCapService(postMapper));
	}

	private static Map<String, Object> inputFor(long postId, String topics) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("post_id", postId);
		if (topics != null) {
			input.put("topics", List.of(topics));
		}
		return input;
	}

	private static Map<String, Object> operatorJwt(long companyId) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_id", 22L);
		jwt.put("company_id", companyId);
		return jwt;
	}

	@Test
	void editForeignCompanyPost_isRejectedWithoutSideEffects() {
		Post foreign = new Post();
		foreign.setPostId(77L);
		foreign.setCompanyId(99L);
		when(postMapper.selectById(77L)).thenReturn(foreign);

		assertThatThrownBy(
						() -> service.edit(inputFor(77L, "新话题"), operatorJwt(11L), false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postMapper, never()).updateById(any(Post.class));
		verify(postTopicMapper, never()).delete(any());
		verify(postBadgeMapper, never()).delete(any());
	}

	@Test
	void editOwnCompanyPost_updatesNormally() {
		Post own = new Post();
		own.setPostId(77L);
		own.setCompanyId(11L);
		own.setTopics("旧话题");
		when(postMapper.selectById(77L)).thenReturn(own, own);

		Map<String, Object> out = service.edit(inputFor(77L, "新话题"), operatorJwt(11L), false);

		assertThat(out.get("post_id")).isEqualTo(77L);
		verify(postMapper).updateById(any(Post.class));
		verify(postTopicMapper).delete(any());
	}
}
