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
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import me.chanjar.weixin.open.api.WxOpenMaPrivacyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import me.chanjar.weixin.common.error.WxErrorException;

@Service
public class WxaSetPrivacySettingService {

	private final ObjectMapper objectMapper;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxJavaMaRuntime wxJavaMaRuntime;

	public WxaSetPrivacySettingService(
			ObjectMapper objectMapper,
			WechatAuthQueryService wechatAuthQueryService,
			WxJavaMaRuntime wxJavaMaRuntime) {
		this.objectMapper = objectMapper;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxJavaMaRuntime = wxJavaMaRuntime;
	}

	public void setPrivacySetting(
			long companyId, String wxaAppId, String ownerSettingRaw, String settingListRaw) {
		String appId = wxaAppId == null ? "" : wxaAppId.trim();
		if (companyId > 0 && StringUtils.hasText(appId)) {
			if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, appId)) {
				throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
			}
		}

		String ownerRaw = !StringUtils.hasText(ownerSettingRaw) ? "{}" : ownerSettingRaw;
		String settingRaw = !StringUtils.hasText(settingListRaw) ? "{}" : settingListRaw;

		JsonNode ownerSettingNode;
		try {
			ownerSettingNode = objectMapper.readTree(ownerRaw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("owner_setting 不是合法 JSON");
		}
		JsonNode settingListNode;
		try {
			settingListNode = objectMapper.readTree(settingRaw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("setting_list 不是合法 JSON");
		}

		if (ownerSettingNode.isObject()) {
			applyStoreExpireTimestamp((ObjectNode) ownerSettingNode);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("owner_setting", objectMapper.convertValue(ownerSettingNode, Object.class));
		body.put("setting_list", objectMapper.convertValue(settingListNode, Object.class));

		String respBody;
		try {
			respBody =
					wxJavaMaRuntime
							.ma(appId)
							.post(WxOpenMaPrivacyService.OPEN_SET_PRIVACY_SETTING, body);
		} catch (WxErrorException e) {
			throw new BadRequestException("微信接口请求失败");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(respBody == null ? "" : respBody);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("微信接口请求失败");
		}

		if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
			String msg = root.has("errmsg") ? root.get("errmsg").asText() : "微信接口错误";
			throw new ResourceException(msg);
		}
	}

	private void applyStoreExpireTimestamp(ObjectNode owner) {
		if (!owner.hasNonNull("store_expire_timestamp")) {
			return;
		}
		JsonNode node = owner.get("store_expire_timestamp");
		if (!storeExpireTruthy(node)) {
			return;
		}
		long epochSec = resolveStoreExpireEpochSeconds(node);
		owner.put("store_expire_timestamp", epochSec);
	}

	private static boolean storeExpireTruthy(JsonNode n) {
		if (n == null || n.isNull()) {
			return false;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isNumber()) {
			return n.asDouble() != 0.0;
		}
		if (n.isTextual()) {
			return StringUtils.hasText(n.asText());
		}
		return n.isArray() || n.isObject();
	}

	private long resolveStoreExpireEpochSeconds(JsonNode node) {
		if (node.isIntegralNumber()) {
			long v = node.asLong();
			if (v <= 0) {
				throw new BadRequestException("store_expire_timestamp 无效");
			}
			return v;
		}
		if (node.isNumber()) {
			throw new BadRequestException("store_expire_timestamp 格式无效");
		}
		if (node.isTextual()) {
			String text = node.asText().trim();
			try {
				LocalDate d = LocalDate.parse(text);
				return d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
			} catch (DateTimeParseException ignored) {
			}
			try {
				return Instant.parse(text).getEpochSecond();
			} catch (DateTimeParseException ignored) {
			}
			try {
				long v = Long.parseLong(text);
				if (v <= 0) {
					throw new BadRequestException("store_expire_timestamp 无效");
				}
				return v;
			} catch (NumberFormatException e) {
				throw new BadRequestException("store_expire_timestamp 格式无效");
			}
		}
		throw new BadRequestException("store_expire_timestamp 格式无效");
	}
}
