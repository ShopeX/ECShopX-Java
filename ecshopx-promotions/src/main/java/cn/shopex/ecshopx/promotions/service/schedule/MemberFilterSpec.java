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

import java.util.Objects;

/**
 * 由活动类型与 {@code trigger_condition} 解析出的会员过滤条件。
 */
public final class MemberFilterSpec {

	public final MemberFilterSpecKind kind;
	/** 生日/入会 月份 1-12，{@link MemberFilterSpecKind#ALL} 时忽略 */
	public final int month;
	/** 日 1-31 或周窗口下界 */
	public final int dayOrFrom;
	/** 周窗口上界，非周模式可与 dayOrFrom 相同 */
	public final int dayTo;

	public MemberFilterSpec(MemberFilterSpecKind kind, int month, int dayOrFrom, int dayTo) {
		this.kind = Objects.requireNonNull(kind);
		this.month = month;
		this.dayOrFrom = dayOrFrom;
		this.dayTo = dayTo;
	}

	public static MemberFilterSpec all() {
		return new MemberFilterSpec(MemberFilterSpecKind.ALL, 0, 0, 0);
	}
}
