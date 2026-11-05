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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-10913：创建地址的 buy_user_id 仅允许本人（自购）。 */
class MembersControllerAddressBuyUserScopeTest {

	@Test
	void withoutBuyUserId_returnsAuthUser() {
		Map<String, Object> b = new LinkedHashMap<>();

		assertThat(MembersController.resolveEffectiveUserIdForCreate(b, 22L)).isEqualTo(22L);
	}

	@Test
	void buyUserIdEqualToAuth_returnsAuthUser() {
		Map<String, Object> b = new LinkedHashMap<>();
		b.put("promoter_user_id", 22L);
		b.put("buy_user_id", 22L);

		assertThat(MembersController.resolveEffectiveUserIdForCreate(b, 22L)).isEqualTo(22L);
	}

	@Test
	void buyUserIdOfAnotherUser_isRejected() {
		Map<String, Object> b = new LinkedHashMap<>();
		b.put("promoter_user_id", 22L);
		b.put("buy_user_id", 99L);

		assertThatThrownBy(() -> MembersController.resolveEffectiveUserIdForCreate(b, 22L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该数据");
	}

	@Test
	void foreignPromoter_isIgnored() {
		Map<String, Object> b = new LinkedHashMap<>();
		b.put("promoter_user_id", 33L);
		b.put("buy_user_id", 99L);

		// promoter 不等于本人时按本人处理（既有语义：非代客请求）
		assertThat(MembersController.resolveEffectiveUserIdForCreate(b, 22L)).isEqualTo(22L);
	}
}
