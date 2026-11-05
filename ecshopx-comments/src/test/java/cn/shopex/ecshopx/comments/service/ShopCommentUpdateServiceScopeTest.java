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

package cn.shopex.ecshopx.comments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.comments.domain.ShopComments;
import cn.shopex.ecshopx.comments.mapper.ShopCommentsMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ShopCommentUpdateServiceScopeTest {

	@Mock
	private ShopCommentsMapper shopCommentsMapper;

	private ShopCommentUpdateService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ShopComments.class);
	}

	@BeforeEach
	void setUp() {
		service = new ShopCommentUpdateService(shopCommentsMapper, new ObjectMapper());
	}

	@Test
	void update_rejectsForeignCompanyComment_beforeWriting() {
		when(shopCommentsMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(() -> service.update("42", Map.of("is_hide", true), 100L))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("评论不存在");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<ShopComments>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(shopCommentsMapper).selectOne(captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(shopCommentsMapper, never()).update(any(ShopComments.class), any(Wrapper.class));
		verify(shopCommentsMapper, never()).updateById(any(ShopComments.class));
	}

	@Test
	void update_allowsOwnCompanyComment_andScopesWrite() {
		ShopComments entity = new ShopComments();
		entity.setCommentId(42L);
		entity.setCompanyId("100");
		entity.setUserId("7");
		entity.setShopId("0");
		entity.setContent("hello");
		entity.setStuck(false);
		entity.setHid(false);
		entity.setCreated(1);
		entity.setUpdated(1);

		ShopComments reloaded = new ShopComments();
		reloaded.setCommentId(42L);
		reloaded.setCompanyId("100");
		reloaded.setUserId("7");
		reloaded.setShopId("0");
		reloaded.setContent("hello");
		reloaded.setStuck(false);
		reloaded.setHid(true);
		reloaded.setCreated(1);
		reloaded.setUpdated(2);

		when(shopCommentsMapper.selectOne(any(Wrapper.class))).thenReturn(entity, reloaded);
		when(shopCommentsMapper.update(any(ShopComments.class), any(Wrapper.class))).thenReturn(1);

		Map<String, Object> out = service.update("42", Map.of("is_hide", true), 100L);

		assertThat(out.get("comment_id")).isEqualTo(42L);
		assertThat(out.get("company_id")).isEqualTo("100");
		assertThat(out.get("hid")).isEqualTo(true);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<ShopComments>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(shopCommentsMapper).update(any(ShopComments.class), updateCaptor.capture());
		assertThat(updateCaptor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(shopCommentsMapper, never()).updateById(any(ShopComments.class));
		verify(shopCommentsMapper, never()).update(isNull(), any(Wrapper.class));
	}
}
