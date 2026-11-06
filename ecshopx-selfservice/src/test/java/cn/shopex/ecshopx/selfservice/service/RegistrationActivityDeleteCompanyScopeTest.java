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

package cn.shopex.ecshopx.selfservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
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
import org.springframework.dao.DataIntegrityViolationException;

/** ECX-10928：删除报名活动必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationActivityDeleteCompanyScopeTest {

	@Mock
	private RegistrationActivityMapper registrationActivityMapper;

	private RegistrationActivityDeleteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), RegistrationActivity.class);
	}

	@BeforeEach
	void setUp() {
		service = new RegistrationActivityDeleteService(registrationActivityMapper);
	}

	private static HttpServletRequest requestWithActivityId(String activityId) {
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		when(request.getContentType()).thenReturn("application/json");
		when(request.getParameter("activity_id")).thenReturn(activityId);
		return request;
	}

	@Test
	void foreignCompanyActivity_isRejectedWithoutDelete() {
		when(registrationActivityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		assertThatThrownBy(
						() ->
								service.deleteData(
										requestWithActivityId("77"),
										Map.of("activity_id", "77"),
										11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该报名活动");

		verify(registrationActivityMapper, never()).deleteById(77L);
	}

	@Test
	void missingActivity_isRejectedWithoutDelete() {
		when(registrationActivityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

		assertThatThrownBy(
						() ->
								service.deleteData(
										requestWithActivityId("404"),
										Map.of("activity_id", "404"),
										11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该报名活动");
	}

	@Test
	void ownCompanyActivity_deletesNormally() {
		when(registrationActivityMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

		Object out =
				service.deleteData(
						requestWithActivityId("77"), Map.of("activity_id", "77"), 11L);

		assertThat(out).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> row = (Map<String, Object>) out;
		assertThat(row.get("status")).isEqualTo(Boolean.TRUE);
		verify(registrationActivityMapper).deleteById(77L);
	}

	@Test
	void invalidActivityId_returnsEmptyList() {
		Object out =
				service.deleteData(
						requestWithActivityId("abc"), Map.of("activity_id", "abc"), 11L);

		assertThat(out).isEqualTo(Collections.emptyList());
		verify(registrationActivityMapper, never()).deleteById(any(Long.class));
	}

	@Test
	void foreignKeyViolation_keepsExistingMessage() {
		when(registrationActivityMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		when(registrationActivityMapper.deleteById(77L))
				.thenThrow(new DataIntegrityViolationException("fk"));

		assertThatThrownBy(
						() ->
								service.deleteData(
										requestWithActivityId("77"),
										Map.of("activity_id", "77"),
										11L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无法删除该活动");
	}
}
