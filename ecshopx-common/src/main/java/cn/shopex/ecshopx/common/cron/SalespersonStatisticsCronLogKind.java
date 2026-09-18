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

package cn.shopex.ecshopx.common.cron;

/**
 * 导购相关定时任务中调试日志场景（与 PHP Job 文案一一对应）。
 */
public enum SalespersonStatisticsCronLogKind {

	/** 导购日统计（shopping_guide Job） */
	SHOPPING_GUIDE_DAILY,

	/** 导购分润统计 Job（拉新分润 newGuestDivided） */
	SHOPPING_GUIDE_COMMISSION,

	/** 导购推广统计 Job（salesCommission / member） */
	SHOPPING_GUIDE_POPULARIZE,

	/** 导购赠券统计 Job（送券表 COUNT + salespersonGiveCoupons/member） */
	SHOPPING_GUIDE_GIVE_COUPONS,

	/** 活动转发次数统计 Job */
	ACTIVE_ARTICLE_FORWARD,
}
