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

package cn.shopex.ecshopx.reservation.domain;

import java.util.List;
import java.util.Map;

/**
 * 有预约时段时 GET /api/v1/reservation/period 的 data 体；无数据时 Controller 返回空 Map，不使用本类型。
 */
public record ReservationEveryDayTimePeriodPayload(
		List<Map<String, String>> tableTitle, String maxLimitDay, String minLimitHour) {}
