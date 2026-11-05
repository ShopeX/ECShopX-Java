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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
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
class CommentDeleteServiceScopeTest {

	@Mock
	private CommentMultiLangDeleteService commentMultiLangDeleteService;

	@Mock
	private CommentMapper commentMapper;

	private CommentDeleteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Comment.class);
	}

	@BeforeEach
	void setUp() {
		service = new CommentDeleteService(commentMultiLangDeleteService, commentMapper);
	}

	@Test
	void deleteComments_mixedTenantIds_deletesOnlyOwnCompany_andStillReportsSuccess() {
		Comment owned = new Comment();
		owned.setCommentId(10L);
		owned.setCompanyId(100L);
		when(commentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(owned));

		Map<String, Object> out =
				service.deleteComments(Map.of("comment_id", List.of(10L, 99L)), 100L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		assertThat(out.get("comment_id")).isEqualTo(List.of(10L, 99L));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Comment>> scopeCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(commentMapper).selectList(scopeCaptor.capture());
		String sqlSegment = scopeCaptor.getValue().getSqlSegment();
		assertThat(sqlSegment).containsIgnoringCase("company_id");

		verify(commentMultiLangDeleteService).deleteMultiLangForComments(eq(100L), eq(List.of(10L)));
		verify(commentMapper).deleteBatchIds(eq(List.of(10L)));
	}

	@Test
	void deleteComments_foreignOnly_skipsPhysicalDelete_andStillReportsSuccess() {
		when(commentMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

		Map<String, Object> out = service.deleteComments(Map.of("comment_id", 99L), 100L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		assertThat(out.get("comment_id")).isEqualTo(List.of(99L));
		verify(commentMultiLangDeleteService, never()).deleteMultiLangForComments(anyLong(), anyList());
		verify(commentMapper, never()).deleteBatchIds(anyCollection());
	}
}
