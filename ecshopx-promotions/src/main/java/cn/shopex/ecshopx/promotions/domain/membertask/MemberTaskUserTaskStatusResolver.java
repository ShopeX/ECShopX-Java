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

/** 由 completion + grant 推导 C 端态（SSOT §11.2）。 */
public final class MemberTaskUserTaskStatusResolver {

	private MemberTaskUserTaskStatusResolver() {}

	public static String resolve(String grantStatus) {
		if (grantStatus == null || grantStatus.isBlank()) {
			return MemberTaskUserTaskStatus.TODO;
		}
		return switch (grantStatus) {
			case "pending" -> MemberTaskUserTaskStatus.PENDING_REWARD;
			case "granted" -> MemberTaskUserTaskStatus.REWARDED;
			case "failed" -> MemberTaskUserTaskStatus.REWARD_FAILED;
			case "cancelled" -> MemberTaskUserTaskStatus.TODO;
			default -> MemberTaskUserTaskStatus.TODO;
		};
	}

	public static boolean shouldShowInFrontList(String activityDisplay, String userTaskStatus) {
		if (MemberTaskDisplayStatus.IN_PROGRESS.equals(activityDisplay)) {
			return true;
		}
		if (MemberTaskDisplayStatus.PENDING_START.equals(activityDisplay)) {
			return !MemberTaskUserTaskStatus.TODO.equals(userTaskStatus);
		}
		if (MemberTaskDisplayStatus.EXPIRED.equals(activityDisplay)) {
			return !MemberTaskUserTaskStatus.TODO.equals(userTaskStatus);
		}
		return false;
	}
}
