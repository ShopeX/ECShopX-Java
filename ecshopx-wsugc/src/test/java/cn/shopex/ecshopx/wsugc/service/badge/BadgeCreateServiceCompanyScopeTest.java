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

/** ECX-10927：角标创建/编辑必须限定当前租户（查重与归属双收窄）。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BadgeCreateServiceCompanyScopeTest {

	@Mock
	private BadgeMapper badgeMapper;

	private BadgeCreateService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Badge.class);
	}

	@BeforeEach
	void setUp() {
		service = new BadgeCreateService(badgeMapper);
	}

	private static Map<String, Object> inputFor(String badgeName, Long badgeId) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("badge_name", badgeName);
		if (badgeId != null) {
			input.put("badge_id", badgeId);
		}
		return input;
	}

	private static Map<String, Object> operatorJwt(long companyId) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_id", 22L);
		jwt.put("company_id", companyId);
		jwt.put("mobile", "13900001001");
		return jwt;
	}

	private static Badge badge(long badgeId, long companyId, String name) {
		Badge b = new Badge();
		b.setBadgeId(badgeId);
		b.setCompanyId(companyId);
		b.setBadgeName(name);
		return b;
	}

	@Test
	void editingForeignCompanyBadge_isRejectedWithoutUpdate() {
		// 查重不命中（同租户无同名）；编辑目标是别租户徽章
		when(badgeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		when(badgeMapper.selectById(77L)).thenReturn(badge(77L, 99L, "优选"));

		assertThatThrownBy(
						() ->
								service.createOrUpdate(
										inputFor("新名称", 77L), operatorJwt(11L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该角标");

		verify(badgeMapper, never()).updateById(any(Badge.class));
	}

	@Test
	void sameNameOnForeignTenantBadge_createSucceeds() {
		// 他租户存在同名「优选」，本租户查重不应误拒
		when(badgeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

		Map<String, Object> out =
				service.createOrUpdate(inputFor("优选", null), operatorJwt(11L));

		assertThat(out.get("message")).isEqualTo("创建角标成功");
		verify(badgeMapper).insert(any(Badge.class));
	}

	@Test
	void sameNameWithinOwnTenant_isRejected() {
		when(badgeMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(badge(50L, 11L, "优选")));

		assertThatThrownBy(
						() ->
								service.createOrUpdate(
										inputFor("优选", null), operatorJwt(11L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("同名角标已存在");
	}

	@Test
	void selfUpdateWithSameName_updatesNormally() {
		when(badgeMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(badge(50L, 11L, "优选")));
		when(badgeMapper.selectById(50L)).thenReturn(badge(50L, 11L, "优选"));

		Map<String, Object> out =
				service.createOrUpdate(inputFor("优选", 50L), operatorJwt(11L));

		assertThat(out.get("message")).isEqualTo("更新角标成功");
		verify(badgeMapper).updateById(any(Badge.class));
	}

	@Test
	void missingBadge_isRejectedWithExistingMessage() {
		when(badgeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		when(badgeMapper.selectById(404L)).thenReturn(null);

		assertThatThrownBy(
						() ->
								service.createOrUpdate(
										inputFor("名称", 404L), operatorJwt(11L)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");
	}
}
