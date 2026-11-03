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

package cn.shopex.ecshopx.common.auth;

/** Request attribute keys for POS JWT claims after successful authentication. */
public final class PosJwtRequestAttributes {

	public static final String POS_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.PosJwtAuthenticationFilter.POS_JWT_USER_DATA";

	/** Claim: token kind marker so Operator JWT and POS JWT are not interchangeable. */
	public static final String CLAIM_TOKEN_KIND = "token_kind";

	public static final String TOKEN_KIND_POS = "pos";

	private PosJwtRequestAttributes() {}
}
