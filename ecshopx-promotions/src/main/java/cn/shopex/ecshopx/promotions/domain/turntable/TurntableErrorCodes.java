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

package cn.shopex.ecshopx.promotions.domain.turntable;

/**
 * 抽奖业务错误码（PRD §6.6 / INT-01）。
 */
public final class TurntableErrorCodes {

	public static final String ACTIVITY_NOT_FOUND = "LUCKY_DRAW_ACTIVITY_NOT_FOUND";
	public static final String NOT_STARTED = "LUCKY_DRAW_NOT_STARTED";
	public static final String ENDED = "LUCKY_DRAW_ENDED";
	public static final String TOTAL_LIMIT = "LUCKY_DRAW_TOTAL_LIMIT";
	public static final String DAILY_LIMIT = "LUCKY_DRAW_DAILY_LIMIT";
	public static final String POINT_NOT_ENOUGH = "LUCKY_DRAW_POINT_NOT_ENOUGH";
	public static final String REQUEST_ID_INVALID = "LUCKY_DRAW_REQUEST_ID_INVALID";
	public static final String GRANT_FAILED = "LUCKY_DRAW_GRANT_FAILED";
	public static final String COST_FAILED = "LUCKY_DRAW_COST_FAILED";
	public static final String CONFIG_VERSION_CONFLICT = "LUCKY_DRAW_CONFIG_VERSION_CONFLICT";

	private TurntableErrorCodes() {}
}
