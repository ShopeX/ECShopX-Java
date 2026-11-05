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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
class FrontUgcCommentDeleteServiceScopeTest {

	@Mock
	private CommentMapper commentMapper;

	@Mock
	private UgcCommentRedisCounterService ugcCommentRedisCounterService;

	private FrontUgcCommentDeleteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Comment.class);
	}

	@BeforeEach
	void setUp() {
		service = new FrontUgcCommentDeleteService(commentMapper, ugcCommentRedisCounterService);
	}

	@Test
	void deleteByMember_rejectsWhenCommentNotInCallerCompany() {
		when(commentMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(() -> service.deleteByMember(7L, 100L, 42L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("评论不存在或已删除");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Comment>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(commentMapper).selectOne(captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(commentMapper, never()).update(any(), any(Wrapper.class));
		verify(ugcCommentRedisCounterService, never())
				.reduceCommentsToRedis(anyLong(), anyLong(), anyLong());
	}

	@Test
	void deleteByMember_softDeletesOwnCompanyComment() {
		Comment found = new Comment();
		found.setCommentId(42L);
		found.setUserId(7L);
		found.setCompanyId(100L);
		found.setPostId(9L);
		found.setDisabled(false);
		found.setContent("hi");
		found.setLikes("0");
		found.setStatus(1);
		found.setEnable(1);
		found.setCreated(1);
		found.setUpdated(1);

		Comment reloaded = new Comment();
		reloaded.setCommentId(42L);
		reloaded.setUserId(7L);
		reloaded.setCompanyId(100L);
		reloaded.setPostId(9L);
		reloaded.setDisabled(true);
		reloaded.setContent("hi");
		reloaded.setLikes("0");
		reloaded.setStatus(1);
		reloaded.setEnable(1);
		reloaded.setCreated(1);
		reloaded.setUpdated(2);

		when(commentMapper.selectOne(any(Wrapper.class))).thenReturn(found);
		when(commentMapper.update(any(), any(Wrapper.class))).thenReturn(1);
		when(commentMapper.selectById(42L)).thenReturn(reloaded);

		Map<String, Object> out = service.deleteByMember(7L, 100L, 42L);

		assertThat(out.get("comment_id")).isEqualTo("42");
		assertThat(out.get("company_id")).isEqualTo("100");
		assertThat(out.get("disabled")).isEqualTo(1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Comment>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(commentMapper).update(any(), updateCaptor.capture());
		assertThat(updateCaptor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(ugcCommentRedisCounterService).reduceCommentsToRedis(42L, 9L, 7L);
	}
}
