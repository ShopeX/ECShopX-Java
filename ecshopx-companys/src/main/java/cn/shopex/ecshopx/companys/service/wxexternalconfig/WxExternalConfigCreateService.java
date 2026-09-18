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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.WxExternalConfig;
import cn.shopex.ecshopx.companys.mapper.WxExternalConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxExternalConfigCreateService {

	private final WxExternalConfigMapper wxExternalConfigMapper;

	public WxExternalConfigCreateService(WxExternalConfigMapper wxExternalConfigMapper) {
		this.wxExternalConfigMapper = wxExternalConfigMapper;
	}

	public Map<String, Object> create(Map<String, Object> body, long companyId) {
		if (body == null) {
			body = Map.of();
		}

		Object appIdRaw = body.get("app_id");
		if (appIdRaw == null || String.valueOf(appIdRaw).trim().isEmpty()) {
			throw new BadRequestException("请填写app_id");
		}
		String appId = String.valueOf(appIdRaw).trim();

		Object appNameRaw = body.get("app_name");
		if (appNameRaw == null || String.valueOf(appNameRaw).trim().isEmpty()) {
			throw new BadRequestException("请填小程序名称");
		}
		String appName = String.valueOf(appNameRaw).trim();

		String appDesc;
		if (!body.containsKey("app_desc") || body.get("app_desc") == null) {
			appDesc = "";
		} else {
			appDesc = stringValue(body.get("app_desc"));
		}

		LambdaQueryWrapper<WxExternalConfig> w =
				new LambdaQueryWrapper<WxExternalConfig>().eq(WxExternalConfig::getAppId, appId);
		if (wxExternalConfigMapper.selectCount(w) > 0) {
			throw new ResourceException("app_id已存在");
		}

		LocalDateTime now = LocalDateTime.now();
		WxExternalConfig entity = new WxExternalConfig();
		entity.setCompanyId(companyId);
		entity.setAppId(appId);
		entity.setAppName(appName);
		entity.setAppDesc(appDesc);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);

		wxExternalConfigMapper.insert(entity);

		Map<String, Object> data = new LinkedHashMap<>();
		Long rowId = entity.getWxExternalConfigId();
		data.put("wx_external_config_id", rowId != null ? String.valueOf(rowId) : null);
		data.put("company_id", String.valueOf(entity.getCompanyId()));
		data.put("app_id", entity.getAppId());
		data.put("app_name", entity.getAppName());
		data.put("app_desc", entity.getAppDesc());
		data.put("created_at", carbonStyleDateTime(entity.getCreatedAt()));
		data.put("updated_at", carbonStyleDateTime(entity.getUpdatedAt()));
		return data;
	}

	/**
	 * 使用 {@code date} / {@code timezone_type} / {@code timezone} 嵌套结构输出时间，避免将 {@link LocalDateTime}
	 * 直接放入 {@code Map<String, Object>} 时在 HTTP 响应序列化阶段失败。
	 */
	private static Map<String, Object> carbonStyleDateTime(LocalDateTime t) {
		if (t == null) {
			return null;
		}
		int micros = t.getNano() / 1000;
		String dateStr =
				String.format(
						Locale.ROOT,
						"%04d-%02d-%02d %02d:%02d:%02d.%06d",
						t.getYear(),
						t.getMonthValue(),
						t.getDayOfMonth(),
						t.getHour(),
						t.getMinute(),
						t.getSecond(),
						micros);
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("date", dateStr);
		m.put("timezone_type", 3);
		m.put("timezone", "PRC");
		return m;
	}

	private static String stringValue(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s;
		}
		return String.valueOf(o);
	}
}
