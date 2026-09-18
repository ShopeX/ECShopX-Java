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

package cn.shopex.ecshopx.companys.domain.language;

/** 语言设置稳定错误码（ECX-10021 §6.3.3）。 */
public final class LanguageSettingErrorCodes {

	public static final String ALL_DISABLED = "LANGUAGE_ALL_DISABLED";
	public static final String DEFAULT_REQUIRED = "LANGUAGE_DEFAULT_REQUIRED";
	public static final String DEFAULT_NOT_ENABLED = "LANGUAGE_DEFAULT_NOT_ENABLED";
	public static final String INVALID_CODE = "LANGUAGE_INVALID_CODE";
	public static final String VERSION_CONFLICT = "LANGUAGE_CONFIG_VERSION_CONFLICT";
	public static final String NOT_SUPPORTED_BY_DEPLOYMENT = "LANGUAGE_NOT_SUPPORTED_BY_DEPLOYMENT";

	private LanguageSettingErrorCodes() {}
}
