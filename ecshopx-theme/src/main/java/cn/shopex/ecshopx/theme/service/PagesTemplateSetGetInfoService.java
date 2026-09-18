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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.support.PagesTemplateSetRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagesTemplateSetGetInfoService {

	private final PagesTemplateSetMapper pagesTemplateSetMapper;
	private final PagesTemplateSetOutsideTabBarReadService pagesTemplateSetOutsideTabBarReadService;
	private final PagesTemplateSetRowMapper pagesTemplateSetRowMapper;
	private final LangueProperties langueProperties;

	public Optional<Map<String, Object>> setInfo(long companyId, String requestLang, long regionauthId) {
		return getInfo(companyId, requestLang, 0L, regionauthId);
	}

	public Optional<Map<String, Object>> getInfo(
			long companyId, String requestLang, long pagesTemplateId, long regionauthId) {
		LambdaQueryWrapper<PagesTemplateSet> q = new LambdaQueryWrapper<>();
		q.eq(PagesTemplateSet::getCompanyId, companyId)
				.eq(PagesTemplateSet::getRegionauthId, regionauthId)
				.eq(PagesTemplateSet::getPagesTemplateId, pagesTemplateId);

		PagesTemplateSet row = pagesTemplateSetMapper.selectOne(q);
		if (row == null) {
			return Optional.empty();
		}

		String canonicalLangTag = resolveCanonicalLangTag(requestLang);
		boolean defaultLang = langueProperties.isDefaultLang(requestLang);
		String modTabBar = null;
		// 默认语种读主表；非默认语种才叠加 outside_* overlay（与 Save 返回逻辑一致）
		if (!defaultLang && canonicalLangTag != null) {
			modTabBar = pagesTemplateSetOutsideTabBarReadService.findTabBarOverlay(
					(int) companyId, row.getId().longValue(), requestLang);
		}

		String baseTabBar = row.getTabBar();
		String displayTabBar;
		if (modTabBar != null && !isLooseEmpty(modTabBar)) {
			displayTabBar = modTabBar;
		} else {
			displayTabBar = baseTabBar;
		}

		Map<String, Object> tabBarLangArg = null;
		if (modTabBar != null && !isLooseEmpty(modTabBar)) {
			LinkedHashMap<String, Object> tabBarLang = new LinkedHashMap<>();
			tabBarLang.put(canonicalLangTag, modTabBar);
			tabBarLangArg = tabBarLang;
		}

		PagesTemplateSet merged = new PagesTemplateSet();
		merged.setId(row.getId());
		merged.setCompanyId(row.getCompanyId());
		merged.setRegionauthId(row.getRegionauthId());
		merged.setIndexType(row.getIndexType());
		merged.setPagesTemplateId(row.getPagesTemplateId());
		merged.setIsEnforceSync(row.getIsEnforceSync());
		merged.setIsOpenRecommend(row.getIsOpenRecommend());
		merged.setIsOpenWechatappLocation(row.getIsOpenWechatappLocation());
		merged.setIsOpenScanQrcode(row.getIsOpenScanQrcode());
		merged.setIsOpenOfficialAccount(row.getIsOpenOfficialAccount());
		merged.setTabBar(displayTabBar);

		return Optional.of(pagesTemplateSetRowMapper.toRowMap(merged, tabBarLangArg));
	}

	private static String resolveCanonicalLangTag(String requestLang) {
		if (requestLang == null) {
			return null;
		}
		String t = requestLang.trim();
		if (t.isEmpty()) {
			return null;
		}
		if ("zh-CN".equalsIgnoreCase(t)) {
			return "zh-CN";
		}
		if ("en-CN".equalsIgnoreCase(t)) {
			return "en-CN";
		}
		if ("ar-SA".equalsIgnoreCase(t)) {
			return "ar-SA";
		}
		return null;
	}

	private static boolean isLooseEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		return false;
	}
}
