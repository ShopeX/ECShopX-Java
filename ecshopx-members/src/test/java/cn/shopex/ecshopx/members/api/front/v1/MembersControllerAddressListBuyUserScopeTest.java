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

package cn.shopex.ecshopx.members.api.front.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** ECX-10923：地址列表的 buy_user_id 仅允许本人（自购），且不再回退 null。 */
class MembersControllerAddressListBuyUserScopeTest {

	private static HttpServletRequest requestWith(Map<String, String> params) {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Map<String, String[]> parameterMap = new LinkedHashMap<>();
		params.forEach((k, v) -> parameterMap.put(k, new String[] {v}));
		when(request.getParameterMap()).thenReturn(parameterMap);
		params.forEach((k, v) -> when(request.getParameter(k)).thenReturn(v));
		return request;
	}

	@Test
	void foreignBuyUserId_isRejected() {
		HttpServletRequest request =
				requestWith(Map.of("promoter_user_id", "22", "buy_user_id", "99"));

		assertThatThrownBy(
						() ->
								MembersController.resolveFilterUserIdForAddressList(
										request, "22", "99", 22L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该数据");
	}

	@Test
	void buyUserIdEqualToAuth_returnsAuthUser() {
		HttpServletRequest request =
				requestWith(Map.of("promoter_user_id", "22", "buy_user_id", "22"));

		assertThat(MembersController.resolveFilterUserIdForAddressList(request, "22", "22", 22L))
				.isEqualTo(22L);
	}

	@Test
	void missingBuyUserId_returnsAuthUser_notNull() {
		// null 旁路消除：不再回退 null 触发 isNull(userId) 孤儿地址查询
		HttpServletRequest request = requestWith(Map.of("promoter_user_id", "22"));

		assertThat(MembersController.resolveFilterUserIdForAddressList(request, "22", null, 22L))
				.isEqualTo(22L);
	}

	@Test
	void foreignPromoter_isIgnored() {
		HttpServletRequest request =
				requestWith(Map.of("promoter_user_id", "33", "buy_user_id", "99"));

		assertThat(MembersController.resolveFilterUserIdForAddressList(request, "33", "99", 22L))
				.isEqualTo(22L);
	}

	@Test
	void withoutPromoter_returnsAuthUser() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		when(request.getParameterMap()).thenReturn(Map.of());

		assertThat(MembersController.resolveFilterUserIdForAddressList(request, null, null, 22L))
				.isEqualTo(22L);
	}
}
