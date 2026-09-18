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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import me.chanjar.weixin.open.api.WxOpenMaPrivacyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import me.chanjar.weixin.common.error.WxErrorException;

@Service
public class WxaGetPrivacySettingService {
	private static final TypeReference<LinkedHashMap<String, Object>> LINKED_MAP_TYPE =
			new TypeReference<LinkedHashMap<String, Object>>() {};

	private final ObjectMapper objectMapper;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxJavaMaRuntime wxJavaMaRuntime;

	public WxaGetPrivacySettingService(
			ObjectMapper objectMapper,
			WechatAuthQueryService wechatAuthQueryService,
			WxJavaMaRuntime wxJavaMaRuntime) {
		this.objectMapper = objectMapper;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxJavaMaRuntime = wxJavaMaRuntime;
	}

	public Map<String, Object> getPrivacySetting(long companyId, String wxaAppId) {
		String appId = wxaAppId == null ? "" : wxaAppId.trim();
		if (companyId > 0 && StringUtils.hasText(appId)) {
			if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, appId)) {
				throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
			}
		}

		String respBody;
		try {
			respBody =
					wxJavaMaRuntime
							.ma(appId)
							.post(WxOpenMaPrivacyService.OPEN_GET_PRIVACY_SETTING, Map.of());
		} catch (WxErrorException e) {
			throw new BadRequestException("微信接口请求失败");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(respBody == null ? "" : respBody);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("微信接口请求失败");
		}

		LinkedHashMap<String, Object> out = objectMapper.convertValue(root, LINKED_MAP_TYPE);
		postProcessOwnerSettingStoreExpire(out);
		return out;
	}

	private void postProcessOwnerSettingStoreExpire(LinkedHashMap<String, Object> out) {
		Object ownerRaw = out.get("owner_setting");
		if (!(ownerRaw instanceof Map<?, ?>)) {
			return;
		}
		LinkedHashMap<String, Object> owner = objectMapper.convertValue(ownerRaw, LINKED_MAP_TYPE);
		Object v = owner.get("store_expire_timestamp");
		long sec = coerceToIntegralSeconds(v);
		if (sec > 0) {
			String dateStr = LocalDate.ofInstant(Instant.ofEpochSecond(sec), ZoneId.systemDefault())
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			owner.put("store_expire_timestamp", dateStr);
			out.put("owner_setting", owner);
		}
	}

	private static long coerceToIntegralSeconds(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof Boolean b) {
			return b ? 1L : 0L;
		}
		if (v instanceof String s) {
			return LeadingNumberParser.parseAsLong(s.trim());
		}
		return 0L;
	}
}
