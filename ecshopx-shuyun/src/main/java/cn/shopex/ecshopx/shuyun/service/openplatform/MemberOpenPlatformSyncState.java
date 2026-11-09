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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import java.util.Map;

/**
 * OFFLINE-only：小程序与店务共用一次 member.register，以本地两字段互为「已入会」标记。对齐 PHP
 * {@code ShuyunOpenPlatformMemberSyncState}。
 */
public final class MemberOpenPlatformSyncState {

	private MemberOpenPlatformSyncState() {}

	/**
	 * @param memberRow members 行片段，需含 {@code shuyun_open_online_wxapp_sync_at}、{@code offline_reg_distributor}
	 */
	public static boolean isRegisteredWithOpenPlatform(Map<String, Object> memberRow) {
		if (memberRow == null || memberRow.isEmpty()) {
			return false;
		}
		return toPositiveInt(memberRow.get("shuyun_open_online_wxapp_sync_at")) > 0
				|| toPositiveInt(memberRow.get("offline_reg_distributor")) > 0;
	}

	private static int toPositiveInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
