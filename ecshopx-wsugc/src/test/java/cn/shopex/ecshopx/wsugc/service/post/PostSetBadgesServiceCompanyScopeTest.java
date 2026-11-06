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
import cn.shopex.ecshopx.wsugc.domain.Badge;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.BadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostBadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** ECX-10925：批量设置角标须校验 post 与 badge 双维度归属本租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostSetBadgesServiceCompanyScopeTest {

	@Mock
	private PostMapper postMapper;

	@Mock
	private PostBadgeMapper postBadgeMapper;

	@Mock
	private BadgeMapper badgeMapper;

	private PostSetBadgesService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Post.class);
		TableInfoHelper.initTableInfo(assistant, Badge.class);
	}

	@BeforeEach
	void setUp() {
		service = new PostSetBadgesService(postMapper, postBadgeMapper, badgeMapper);
	}

	private static Map<String, Object> inputFor(List<Long> postIds, List<Long> badgeIds) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("post_id", postIds);
		input.put("badges", badgeIds);
		return input;
	}

	private static Map<String, Object> operatorJwt(long companyId) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_id", 22L);
		jwt.put("company_id", companyId);
		return jwt;
	}

	private static Post post(long postId, long companyId) {
		Post p = new Post();
		p.setPostId(postId);
		p.setCompanyId(companyId);
		return p;
	}

	private static Badge badge(long badgeId, long companyId) {
		Badge b = new Badge();
		b.setBadgeId(badgeId);
		b.setCompanyId(companyId);
		return b;
	}

	@Test
	void mixedPostBatch_isRejectedWithoutSideEffects() {
		when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of(post(77L, 11L)));

		assertThatThrownBy(
						() ->
								service.setBadges(
										inputFor(List.of(77L, 88L), List.of(5L)),
										operatorJwt(11L),
										false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postBadgeMapper, never()).delete(any(Wrapper.class));
		verify(postBadgeMapper, never()).insert(any(cn.shopex.ecshopx.wsugc.domain.PostBadge.class));
		verify(postMapper, never()).updateById(any(Post.class));
	}

	@Test
	void foreignCompanyPost_isRejectedWithoutSideEffects() {
		when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of(post(88L, 99L)));

		assertThatThrownBy(
						() ->
								service.setBadges(
										inputFor(List.of(88L), List.of(5L)),
										operatorJwt(11L),
										false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postBadgeMapper, never()).delete(any(Wrapper.class));
	}

	@Test
	void foreignBadge_isRejectedWithoutSideEffects() {
		when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of(post(77L, 11L)));
		when(badgeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(badge(5L, 99L)));

		assertThatThrownBy(
						() ->
								service.setBadges(
										inputFor(List.of(77L), List.of(5L)),
										operatorJwt(11L),
										false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该角标");

		verify(postBadgeMapper, never()).delete(any(Wrapper.class));
		verify(postBadgeMapper, never()).insert(any(cn.shopex.ecshopx.wsugc.domain.PostBadge.class));
	}

	@Test
	void missingBadge_isRejectedWithoutSideEffects() {
		when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of(post(77L, 11L)));
		when(badgeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

		assertThatThrownBy(
						() ->
								service.setBadges(
										inputFor(List.of(77L), List.of(404L)),
										operatorJwt(11L),
										false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该角标");
	}

	@Test
	void ownPostsAndBadges_setNormally() {
		Post own = post(77L, 11L);
		when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of(own));
		when(badgeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(badge(5L, 11L)));

		Map<String, Object> out =
				service.setBadges(inputFor(List.of(77L), List.of(5L)), operatorJwt(11L), false);

		assertThat(out.get("message")).isEqualTo("批量设置角标成功");
		verify(postBadgeMapper).delete(any(Wrapper.class));
		verify(postBadgeMapper).insert(any(cn.shopex.ecshopx.wsugc.domain.PostBadge.class));
		verify(postMapper).updateById(any(Post.class));
	}
}
