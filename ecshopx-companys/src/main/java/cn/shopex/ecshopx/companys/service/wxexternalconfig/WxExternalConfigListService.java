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

package cn.shopex.ecshopx.companys.service.wxexternalconfig;

import cn.shopex.ecshopx.companys.domain.WxExternalConfig;
import cn.shopex.ecshopx.companys.mapper.WxExternalConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxExternalConfigListService {

	private final WxExternalConfigMapper wxExternalConfigMapper;

	public WxExternalConfigListService(WxExternalConfigMapper wxExternalConfigMapper) {
		this.wxExternalConfigMapper = wxExternalConfigMapper;
	}

	public Map<String, Object> getWxExternalConfigList(long companyId, String appNameRaw, int page, int pageSize) {
		int p = page < 1 ? 1 : page;
		int ps = pageSize < 1 ? 20 : pageSize;

		LambdaQueryWrapper<WxExternalConfig> w = new LambdaQueryWrapper<>();
		w.eq(WxExternalConfig::getCompanyId, companyId);

		String t = appNameRaw == null ? "" : appNameRaw.trim();
		if (!t.isEmpty()) {
			w.like(WxExternalConfig::getAppName, "%" + escapeLike(t) + "%");
		}

		w.orderByDesc(WxExternalConfig::getCreatedAt);

		Long totalLong = wxExternalConfigMapper.selectCount(w);
		long totalCount = totalLong == null ? 0L : totalLong;

		if (totalCount == 0L) {
			return Map.of("total_count", 0L, "list", List.of());
		}

		Page<WxExternalConfig> mpPage = new Page<>(p, ps, false);
		wxExternalConfigMapper.selectPage(mpPage, w);

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (WxExternalConfig e : mpPage.getRecords()) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			Long rowId = e.getWxExternalConfigId();
			row.put("wx_external_config_id", rowId == null ? null : String.valueOf(rowId));
			row.put("company_id", String.valueOf(e.getCompanyId()));
			row.put("app_id", e.getAppId());
			row.put("app_name", e.getAppName());
			row.put("app_desc", e.getAppDesc());
			row.put("created_at", formatDateTimeString(e.getCreatedAt()));
			row.put("updated_at", formatDateTimeString(e.getUpdatedAt()));
			listMaps.add(row);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", listMaps);
		return data;
	}

	private static String formatDateTimeString(LocalDateTime t) {
		if (t == null) {
			return null;
		}
		return String.format(
				Locale.ROOT,
				"%04d-%02d-%02d %02d:%02d:%02d",
				t.getYear(),
				t.getMonthValue(),
				t.getDayOfMonth(),
				t.getHour(),
				t.getMinute(),
				t.getSecond());
	}

	private static String escapeLike(String needle) {
		if (needle == null) {
			return "";
		}
		return needle
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}
