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

package cn.shopex.ecshopx.common.wechat;

/**
 * Resolves a member's WeChat {@code open_id} for a given mini-program authorizer app id (cross-module seam).
 */
public interface WxaMemberOpenIdLookupPort {

	/**
	 * @param userId       member user id
	 * @param authorizerAppid mini program authorizer app id
	 * @return trimmed open_id, or empty string when not found
	 */
	String resolveOpenId(long userId, String authorizerAppid);
}
