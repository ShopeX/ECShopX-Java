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

package cn.shopex.ecshopx.promotions.api.front.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ECX-10876：砍价助力身份必须来自登录 claims。 */
class UserBargainsBargainLogIdentityTest {

	@Test
	void bindHelperIdentity_rejectsWhenUnauthenticated() {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("open_id", "spoofed");

		assertThatThrownBy(() -> UserBargainsController.bindHelperIdentityFromClaims(merged, Map.of()))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("Unable to authenticate user.");
	}

	@Test
	void bindHelperIdentity_overwritesBodyOpenIdWithClaims() {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("open_id", "spoofed-open-id");
		merged.put("nickname", "fake");
		merged.put("headimgurl", "http://evil/a.png");
		merged.put("user_id", "99");

		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("user_id", 7L);
		claims.put("open_id", "real-open-id");
		claims.put("nickname", "真实用户");
		claims.put("headimgurl", "http://cdn/real.png");

		UserBargainsController.bindHelperIdentityFromClaims(merged, claims);

		assertThat(merged.get("open_id")).isEqualTo("real-open-id");
		assertThat(merged.get("nickname")).isEqualTo("真实用户");
		assertThat(merged.get("headimgurl")).isEqualTo("http://cdn/real.png");
		assertThat(merged.get("user_id")).isEqualTo("99");
	}
}
