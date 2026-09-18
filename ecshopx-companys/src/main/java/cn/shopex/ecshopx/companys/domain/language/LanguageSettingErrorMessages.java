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

import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public final class LanguageSettingErrorMessages {

	private static final Map<String, String> I18N_KEYS =
			Map.ofEntries(
					Map.entry(
							LanguageSettingErrorCodes.ALL_DISABLED,
							"companys.language_setting.all_disabled"),
					Map.entry(
							LanguageSettingErrorCodes.DEFAULT_REQUIRED,
							"companys.language_setting.default_required"),
					Map.entry(
							LanguageSettingErrorCodes.DEFAULT_NOT_ENABLED,
							"companys.language_setting.default_not_enabled"),
					Map.entry(
							LanguageSettingErrorCodes.INVALID_CODE,
							"companys.language_setting.invalid_code"),
					Map.entry(
							LanguageSettingErrorCodes.VERSION_CONFLICT,
							"companys.language_setting.version_conflict"),
					Map.entry(
							LanguageSettingErrorCodes.NOT_SUPPORTED_BY_DEPLOYMENT,
							"companys.language_setting.not_supported_by_deployment"));

	private static final Map<String, String> ZH_CN_FALLBACKS =
			Map.ofEntries(
					Map.entry(LanguageSettingErrorCodes.ALL_DISABLED, "至少开启一种语言后才能保存"),
					Map.entry(LanguageSettingErrorCodes.DEFAULT_REQUIRED, "请选择默认语言后再保存"),
					Map.entry(LanguageSettingErrorCodes.DEFAULT_NOT_ENABLED, "默认语言必须是已开启语言"),
					Map.entry(LanguageSettingErrorCodes.INVALID_CODE, "语言标识无效"),
					Map.entry(
							LanguageSettingErrorCodes.VERSION_CONFLICT,
							"配置已被他人修改，请刷新后重试"),
					Map.entry(
							LanguageSettingErrorCodes.NOT_SUPPORTED_BY_DEPLOYMENT,
							"当前部署不支持该语言"));

	public static final String SAVED_KEY = "companys.language_setting.saved";
	public static final String SAVED_FALLBACK = "语言设置已保存";

	public static final String CONFIG_WARNING_KEY = "companys.language_setting.config_warning";
	public static final String CONFIG_WARNING_FALLBACK = "默认语言未启用，请保存修正配置";

	private LanguageSettingErrorMessages() {}

	public static String message(MessageSource messageSource, String errorCode) {
		return message(messageSource, errorCode, LocaleContextHolder.getLocale());
	}

	public static String message(MessageSource messageSource, String errorCode, Locale locale) {
		if (messageSource == null || errorCode == null) {
			return errorCode;
		}
		String key = I18N_KEYS.get(errorCode);
		if (key == null) {
			return errorCode;
		}
		Locale effective = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
		String fallback = ZH_CN_FALLBACKS.getOrDefault(errorCode, errorCode);
		return messageSource.getMessage(key, null, fallback, effective);
	}

	public static String savedMessage(MessageSource messageSource) {
		if (messageSource == null) {
			return SAVED_FALLBACK;
		}
		return messageSource.getMessage(
				SAVED_KEY, null, SAVED_FALLBACK, LocaleContextHolder.getLocale());
	}

	public static String configWarningMessage(MessageSource messageSource) {
		if (messageSource == null) {
			return CONFIG_WARNING_FALLBACK;
		}
		return messageSource.getMessage(
				CONFIG_WARNING_KEY, null, CONFIG_WARNING_FALLBACK, LocaleContextHolder.getLocale());
	}
}
