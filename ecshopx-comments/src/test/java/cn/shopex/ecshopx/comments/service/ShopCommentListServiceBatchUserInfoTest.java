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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.comments.domain.ShopComments;
import cn.shopex.ecshopx.comments.mapper.ShopCommentsMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

/** ECX-10918：评论列表用户信息须批量查询（消除 N+1）。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShopCommentListServiceBatchUserInfoTest {

	@Mock
	private ShopCommentsMapper shopCommentsMapper;

	@Mock
	private MemberAccountService memberAccountService;

	private ShopCommentListService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ShopComments.class);
	}

	@BeforeEach
	void setUp() {
		service = new ShopCommentListService(shopCommentsMapper, memberAccountService, new com.fasterxml.jackson.databind.ObjectMapper());
	}

	private static ShopComments comment(long commentId, String userId) {
		ShopComments c = new ShopComments();
		c.setCommentId(commentId);
		c.setCompanyId("11");
		c.setUserId(userId);
		c.setCreated(1730784000);
		return c;
	}

	private static Map<String, Object> userInfo(String nickname) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("nickname", nickname);
		m.put("headimgurl", "https://cdn/avatar.png");
		m.put("user_id", 1001L);
		return m;
	}

	@Test
	void list_loadsUserInfoInSingleBatchCall() {
		Page<ShopComments> page = new Page<>(1, 20);
		page.setTotal(3);
		page.setRecords(List.of(comment(1L, "1001"), comment(2L, "1002"), comment(3L, "1001")));
		when(shopCommentsMapper.selectPage(any(), any())).thenAnswer(inv -> {
			@SuppressWarnings("unchecked")
			Page<ShopComments> p = inv.getArgument(0);
			p.setTotal(3);
			p.setRecords(List.of(comment(1L, "1001"), comment(2L, "1002"), comment(3L, "1001")));
			return p;
		});
		when(memberAccountService.getWechatUserInfoByUserIds(anyList()))
				.thenReturn(Map.of(1001L, userInfo("张三"), 1002L, userInfo("李四")));

		Map<String, Object> out = service.list(11L, Map.of(), 1, 20);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) out.get("list");
		assertThat(list).hasSize(3);
		assertThat(list.get(0).get("nickname")).isEqualTo("张三");
		assertThat(list.get(1).get("nickname")).isEqualTo("李四");
		// 3 行 2 个用户 → 批量方法只调 1 次（去重后）
		verify(memberAccountService, times(1)).getWechatUserInfoByUserIds(anyList());
	}

	@Test
	void list_missingUser_stillThrows() {
		when(shopCommentsMapper.selectPage(any(), any())).thenAnswer(inv -> {
			@SuppressWarnings("unchecked")
			Page<ShopComments> p = inv.getArgument(0);
			p.setTotal(1);
			p.setRecords(List.of(comment(1L, "404")));
			return p;
		});
		when(memberAccountService.getWechatUserInfoByUserIds(anyList())).thenReturn(Map.of());

		assertThatThrownBy(() -> service.list(11L, Map.of(), 1, 20))
				.isInstanceOf(ResourceException.class)
				.hasMessage("用户信息错误");
	}

	@Test
	void emptyList_skipsUserLookup() {
		when(shopCommentsMapper.selectPage(any(), any())).thenAnswer(inv -> {
			@SuppressWarnings("unchecked")
			Page<ShopComments> p = inv.getArgument(0);
			p.setTotal(0);
			p.setRecords(List.of());
			return p;
		});

		Map<String, Object> out = service.list(11L, Map.of(), 1, 20);

		assertThat(out.get("total_count")).isEqualTo(0);
		verify(memberAccountService, never()).getWechatUserInfoByUserIds(anyList());
	}
}
