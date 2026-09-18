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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.promotions.port.WxopenTemplateSendDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.WxaNoticeTemplate;
import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import cn.shopex.ecshopx.promotions.service.wxatemplate.WxaTemplateSceneDefinitions;
import cn.shopex.ecshopx.promotions.service.wxatemplate.WxaTemplateSceneDefinitions.WxaTemplateSceneDefinition;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaTemplateMsgActivityRemindSendService {

	private static final Logger log = LoggerFactory.getLogger(WxaTemplateMsgActivityRemindSendService.class);

	private static final String PAGE_REGISTRATION_ACTIVITY_DETAIL = "marketing/pages/member/activity-detail";

	private static final String PAGE_AFTERSALES_DETAIL = "order/pages/aftersales/detail";

	private final WxaNoticeTemplateMapper wxaNoticeTemplateMapper;
	private final WeappMapper weappMapper;
	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper;
	private final WxopenTemplateSendDispatchPublisher wxopenTemplateSendDispatchPublisher;

	public WxaTemplateMsgActivityRemindSendService(
			WxaNoticeTemplateMapper wxaNoticeTemplateMapper,
			WeappMapper weappMapper,
			WxJavaMaRuntime wxJavaMaRuntime,
			ObjectMapper objectMapper,
			WxopenTemplateSendDispatchPublisher wxopenTemplateSendDispatchPublisher) {
		this.wxaNoticeTemplateMapper = wxaNoticeTemplateMapper;
		this.weappMapper = weappMapper;
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.objectMapper = objectMapper;
		this.wxopenTemplateSendDispatchPublisher = wxopenTemplateSendDispatchPublisher;
	}

	/**
	 * Sends a subscribe message when template metadata allows immediate delivery.
	 *
	 * @return {@code true} when finished without throwing (including validation skips and downstream failures),
	 *         mirroring tolerant behaviour of the legacy notifier.
	 */
	public boolean send(Map<String, Object> wxopenTemplatePayload, boolean forceFire) {
		try {
			if (!validateRequired(wxopenTemplatePayload)) {
				return true;
			}
			long companyId = extractLong(wxopenTemplatePayload.get("company_id"));
			String scenesName = String.valueOf(wxopenTemplatePayload.get("scenes_name")).trim();
			String authorizerAppid = String.valueOf(wxopenTemplatePayload.get("appid")).trim();
			String openid = String.valueOf(wxopenTemplatePayload.get("openid")).trim();

			Weapp weapp = weappMapper.selectOne(new LambdaQueryWrapper<Weapp>()
					.eq(Weapp::getCompanyId, companyId)
					.eq(Weapp::getAuthorizerAppid, authorizerAppid)
					.last("LIMIT 1"));
			if (weapp == null || !StringUtils.hasText(weapp.getTemplateName())) {
				return true;
			}
			String templateName = weapp.getTemplateName().trim();

			WxaNoticeTemplate notice =
					wxaNoticeTemplateMapper.selectOne(new LambdaQueryWrapper<WxaNoticeTemplate>()
							.eq(WxaNoticeTemplate::getCompanyId, companyId)
							.eq(WxaNoticeTemplate::getScenesName, scenesName)
							.eq(WxaNoticeTemplate::getTemplateName, templateName)
							.last("LIMIT 1"));
			if (notice == null
					|| !Boolean.TRUE.equals(notice.getIsOpen())
					|| !StringUtils.hasText(notice.getTemplateId())) {
				return true;
			}

			int delayMinutes = delayMinutesFromSendTimeDesc(notice.getSendTimeDesc());
			if (!forceFire && delayMinutes > 0) {
				Map<String, Object> biz = new LinkedHashMap<>(wxopenTemplatePayload);
				wxopenTemplateSendDispatchPublisher.publish(biz, true, Duration.ofMinutes(delayMinutes));
				return true;
			}

			if (!checkSendAllowed(wxopenTemplatePayload, scenesName)) {
				return true;
			}

			List<Map<String, String>> contentRows = resolveContentRows(notice, scenesName);
			if (contentRows.isEmpty()) {
				return true;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> dataRoot =
					(Map<String, Object>) wxopenTemplatePayload.get("data");
			Map<String, Map<String, String>> subscribeData = buildSubscribeData(contentRows, dataRoot);

			String pageBase = resolvePagePath(scenesName);
			String page = pageBase;
			Object pageQueryStr = wxopenTemplatePayload.get("page_query_str");
			if (pageQueryStr != null && StringUtils.hasText(String.valueOf(pageQueryStr))) {
				page = pageBase + "?" + String.valueOf(pageQueryStr).trim();
			}

			postSubscribeSend(
					authorizerAppid,
					openid,
					notice.getTemplateId().trim(),
					page,
					subscribeData);
			return true;
		} catch (RuntimeException e) {
			log.debug("wxa activity remind subscribe send skipped: {}", e.toString());
			return true;
		}
	}

	private static boolean validateRequired(Map<String, Object> payload) {
		if (!StringUtils.hasText(stringVal(payload.get("company_id")))) {
			return false;
		}
		if (!StringUtils.hasText(stringVal(payload.get("scenes_name")))) {
			return false;
		}
		if (!StringUtils.hasText(stringVal(payload.get("appid")))) {
			return false;
		}
		if (!StringUtils.hasText(stringVal(payload.get("openid")))) {
			return false;
		}
		Object data = payload.get("data");
		if (!(data instanceof Map<?, ?> m) || m.isEmpty()) {
			return false;
		}
		return true;
	}

	private static boolean checkSendAllowed(Map<String, Object> payload, String scenesName) {
		if (!"payOrdersRemind".equals(scenesName)) {
			return true;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) payload.get("data");
		return data != null && StringUtils.hasText(stringVal(data.get("order_id")));
	}

	private List<Map<String, String>> resolveContentRows(WxaNoticeTemplate notice, String scenesName) {
		List<Map<String, String>> parsed = parseContentRows(notice.getContent());
		if (!parsed.isEmpty() && rowLooksModern(parsed)) {
			return parsed;
		}
		Optional<WxaTemplateSceneDefinition> def = WxaTemplateSceneDefinitions.findByScenesName(scenesName);
		if (def.isEmpty()) {
			return List.of();
		}
		List<Map<String, String>> fallback = new ArrayList<>();
		for (Map<String, String> row : def.get().getValueAsMaps()) {
			fallback.add(Map.copyOf(row));
		}
		return fallback;
	}

	private static boolean rowLooksModern(List<Map<String, String>> rows) {
		for (Map<String, String> row : rows) {
			if (row.containsKey("keyword")) {
				return true;
			}
		}
		return false;
	}

	private List<Map<String, String>> parseContentRows(String raw) {
		try {
			if (raw == null || raw.isBlank()) {
				return List.of();
			}
			JsonNode node = objectMapper.readTree(raw);
			if (!node.isArray()) {
				return List.of();
			}
			return objectMapper.convertValue(node, new TypeReference<List<Map<String, String>>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private Map<String, Map<String, String>> buildSubscribeData(
			List<Map<String, String>> contentRows, Map<String, Object> dataValues) {
		Map<String, Map<String, String>> out = new LinkedHashMap<>();
		for (Map<String, String> row : contentRows) {
			String keyword = row.get("keyword");
			if (!StringUtils.hasText(keyword)) {
				continue;
			}
			String column = row.get("column");
			String resolved = resolveCellValue(row, column, dataValues);
			String formatted = formatKeywordValue(keyword, resolved);
			out.put(keyword.trim(), Map.of("value", formatted));
		}
		return out;
	}

	private static String resolveCellValue(
			Map<String, String> row, String column, Map<String, Object> dataValues) {
		String preset = row.get("value");
		if (StringUtils.hasText(preset)) {
			if (StringUtils.hasText(column) && dataValues != null && dataValues.containsKey(column)) {
				String cell = stringVal(dataValues.get(column));
				return preset.replace("{" + column + "}", cell);
			}
			return preset;
		}
		if (dataValues != null && StringUtils.hasText(column)) {
			return stringVal(dataValues.get(column));
		}
		return "";
	}

	private static String formatKeywordValue(String keyword, String value) {
		String v = value == null ? "" : value;
		if (keyword != null && keyword.startsWith("thing")) {
			return truncateThing(v);
		}
		return v;
	}

	private static String truncateThing(String value) {
		if (codePointLength(value) <= 20) {
			return value;
		}
		return substringByCodePoints(value, 17) + "...";
	}

	private static int codePointLength(String s) {
		int n = 0;
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			n++;
			i += Character.charCount(cp);
		}
		return n;
	}

	private static String substringByCodePoints(String s, int maxCp) {
		StringBuilder sb = new StringBuilder();
		int seen = 0;
		for (int i = 0; i < s.length(); ) {
			if (seen >= maxCp) {
				break;
			}
			int cp = s.codePointAt(i);
			sb.appendCodePoint(cp);
			seen++;
			i += Character.charCount(cp);
		}
		return sb.toString();
	}

	private static String resolvePagePath(String scenesName) {
		return switch (scenesName) {
			case "registrationActivityNotice" -> PAGE_REGISTRATION_ACTIVITY_DETAIL;
			case "aftersalesSuccess" -> PAGE_AFTERSALES_DETAIL;
			default -> "pages/index";
		};
	}

	private int delayMinutesFromSendTimeDesc(String raw) {
		try {
			if (raw == null || raw.isBlank()) {
				return 0;
			}
			JsonNode node = objectMapper.readTree(raw);
			JsonNode valueNode = node.get("value");
			if (valueNode == null || valueNode.isNull()) {
				return 0;
			}
			if (valueNode.isNumber()) {
				return Math.max(0, valueNode.intValue());
			}
			if (valueNode.isTextual()) {
				return Integer.parseInt(valueNode.asText().trim());
			}
		} catch (Exception ignored) {
			return 0;
		}
		return 0;
	}

	private void postSubscribeSend(
			String authorizerAppid,
			String touser,
			String priTemplateId,
			String page,
			Map<String, Map<String, String>> data) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("touser", touser);
		body.put("template_id", priTemplateId);
		body.put("page", page);
		body.put("data", data);
		String json;
		try {
			json = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			ResourceException rex = new ResourceException("subscribe send body serialize failed");
			rex.initCause(e);
			throw rex;
		}
		try {
			String raw =
					wxJavaMaRuntime
							.ma(authorizerAppid)
							.post(cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants.Subscribe.SUBSCRIBE_MSG_SEND_URL, json);
			if (raw == null || raw.isEmpty()) {
				return;
			}
			JsonNode root = objectMapper.readTree(raw);
			int code = root.path("errcode").asInt(0);
			if (code != 0) {
				log.debug(
						"wxa subscribe send returned errcode={} errmsg={}",
						code,
						root.path("errmsg").asText(""));
			}
		} catch (Exception e) {
			log.debug("wxa subscribe send failed: {}", e.toString());
		}
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
