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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

/** 口令通道常量 */
public final class PassphraseConstants {

	public static final String BIND_CHANNEL_PASSPHRASE = "passphrase";

	public static final String BEHAVIOR_SCAN = "scan";
	public static final String BEHAVIOR_PASSPHRASE_VERIFY = "passphrase_verify";
	public static final String BEHAVIOR_BIND = "bind";
	public static final String BEHAVIOR_ORDER = "order";

	public static final String RESULT_SUCCESS = "success";
	public static final String RESULT_FAIL = "fail";

	private PassphraseConstants() {}
}
