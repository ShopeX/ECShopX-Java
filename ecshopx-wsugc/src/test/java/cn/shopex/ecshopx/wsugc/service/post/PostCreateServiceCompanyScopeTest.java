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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.PostBadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostTopicMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10907：B 端发布笔记的编辑分支必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class PostCreateServiceCompanyScopeTest {

	@Mock
	private PostMapper postMapper;

	@Mock
	private PostTopicMapper postTopicMapper;

	@Mock
	private PostBadgeMapper postBadgeMapper;

	private PostCreateService service;

	@BeforeEach
	void setUp() {
		service =
				new PostCreateService(
						postMapper,
						postTopicMapper,
						postBadgeMapper,
						new ObjectMapper(),
						new PostTopCapService(postMapper));
	}

	private static Map<String, Object> inputFor(long postId) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("post_id", postId);
		input.put("title", "改后的标题");
		input.put("content", "改后的内容");
		input.put("cover", "https://cdn/cover.jpg");
		input.put("image_path", List.of("https://cdn/a.jpg"));
		return input;
	}

	private static Map<String, Object> operatorJwt(long companyId) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_id", 22L);
		jwt.put("company_id", companyId);
		jwt.put("mobile", "15600000000");
		return jwt;
	}

	@Test
	void editForeignCompanyPost_isRejectedWithoutUpdate() {
		Post foreign = new Post();
		foreign.setPostId(77L);
		foreign.setCompanyId(99L);
		when(postMapper.selectById(77L)).thenReturn(foreign);

		assertThatThrownBy(
						() ->
								service.createOrUpdate(
										inputFor(77L), operatorJwt(11L), "127.0.0.1", false))
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
		own.setTitle("原标题");
		when(postMapper.selectById(77L)).thenReturn(own, own);

		Map<String, Object> out =
				service.createOrUpdate(inputFor(77L), operatorJwt(11L), "127.0.0.1", false);

		assertThat(out.get("message")).isEqualTo("更新笔记成功");
		verify(postMapper).updateById(any(Post.class));
	}

	@Test
	void createWithoutPostId_insertsNewPost() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("title", "新标题");
		input.put("content", "新内容");
		input.put("cover", "https://cdn/cover.jpg");
		input.put("image_path", List.of("https://cdn/a.jpg"));

		Post inserted = new Post();
		inserted.setPostId(101L);
		inserted.setCompanyId(11L);
		inserted.setTitle("新标题");
		when(postMapper.insert(any(Post.class)))
				.thenAnswer(
						inv -> {
							((Post) inv.getArgument(0)).setPostId(101L);
							return 1;
						});
		when(postMapper.selectById(101L)).thenReturn(inserted);

		Map<String, Object> out =
				service.createOrUpdate(input, operatorJwt(11L), "127.0.0.1", false);

		assertThat(out.get("message")).isEqualTo("创建笔记成功");
		verify(postMapper).insert(any(Post.class));
		verify(postMapper, never()).updateById(any(Post.class));
	}
}
