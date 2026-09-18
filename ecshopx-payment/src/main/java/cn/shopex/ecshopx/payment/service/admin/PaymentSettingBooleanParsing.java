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

package cn.shopex.ecshopx.payment.service.admin;

public final class PaymentSettingBooleanParsing {

	private PaymentSettingBooleanParsing() {}

	public static boolean strictTrueString(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(o).trim());
	}

	public static boolean looseTrueString(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(o).trim());
	}

	public static String offlinePayIsOpenString(Object o) {
		if (looseTrueString(o)) {
			return "true";
		}
		return "false";
	}

	public static boolean truthyForPaymentTypeOpenFlag(Object o) {
		return looseTrueString(o);
	}
}
