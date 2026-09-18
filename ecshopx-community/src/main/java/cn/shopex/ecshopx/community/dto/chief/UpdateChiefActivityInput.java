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

package cn.shopex.ecshopx.community.dto.chief;

import java.util.List;

/**
 * 团长修改活动入参（Controller 组装后传入 Service）。
 */
public record UpdateChiefActivityInput(
		long activityId,
		long companyId,
		long chiefId,
		String activityName,
		int startTime,
		int endTime,
		String activityPicsOrNull,
		String activityDescOrNull,
		boolean activityIntroKeyPresent,
		String activityIntroOrNull,
		String activityStatusOrNull,
		Integer distributorIdFromBodyOrNull,
		List<Long> itemIds,
		List<ChiefActivityZitiInput> zitiRows) {}
