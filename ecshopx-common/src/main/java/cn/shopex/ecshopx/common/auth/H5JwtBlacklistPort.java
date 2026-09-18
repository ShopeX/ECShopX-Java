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

/**
 * H5 端 JWT 按紧凑字符串黑名单：刷新作废后后续请求在验签前拒绝同一 token。
 */
public interface H5JwtBlacklistPort {

	/** Message returned on refresh when the compact JWT is treated as invalidated. */
	String TOKEN_BLACKLISTED_MESSAGE = "The token has been blacklisted";

	/**
	 * 将紧凑 JWT 加入黑名单，存活至 {@code expirationEpochSeconds}（Unix 秒）对应的时刻。
	 *
	 * @param compactJwt Bearer 前缀之后的完整 JWT 字符串
	 * @param expirationEpochSeconds 该 JWT 的 exp（Unix 秒）
	 */
	void addToBlacklist(String compactJwt, long expirationEpochSeconds);

	/**
	 * @param compactJwt Bearer 前缀之后的完整 JWT 字符串
	 * @return 是否已在黑名单中
	 */
	boolean isBlacklisted(String compactJwt);
}
