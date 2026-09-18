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

package cn.shopex.ecshopx.selfservice.support;

/**
 * MessageSource keys for front cancel registration record API.
 */
public final class RegistrationActivityFrontCancelRecordMessageKeys {

	public static final String PLEASE_LOGIN_BEFORE_CANCEL = "selfservice.front.cancel_record.please_login_before_cancel";

	public static final String PLEASE_SPECIFY_REGISTRATION_RECORD_ID =
			"selfservice.front.cancel_record.please_specify_registration_record_id";

	public static final String REGISTRATION_STATUS_CANNOT_CANCEL =
			"selfservice.front.cancel_record.registration_status_cannot_cancel";

	public static final String ACTIVITY_NOT_ALLOW_CANCEL = "selfservice.front.cancel_record.activity_not_allow_cancel";

	private RegistrationActivityFrontCancelRecordMessageKeys() {
	}
}
