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

package cn.shopex.ecshopx.wsugc.service.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Tag;
import cn.shopex.ecshopx.wsugc.mapper.TagMapper;
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

/** ECX-10935：标签审核必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class TagVerifyServiceCompanyScopeTest {

	@Mock
	private TagMapper tagMapper;

	private TagVerifyService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Tag.class);
	}

	@BeforeEach
	void setUp() {
		service = new TagVerifyService(tagMapper);
	}

	private static Map<String, Object> inputFor(Object tagIds) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("tag_id", tagIds);
		input.put("status", 1);
		return input;
	}

	private static Tag tag(long tagId, long companyId) {
		Tag tag = new Tag();
		tag.setTagId(tagId);
		tag.setCompanyId(companyId);
		return tag;
	}

	@Test
	void foreignCompanyTag_isRejectedWithoutUpdate() {
		when(tagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tag(5L, 99L)));

		assertThatThrownBy(() -> service.verify(inputFor(5L), Map.of("company_id", 11L), false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该标签");

		verify(tagMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	void mixedCompanyTags_areRejectedWithoutUpdate() {
		when(tagMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(tag(5L, 11L), tag(6L, 99L)));

		assertThatThrownBy(() -> service.verify(inputFor(List.of(5L, 6L)), Map.of("company_id", 11L), false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该标签");

		verify(tagMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	void missingTag_isRejectedWithoutUpdate() {
		when(tagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tag(5L, 11L)));

		assertThatThrownBy(() -> service.verify(inputFor(List.of(5L, 404L)), Map.of("company_id", 11L), false))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该标签");

		verify(tagMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	void ownCompanyTags_updateNormally() {
		when(tagMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(tag(5L, 11L), tag(6L, 11L)));

		Map<String, Object> result =
				service.verify(inputFor(List.of(5L, 6L)), Map.of("company_id", 11L), false);

		assertThat(result.get("message")).isEqualTo("审核成功");
		assertThat(result.get("tag_id")).isEqualTo(List.of(5L, 6L));
		verify(tagMapper).update(isNull(), any(Wrapper.class));
	}
}
