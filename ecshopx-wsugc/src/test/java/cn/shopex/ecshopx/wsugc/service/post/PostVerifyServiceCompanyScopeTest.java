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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.service.point.UgcPostPointService;
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

/** ECX-10921：B 端笔记审核必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostVerifyServiceCompanyScopeTest {

	@Mock
	private PostMapper postMapper;

	@Mock
	private MessageMapper messageMapper;

	@Mock
	private UgcPostPointService ugcPostPointService;

	@Mock
	private MemberAccountService memberAccountService;

	private PostVerifyService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Post.class);
	}

	@BeforeEach
	void setUp() {
		service = new PostVerifyService(postMapper, messageMapper, ugcPostPointService, memberAccountService);
	}

	private static Post post(long postId, long companyId, long userId) {
		Post p = new Post();
		p.setPostId(postId);
		p.setCompanyId(companyId);
		p.setUserId(userId);
		return p;
	}

	private static Map<String, Object> inputFor(Object postIds, int status) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("post_id", postIds);
		input.put("status", status);
		return input;
	}

	private static Map<String, Object> operatorJwt(long companyId) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_id", 22L);
		jwt.put("company_id", companyId);
		jwt.put("username", "管理员");
		return jwt;
	}

	@Test
	void mixedBatchWithForeignPost_isRejectedWithoutSideEffects() {
		when(postMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(post(77L, 11L, 1001L)));

		assertThatThrownBy(
						() ->
								service.verify(
										inputFor(List.of(77L, 88L), 4),
										operatorJwt(11L),
										false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postMapper, never()).update(any(), any(Wrapper.class));
		verify(ugcPostPointService, never()).addUgcPoint(anyLong(), anyLong(), anyLong(), anyInt(), anyString());
		verify(messageMapper, never()).insert(any(cn.shopex.ecshopx.wsugc.domain.Message.class));
	}

	@Test
	void allForeignPosts_areRejectedWithoutSideEffects() {
		when(postMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(post(88L, 99L, 2001L)));

		assertThatThrownBy(
						() ->
								service.verify(
										inputFor(List.of(88L), 1),
										operatorJwt(11L),
										false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postMapper, never()).update(any(), any(Wrapper.class));
	}

	@Test
	void ownCompanyPosts_verifyNormally() {
		Post own = post(77L, 11L, 1001L);
		when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of(own));
		when(postMapper.selectById(77L)).thenReturn(own);
		when(memberAccountService.getWechatUserInfo(any())).thenReturn(Map.of("nickname", "张三"));

		Map<String, Object> out =
				service.verify(inputFor(List.of(77L), 4), operatorJwt(11L), false);

		assertThat(out.get("message")).isEqualTo("审核成功");
		verify(postMapper).update(any(), any(Wrapper.class));
		verify(ugcPostPointService).addUgcPoint(77L, 1001L, 11L, 9920, "reduce");
		verify(messageMapper).insert(any(cn.shopex.ecshopx.wsugc.domain.Message.class));
	}

	@Test
	void duplicateOwnPostIds_areNotRejected() {
		Post own = post(77L, 11L, 1001L);
		when(postMapper.selectList(any(Wrapper.class))).thenReturn(List.of(own));
		when(postMapper.selectById(77L)).thenReturn(own);

		Map<String, Object> out =
				service.verify(inputFor(List.of(77L, 77L), 1), operatorJwt(11L), false);

		assertThat(out.get("message")).isEqualTo("审核成功");
		verify(postMapper).update(any(), any(Wrapper.class));
	}
}
