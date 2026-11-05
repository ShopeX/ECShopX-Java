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

package cn.shopex.ecshopx.members.service.systemtag;

public final class MemberSystemTagCodes {

	public static final String KIND_MANUAL = "manual";
	public static final String KIND_SYSTEM = "system";
	public static final String NEW_CUSTOMER = "new_customer";
	public static final String BIRTHDAY_THIS_MONTH = "birthday_this_month";
	public static final String SLEEPING_USER = "sleeping_user";
	public static final String NEW_CUSTOMER_NAME = "新客";
	public static final String BIRTHDAY_NAME = "当月生日";
	public static final String SLEEPING_USER_NAME = "沉睡用户";
	public static final String NEW_CUSTOMER_DESC = "当前没有有效已付订单的会员";
	public static final String BIRTHDAY_DESC = "生日月份等于当前自然月的会员";
	public static final String SLEEPING_USER_DESC = "距离上次完成订单超过 180 天的会员";
	public static final int RULE_VERSION = 1;
	/** 沉睡判定：超过 180 天（秒）才命中，等于不算。 */
	public static final long SLEEPING_THRESHOLD_SECONDS = 180L * 86400L;
	public static final String SOURCE_REGISTER = "register";
	public static final String SOURCE_PAY = "pay";
	public static final String SOURCE_REFUND = "refund";
	public static final String SOURCE_CANCEL = "cancel";
	public static final String SOURCE_BIND = "bind";
	public static final String SOURCE_DISABLE = "disable";
	public static final String SOURCE_ENABLE = "enable";
	public static final String SOURCE_COMPENSATE = "compensate";
	public static final String SOURCE_DAILY = "daily";
	public static final String ACTION_ATTACH = "attach";
	public static final String ACTION_DETACH = "detach";

	private MemberSystemTagCodes() {}

	public static boolean isReservedCode(String tagCode) {
		if (tagCode == null || tagCode.isBlank()) {
			return false;
		}
		String t = tagCode.trim();
		return NEW_CUSTOMER.equals(t) || BIRTHDAY_THIS_MONTH.equals(t) || SLEEPING_USER.equals(t);
	}

	public static boolean isSystemKind(String tagKind) {
		return KIND_SYSTEM.equals(tagKind);
	}
}
