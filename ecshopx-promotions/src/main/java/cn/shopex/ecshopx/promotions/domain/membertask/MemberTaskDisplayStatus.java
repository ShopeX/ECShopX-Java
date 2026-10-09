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

package cn.shopex.ecshopx.promotions.domain.membertask;

/** Admin 列表展示态（Api 英文枚举，对齐 PRD 待发布/待开始/进行中/已失效）。 */
public final class MemberTaskDisplayStatus {

	public static final String DRAFT = "draft";
	public static final String PENDING_START = "pending_start";
	public static final String IN_PROGRESS = "in_progress";
	public static final String EXPIRED = "expired";

	private MemberTaskDisplayStatus() {}

	public static String resolve(int published, int offShelf, int beginTime, int endTime, long nowEpochSec) {
		if (offShelf != 0) {
			return EXPIRED;
		}
		if (endTime > 0 && nowEpochSec > endTime) {
			return EXPIRED;
		}
		if (published == 0) {
			return DRAFT;
		}
		if (beginTime > 0 && nowEpochSec < beginTime) {
			return PENDING_START;
		}
		return IN_PROGRESS;
	}

	public static boolean isKnownFilter(String displayStatus) {
		if (displayStatus == null || displayStatus.isBlank()) {
			return true;
		}
		return DRAFT.equals(displayStatus)
				|| PENDING_START.equals(displayStatus)
				|| IN_PROGRESS.equals(displayStatus)
				|| EXPIRED.equals(displayStatus);
	}
}
