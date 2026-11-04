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

package cn.shopex.ecshopx.selfservice.api.front.v1;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordDateListService;
import cn.shopex.ecshopx.selfservice.service.UserDailyRecordPersonalRecordService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class FormTemplateControllerStatisticalAnalysisScopeTest {

	@Mock
	private UserDailyRecordDateListService userDailyRecordDateListService;

	@Mock
	private UserDailyRecordPersonalRecordService userDailyRecordPersonalRecordService;

	@Mock
	private MessageSource messageSource;

	private FormTemplateController controller;

	@BeforeEach
	void setUp() {
		controller =
				new FormTemplateController(
						userDailyRecordDateListService,
						userDailyRecordPersonalRecordService,
						messageSource);
	}

	@Test
	void statisticalAnalysis_ignoresQueryUserId_andUsesJwtUser() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("company_id", 10L);
		claims.put("user_id", 100L);
		request.setAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS, claims);

		when(userDailyRecordPersonalRecordService.statisticalAnalysis(
						anyLong(), anyLong(), anyInt(), anyInt(), isNull(), anyString()))
				.thenReturn(Map.of("list", java.util.List.of(), "keyindex", java.util.List.of()));

		// Extra query user_id must not affect the bound member (Spring still accepts unknown params).
		request.setParameter("user_id", "999");

		controller.statisticalAnalysis(
				request, "5", null, "physical", null, Locale.SIMPLIFIED_CHINESE);

		verify(userDailyRecordPersonalRecordService)
				.statisticalAnalysis(eq(10L), eq(100L), eq(5), anyInt(), isNull(), eq("physical"));
	}
}
