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

package cn.shopex.ecshopx.wsugc.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
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

@ExtendWith(MockitoExtension.class)
class CommentVerifyServiceScopeTest {

	@Mock
	private CommentMapper commentMapper;

	@Mock
	private MessageMapper messageMapper;

	@Mock
	private MemberAccountService memberAccountService;

	private CommentVerifyService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Comment.class);
	}

	@BeforeEach
	void setUp() {
		service = new CommentVerifyService(commentMapper, messageMapper, memberAccountService);
	}

	@Test
	void verify_rejectsMissingCompanyId() {
		assertThatThrownBy(
						() ->
								service.verify(
										Map.of("status", 2, "comment_id", 10L),
										Map.of("operator_id", 1L),
										false))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("企业信息");

		verify(commentMapper, never()).update(any(), any(Wrapper.class));
	}

	@Test
	void verify_scopesUpdateByCompany_andStillReportsSuccessForMixedIds() {
		when(commentMapper.update(any(), any(Wrapper.class))).thenReturn(1);

		Map<String, Object> out =
				service.verify(
						Map.of("status", 2, "comment_id", List.of(10L, 99L)),
						Map.of("operator_id", 1L, "company_id", 100L, "username", "op"),
						false);

		assertThat(out.get("message")).isEqualTo("审核成功");
		assertThat(out.get("comment_id")).isEqualTo(List.of(10L, 99L));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Comment>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(commentMapper).update(any(), captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(messageMapper, never()).insert(any(Message.class));
	}

	@Test
	void verify_refuseMessage_loadsCommentWithinCompanyOnly() {
		when(commentMapper.update(any(), any(Wrapper.class))).thenReturn(1);
		when(commentMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> out =
				service.verify(
						Map.of("status", 4, "comment_id", 99L, "refuse_reason", "spam"),
						Map.of("operator_id", 1L, "company_id", 100L),
						false);

		assertThat(out.get("message")).isEqualTo("审核成功");
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Comment>> selectCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(commentMapper).selectOne(selectCaptor.capture());
		assertThat(selectCaptor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(messageMapper, never()).insert(any(Message.class));
	}
}
