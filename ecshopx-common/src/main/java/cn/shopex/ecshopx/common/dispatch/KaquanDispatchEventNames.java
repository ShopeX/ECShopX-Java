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

package cn.shopex.ecshopx.common.dispatch;

public final class KaquanDispatchEventNames {

	public static final String EVENT_COUPON_ADD = "event:283:KaquanBundle\\Events\\CouponAddEvent";

	/** New-gift card create path; distinct Bus message name from {@link #EVENT_COUPON_ADD}. */
	public static final String EVENT_COUPON_ADD_NEW_GIFT = "event:284:KaquanBundle\\Events\\CouponAddEvent";

	/** Standard discount card PATCH update path; distinct from any future new_gift edit Bus name. */
	public static final String EVENT_COUPON_EDIT = "event:285:KaquanBundle\\Events\\CouponEditEvent";

	/**
	 * New-gift card PATCH update path; distinct Bus {@code messageName} from {@link #EVENT_COUPON_EDIT}
	 * (same class name suffix as 285, different event id), analogous to {@link #EVENT_COUPON_ADD} /
	 * {@link #EVENT_COUPON_ADD_NEW_GIFT}.
	 */
	public static final String EVENT_COUPON_EDIT_NEW_GIFT = "event:286:KaquanBundle\\Events\\CouponEditEvent";

	public static final String EVENT_COUPON_DELETE = "event:287:KaquanBundle\\Events\\CouponDeleteEvent";

	public static final String LISTENER_YOUSHU_BUNDLE_COUPON = "listener:YoushuBundle\\Listeners\\Coupon";

	private KaquanDispatchEventNames() {}
}
