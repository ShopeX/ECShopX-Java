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

package cn.shopex.ecshopx.members.service.h5.support;

/**
 * Normalized interpretation of H5 wxapp query parameters (activity id, invite code) for branching logic:
 * blank, {@code "0"}, or unparsable activity id are treated as absent; invite code is absent when null/blank.
 */
public final class WxappMemberH5QueryParamEmpty {

	private WxappMemberH5QueryParamEmpty() {}

	public static boolean isActivityIdTruthy(String activityIdRaw) {
		if (activityIdRaw == null) {
			return false;
		}
		String t = activityIdRaw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equals(t);
	}

	public static boolean isInviteCodeTruthy(String inviteCodeRaw) {
		return inviteCodeRaw != null && !inviteCodeRaw.trim().isEmpty();
	}

	public static long parseActivityIdAsLong(String activityIdRaw) {
		if (!isActivityIdTruthy(activityIdRaw)) {
			return 0L;
		}
		String t = activityIdRaw.trim();
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
