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

package cn.shopex.ecshopx.theme.support;

import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PagesTemplateSetRowMapper {

	public Map<String, Object> toRowMap(PagesTemplateSet entity) {
		return toRowMap(entity, null);
	}

	public Map<String, Object> toRowMap(PagesTemplateSet entity, Map<String, Object> tabBarLangSingleLocaleOrNull) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (entity.getId() != null) {
			m.put("id", entity.getId());
		}
		if (entity.getCompanyId() != null) {
			m.put("company_id", entity.getCompanyId());
		}
		if (entity.getRegionauthId() != null) {
			m.put("regionauth_id", entity.getRegionauthId());
		}
		if (entity.getIndexType() != null) {
			m.put("index_type", entity.getIndexType());
		}
		if (entity.getPagesTemplateId() != null) {
			m.put("pages_template_id", entity.getPagesTemplateId());
		}
		if (entity.getIsEnforceSync() != null) {
			m.put("is_enforce_sync", entity.getIsEnforceSync());
		}
		if (entity.getIsOpenRecommend() != null) {
			m.put("is_open_recommend", entity.getIsOpenRecommend());
		}
		if (entity.getIsOpenWechatappLocation() != null) {
			m.put("is_open_wechatapp_location", entity.getIsOpenWechatappLocation());
		}
		if (entity.getIsOpenScanQrcode() != null) {
			m.put("is_open_scan_qrcode", entity.getIsOpenScanQrcode());
		}
		m.put("tab_bar", entity.getTabBar());
		if (tabBarLangSingleLocaleOrNull != null && !tabBarLangSingleLocaleOrNull.isEmpty()) {
			m.put("tab_bar_lang", tabBarLangSingleLocaleOrNull);
		}
		if (entity.getIsOpenOfficialAccount() != null) {
			m.put("is_open_official_account", entity.getIsOpenOfficialAccount());
		}
		return m;
	}
}
