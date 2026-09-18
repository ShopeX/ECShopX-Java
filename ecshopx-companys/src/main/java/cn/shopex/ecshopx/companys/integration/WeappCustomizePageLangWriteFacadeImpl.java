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

package cn.shopex.ecshopx.companys.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WeappCustomizePageLangWriteFacade;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WeappCustomizePageLangWriteFacadeImpl implements WeappCustomizePageLangWriteFacade {

	private static final String TABLE_WECHAT_WEAPP_CUSTOMIZE_PAGE = "wechat_weapp_customize_page";
	private static final String MODULE_WECHAT_WEAPP_CUSTOMIZE_PAGE = "wechat_weapp_customize_page";

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private final CommonLangModWriteService commonLangModWriteService;

	public WeappCustomizePageLangWriteFacadeImpl(CommonLangModWriteService commonLangModWriteService) {
		this.commonLangModWriteService = commonLangModWriteService;
	}

	@Override
	public void saveInitialSalespersonCustomizePageLangIfNeeded(long companyId, long dataId, String requestLang,
			Map<String, String> langFieldValues) {
		String lang = requestLang == null ? "" : requestLang.trim();
		if (!StringUtils.hasText(lang)) {
			return;
		}
		if (!LANG_ZH_CN.equalsIgnoreCase(lang) && !LANG_EN_CN.equalsIgnoreCase(lang) && !LANG_AR_SA.equalsIgnoreCase(lang)) {
			return;
		}

		String langRaw = extractPrimaryLanguageTag(lang);
		String canonical = canonicalCommonLangLocaleTag(langRaw);
		if (canonical == null) {
			canonical = LANG_ZH_CN;
		}
		if (LANG_ZH_CN.equals(canonical)) {
			return;
		}

		if (companyId < 1L || companyId > Integer.MAX_VALUE) {
			throw new ResourceException("商户标识超出有效范围");
		}
		if (dataId < 1L || dataId > Integer.MAX_VALUE) {
			throw new ResourceException("数据标识超出有效范围");
		}

		int cid = (int) companyId;
		int did = (int) dataId;

		if (LANG_EN_CN.equals(canonical)) {
			commonLangModWriteService.saveLang(cid, langFieldValues, TABLE_WECHAT_WEAPP_CUSTOMIZE_PAGE, did,
					MODULE_WECHAT_WEAPP_CUSTOMIZE_PAGE, LANG_EN_CN);
		} else if (LANG_AR_SA.equals(canonical)) {
			commonLangModWriteService.saveLang(cid, langFieldValues, TABLE_WECHAT_WEAPP_CUSTOMIZE_PAGE, did,
					MODULE_WECHAT_WEAPP_CUSTOMIZE_PAGE, LANG_AR_SA);
		}
	}

	private static String extractPrimaryLanguageTag(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String s = raw.trim();
		int comma = s.indexOf(',');
		if (comma >= 0) {
			s = s.substring(0, comma).trim();
		}
		int semi = s.indexOf(';');
		if (semi >= 0) {
			s = s.substring(0, semi).trim();
		}
		return s;
	}

	private static String canonicalCommonLangLocaleTag(String langRaw) {
		if (LANG_ZH_CN.equalsIgnoreCase(langRaw)) {
			return LANG_ZH_CN;
		}
		if (LANG_EN_CN.equalsIgnoreCase(langRaw)) {
			return LANG_EN_CN;
		}
		if (LANG_AR_SA.equalsIgnoreCase(langRaw)) {
			return LANG_AR_SA;
		}
		return null;
	}
}
