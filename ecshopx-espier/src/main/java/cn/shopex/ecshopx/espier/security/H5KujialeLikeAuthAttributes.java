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

package cn.shopex.ecshopx.espier.security;

/**
 * Request attributes set by {@link H5KujialeLikeJwtAuthenticationFilter} for the H5 kujiale like endpoint.
 */
public final class H5KujialeLikeAuthAttributes {

	private H5KujialeLikeAuthAttributes() {}

	/** JWT claims map ({@link java.util.Map}) for the authenticated H5 user. */
	public static final String JWT_CLAIMS = "cn.shopex.ecshopx.espier.security.H5_KUJIALE_LIKE_JWT_CLAIMS";
}
