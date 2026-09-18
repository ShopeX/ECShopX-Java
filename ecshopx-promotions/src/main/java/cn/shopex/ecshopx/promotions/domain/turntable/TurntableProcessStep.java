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
 * PROCESSING 子进度（PRD §3.2 / §6.4.1）。
 */
public final class TurntableProcessStep {

	public static final String CREATED = "CREATED";
	public static final String PRIZE_SELECTED = "PRIZE_SELECTED";
	public static final String DRAW_STOCK_RESERVED = "DRAW_STOCK_RESERVED";
	public static final String COUNT_RESERVED = "COUNT_RESERVED";
	public static final String POINT_DEDUCTED = "POINT_DEDUCTED";
	public static final String GRANTING = "GRANTING";

	private TurntableProcessStep() {}
}
