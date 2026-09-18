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

package cn.shopex.ecshopx.point.service;

/**
 * 大转盘扣积分幂等查询结果（PRD §6.4.1）。
 *
 * <p>仅基于本地 {@code point_member_log.order_id=requestId} 且 {@code outcome&gt;0} 判定；
 * 无法从 DB 单独区分「明确失败」与「不存在」，缺失一律为 {@link #ABSENT}。
 *
 * <p>补偿任务 {@code COUNT_RESERVED} 语义：
 * <ul>
 *   <li>{@link #SUCCESS} → 可推进 {@code process_step=POINT_DEDUCTED}，继续发奖或 thanks 终态</li>
 *   <li>{@link #ABSENT} → 视为 UNKNOWN，保持 {@code PROCESSING}，禁止再次扣积分</li>
 * </ul>
 */
public enum TurntableDrawCostQueryResult {
	SUCCESS,
	ABSENT
}
