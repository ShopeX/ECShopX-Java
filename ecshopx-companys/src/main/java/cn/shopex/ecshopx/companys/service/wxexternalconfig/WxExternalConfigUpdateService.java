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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxExternalConfigUpdateService {

	private final WxExternalConfigMapper wxExternalConfigMapper;

	public WxExternalConfigUpdateService(WxExternalConfigMapper wxExternalConfigMapper) {
		this.wxExternalConfigMapper = wxExternalConfigMapper;
	}

	public Map<String, Object> updateWxExternalConfig(long companyId, String wxExternalConfigIdRaw, Map<String, Object> body) {
		if (body == null) {
			body = Map.of();
		}

		String raw = wxExternalConfigIdRaw == null ? "" : wxExternalConfigIdRaw.trim();
		if (raw.isEmpty()) {
			throw new BadRequestException("请填写配置ID");
		}

		Object appNameRaw = body.get("app_name");
		if (appNameRaw == null || String.valueOf(appNameRaw).trim().isEmpty()) {
			throw new BadRequestException("请填写小程序名称");
		}
		String appName = String.valueOf(appNameRaw).trim();

		boolean appIdTruthy =
				body.containsKey("app_id")
						&& body.get("app_id") != null
						&& !String.valueOf(body.get("app_id")).trim().isEmpty();
		String newAppId = null;
		if (appIdTruthy) {
			newAppId = String.valueOf(body.get("app_id")).trim();
		}

		boolean updateAppDesc = body.containsKey("app_desc") && body.get("app_desc") != null;
		String newAppDesc = null;
		if (updateAppDesc) {
			newAppDesc = stringValue(body.get("app_desc"));
		}

		LambdaQueryWrapper<WxExternalConfig> rowWrapper =
				new LambdaQueryWrapper<WxExternalConfig>()
						.eq(WxExternalConfig::getCompanyId, companyId)
						.apply("wx_external_config_id = {0}", raw);
		WxExternalConfig current = wxExternalConfigMapper.selectOne(rowWrapper);
		if (current == null) {
			throw new ResourceException("未查询到更新数据");
		}

		if (appIdTruthy) {
			LambdaQueryWrapper<WxExternalConfig> byAppId =
					new LambdaQueryWrapper<WxExternalConfig>()
							.eq(WxExternalConfig::getAppId, newAppId)
							.apply("wx_external_config_id <> {0}", raw);
			WxExternalConfig other = wxExternalConfigMapper.selectOne(byAppId);
			if (other != null) {
				throw new ResourceException("app_id已存在");
			}
		}

		LocalDateTime now = LocalDateTime.now();
		LambdaUpdateWrapper<WxExternalConfig> uw =
				new LambdaUpdateWrapper<WxExternalConfig>()
						.eq(WxExternalConfig::getCompanyId, companyId)
						.apply("wx_external_config_id = {0}", raw)
						.set(WxExternalConfig::getAppName, appName)
						.set(WxExternalConfig::getUpdatedAt, now);
		if (appIdTruthy) {
			uw.set(WxExternalConfig::getAppId, newAppId);
		}
		if (updateAppDesc) {
			uw.set(WxExternalConfig::getAppDesc, newAppDesc);
		}
		int updated = wxExternalConfigMapper.update(null, uw);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		WxExternalConfig refreshed = wxExternalConfigMapper.selectOne(rowWrapper);
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> data = new LinkedHashMap<>();
		Long rowId = refreshed.getWxExternalConfigId();
		data.put("wx_external_config_id", rowId != null ? String.valueOf(rowId) : null);
		data.put("company_id", String.valueOf(refreshed.getCompanyId()));
		data.put("app_id", refreshed.getAppId());
		data.put("app_name", refreshed.getAppName());
		data.put("app_desc", refreshed.getAppDesc());
		data.put("created_at", carbonStyleDateTime(refreshed.getCreatedAt()));
		data.put("updated_at", carbonStyleDateTime(refreshed.getUpdatedAt()));
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
