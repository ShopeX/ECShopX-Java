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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10909：B 端删除笔记必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class PostDeleteServiceCompanyScopeTest {

	@Mock
	private PostMapper postMapper;

	private PostDeleteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Post.class);
	}

	@BeforeEach
	void setUp() {
		service = new PostDeleteService(postMapper);
	}

	private static Post post(long postId, long companyId) {
		Post p = new Post();
		p.setPostId(postId);
		p.setCompanyId(companyId);
		return p;
	}

	@Test
	void deleteForeignCompanyPost_isRejectedWithoutUpdate() {
		when(postMapper.selectList(any())).thenReturn(List.of(post(77L, 99L)));

		assertThatThrownBy(() -> service.softDeleteByPostId(77L, 11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postMapper, never()).update(isNull(), any());
	}

	@Test
	void deleteBatchContainingForeignPost_isRejectedWithoutUpdate() {
		when(postMapper.selectList(any()))
				.thenReturn(List.of(post(77L, 11L), post(78L, 99L)));

		assertThatThrownBy(() -> service.softDeleteByPostId(List.of(77L, 78L), 11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postMapper, never()).update(isNull(), any());
	}

	@Test
	void deleteMissingPost_isRejectedWithoutUpdate() {
		when(postMapper.selectList(any())).thenReturn(List.of(post(77L, 11L)));

		assertThatThrownBy(() -> service.softDeleteByPostId(List.of(77L, 404L), 11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该笔记");

		verify(postMapper, never()).update(isNull(), any());
	}

	@Test
	void deleteOwnCompanyPost_succeeds() {
		when(postMapper.selectList(any())).thenReturn(List.of(post(77L, 11L)));

		var out = service.softDeleteByPostId(77L, 11L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		verify(postMapper).update(isNull(), any());
	}

	@Test
	void deleteOwnCompanyPostsBatch_succeeds() {
		when(postMapper.selectList(any()))
				.thenReturn(List.of(post(77L, 11L), post(78L, 11L)));

		var out = service.softDeleteByPostId(List.of(77L, 78L), 11L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		verify(postMapper).update(isNull(), any());
	}
}
