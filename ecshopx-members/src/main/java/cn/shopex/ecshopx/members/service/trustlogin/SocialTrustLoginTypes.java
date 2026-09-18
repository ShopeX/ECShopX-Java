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

package cn.shopex.ecshopx.members.service.trustlogin;

import java.util.List;
import java.util.Set;

/** 海外社交信任登录类型（对齐 PHP SocialTrustLoginService::SOCIAL_TYPES）。 */
public final class SocialTrustLoginTypes {

	public static final List<String> SOCIAL_TYPES = List.of("apple", "google", "facebook", "line");

	private static final Set<String> SOCIAL_SET = Set.copyOf(SOCIAL_TYPES);

	private SocialTrustLoginTypes() {}

	public static boolean isSocial(String trustloginTag) {
		if (trustloginTag == null) {
			return false;
		}
		return SOCIAL_SET.contains(trustloginTag.trim().toLowerCase());
	}
}
