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

package cn.shopex.ecshopx.promotions.service.schedule;

/**
 * 会员统计/列表在 {@code members} + {@code members_info} 上的附加过滤（固定公司、手机号与会员卡非空在 SQL 中统一追加）。
 */
public enum MemberFilterSpecKind {
	/** 仅公司维度（如会员日） */
	ALL,
	BIRTHDAY_MONTH,
	BIRTHDAY_WEEK,
	BIRTHDAY_DAY,
	ANNIVERSARY_MONTH,
	ANNIVERSARY_WEEK,
	ANNIVERSARY_DAY
}
