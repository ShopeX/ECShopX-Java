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

package cn.shopex.ecshopx.wsugc.api.admin.v1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.wsugc.service.topic.TopicCreateService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicDeleteService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicDetailService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicListService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicSetTopService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicVerifyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;

/** ECX-10943：删除话题使用可信 JWT 租户上下文。 */
@ExtendWith(MockitoExtension.class)
class TopicDeleteControllerScopeTest {

	@Mock
	private TopicCreateService topicCreateService;

	@Mock
	private TopicDetailService topicDetailService;

	@Mock
	private TopicListService topicListService;

	@Mock
	private TopicSetTopService topicSetTopService;

	@Mock
	private TopicVerifyService topicVerifyService;

	@Mock
	private TopicDeleteService topicDeleteService;

	@Mock
	private OperatorLogsWriteService operatorLogsWriteService;

	@Mock
	private Environment environment;

	private TopicController controller;

	@BeforeEach
	void setUp() {
		controller = new TopicController(
				topicCreateService,
				topicDetailService,
				topicListService,
				topicSetTopService,
				topicVerifyService,
				topicDeleteService,
				operatorLogsWriteService,
				new ObjectMapper(),
				environment);
	}

	@Test
	void deleteTopic_passesJwtCompanyIdToService() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/json");
		request.setAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA, Map.of("company_id", "100", "operator_id", 7L));
		Map<String, Object> body = Map.of("topic_id", List.of(10L, 11L));
		when(topicDeleteService.deleteTopics(body, 100L)).thenReturn(Map.of("message", "删除成功"));

		controller.deleteTopic(request, body);

		verify(topicDeleteService).deleteTopics(anyMap(), eq(100L));
	}

	@Test
	void deleteTopic_missingCompanyId_isRejectedBeforeDelete() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA, Map.of("operator_id", 7L));

		assertThatThrownBy(() -> controller.deleteTopic(request, Map.of("topic_id", 10L)))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("未登录");
		verify(topicDeleteService, never()).deleteTopics(anyMap(), anyLong());
	}

	@Test
	void deleteTopic_invalidCompanyId_isRejectedBeforeDelete() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA, Map.of("company_id", "invalid", "operator_id", 7L));

		assertThatThrownBy(() -> controller.deleteTopic(request, Map.of("topic_id", 10L)))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("未登录");
		verify(topicDeleteService, never()).deleteTopics(anyMap(), anyLong());
	}
}
