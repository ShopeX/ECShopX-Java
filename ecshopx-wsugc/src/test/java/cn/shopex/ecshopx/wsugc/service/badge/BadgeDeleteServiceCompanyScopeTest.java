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

package cn.shopex.ecshopx.wsugc.service.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Badge;
import cn.shopex.ecshopx.wsugc.mapper.BadgeMapper;
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

/** ECX-10929：删除角标必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BadgeDeleteServiceCompanyScopeTest {

	@Mock
	private BadgeMapper badgeMapper;

	private BadgeDeleteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Badge.class);
	}

	@BeforeEach
	void setUp() {
		service = new BadgeDeleteService(badgeMapper);
	}

	private static Map<String, Object> inputFor(Object badgeIds) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("badge_id", badgeIds);
		return input;
	}

	private static Badge badge(long badgeId, long companyId) {
		Badge b = new Badge();
		b.setBadgeId(badgeId);
		b.setCompanyId(companyId);
		return b;
	}

	@Test
	void mixedBatchWithForeignBadge_isRejectedWithoutDelete() {
		when(badgeMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(badge(5L, 11L)));

		assertThatThrownBy(
						() -> service.deleteBadges(inputFor(List.of(5L, 6L)), 11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该角标");

		verify(badgeMapper, never()).deleteBatchIds(anyCollection());
	}

	@Test
	void foreignCompanyBadge_isRejectedWithoutDelete() {
		when(badgeMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(badge(5L, 99L)));

		assertThatThrownBy(() -> service.deleteBadges(inputFor(List.of(5L)), 11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该角标");

		verify(badgeMapper, never()).deleteBatchIds(anyCollection());
	}

	@Test
	void missingBadge_isRejectedWithoutDelete() {
		when(badgeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

		assertThatThrownBy(() -> service.deleteBadges(inputFor(List.of(404L)), 11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该角标");
	}

	@Test
	void ownCompanyBadges_deleteNormally() {
		when(badgeMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(badge(5L, 11L), badge(6L, 11L)));

		Map<String, Object> out = service.deleteBadges(inputFor(List.of(5L, 6L)), 11L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		verify(badgeMapper).deleteBatchIds(anyCollection());
	}

	@Test
	void garbageTokenWithoutParsedId_keepsExistingBehavior() {
		// 纯垃圾 token：无可解析 id → 不查库不删除，返回「删除成功」（既有行为）
		Map<String, Object> out = service.deleteBadges(inputFor(List.of("abc")), 11L);

		assertThat(out.get("message")).isEqualTo("删除成功");
		verify(badgeMapper, never()).selectList(any(Wrapper.class));
		verify(badgeMapper, never()).deleteBatchIds(anyCollection());
	}
}
