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

package cn.shopex.ecshopx.employeepurchase.support;

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import org.springframework.util.StringUtils;

/** 企业购购买方式辅助。 */
public final class PurchaseModeSupport {

	public static final String CASH = "cash";
	public static final String PREPAID_POINT = "prepaid_point";

	private PurchaseModeSupport() {}

	public static boolean isValid(String mode) {
		return CASH.equals(mode) || PREPAID_POINT.equals(mode);
	}

	public static boolean isPrepaidPoint(String mode) {
		return PREPAID_POINT.equals(mode);
	}

	public static boolean isPrepaidPoint(Activities activity) {
		return activity != null && isPrepaidPoint(activity.getPurchaseMode());
	}

	/** 有 purchase_mode 即为新契约活动。 */
	public static boolean isNewContractActivity(Activities activity) {
		return activity != null && StringUtils.hasText(activity.getPurchaseMode());
	}

	public static String desc(String mode) {
		if (PREPAID_POINT.equals(mode)) {
			return "预充点数";
		}
		if (CASH.equals(mode)) {
			return "现金";
		}
		return "";
	}

	public static String normalize(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim().toLowerCase();
	}
}
