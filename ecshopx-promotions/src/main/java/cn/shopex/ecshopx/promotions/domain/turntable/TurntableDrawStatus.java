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
 * 抽奖记录主状态（PRD §3.2）。
 */
public final class TurntableDrawStatus {

	public static final String PROCESSING = "PROCESSING";
	public static final String SUCCESS = "SUCCESS";
	public static final String GRANT_FAILED = "GRANT_FAILED";
	public static final String COST_FAILED = "COST_FAILED";

	private TurntableDrawStatus() {}
}
