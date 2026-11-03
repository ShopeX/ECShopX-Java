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

package cn.shopex.ecshopx.companys.service.posauth;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/** ECX-10532：POS 鉴权文案（MessageSource + 中文 fallback）。 */
public final class PosAuthMessages {

	static final String BAD_CREDENTIALS = "pos.auth.bad_credentials";
	static final String ACCOUNT_DISABLED = "pos.auth.account_disabled";
	static final String ACCOUNT_RESIGNED = "pos.auth.account_resigned";
	static final String ACCOUNT_DATA_ABNORMAL = "pos.auth.account_data_abnormal";
	static final String LOGIN_INVALID = "pos.auth.login_invalid";
	static final String SHOP_CLOSED = "pos.auth.shop_closed";
	static final String SHOP_DELETED = "pos.auth.shop_deleted";
	static final String SHOP_MISSING = "pos.auth.shop_missing";

	private PosAuthMessages() {}

	public static String badCredentials(MessageSource messageSource) {
		return msg(messageSource, BAD_CREDENTIALS, "账号或密码错误");
	}

	public static String accountDisabled(MessageSource messageSource) {
		return msg(messageSource, ACCOUNT_DISABLED, "账号已禁用");
	}

	public static String accountResigned(MessageSource messageSource) {
		return msg(messageSource, ACCOUNT_RESIGNED, "账号已离职");
	}

	public static String accountDataAbnormal(MessageSource messageSource) {
		return msg(messageSource, ACCOUNT_DATA_ABNORMAL, "账号数据异常，请联系管理员");
	}

	public static String loginInvalid(MessageSource messageSource) {
		return msg(messageSource, LOGIN_INVALID, "登录验证错误");
	}

	public static String shopClosed(MessageSource messageSource) {
		return msg(messageSource, SHOP_CLOSED, "门店已闭店，无法登录");
	}

	public static String shopDeleted(MessageSource messageSource) {
		return msg(messageSource, SHOP_DELETED, "门店已撤店，无法登录");
	}

	public static String shopMissing(MessageSource messageSource) {
		return msg(messageSource, SHOP_MISSING, "门店不存在，无法登录");
	}

	private static String msg(MessageSource messageSource, String key, String zhFallback) {
		Locale locale = LocaleContextHolder.getLocale();
		if (locale == null) {
			locale = Locale.SIMPLIFIED_CHINESE;
		}
		if (messageSource == null) {
			return zhFallback;
		}
		return messageSource.getMessage(key, null, zhFallback, locale);
	}
}
