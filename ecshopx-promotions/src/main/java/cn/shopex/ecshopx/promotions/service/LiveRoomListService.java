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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.wechat.service.WxaAuthorizerAppIdByTemplateService;
import cn.shopex.ecshopx.wechat.wxa.WxaLiveBroadcastHttpClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LiveRoomListService {

	private final WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService;
	private final WxaLiveBroadcastHttpClient wxaLiveBroadcastHttpClient;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public LiveRoomListService(
			WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService,
			WxaLiveBroadcastHttpClient wxaLiveBroadcastHttpClient,
			MessageSource messageSource,
			ObjectMapper objectMapper) {
		this.wxaAuthorizerAppIdByTemplateService = wxaAuthorizerAppIdByTemplateService;
		this.wxaLiveBroadcastHttpClient = wxaLiveBroadcastHttpClient;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getLiveRooms(
			long companyId,
			String templateNameRaw,
			String wxappIdRaw,
			String pageRaw,
			String pageSizeRaw,
			String roomidRaw,
			String actionRaw,
			Locale locale) {
		String msg = messageSource.getMessage("promotions.liverooms.get_list_error", null, locale);

		boolean hasWx = StringUtils.hasText(wxappIdRaw == null ? "" : wxappIdRaw.trim());
		boolean hasTpl = StringUtils.hasText(templateNameRaw == null ? "" : templateNameRaw.trim());
		if (!hasWx && !hasTpl) {
			Map<String, List<String>> fe = new LinkedHashMap<>();
			fe.put("template_name", List.of("validation.required_without"));
			fe.put("wxapp_id", List.of("validation.required_without"));
			throw new ResourceException(msg, fe);
		}

		int page = parsePageRequired(pageRaw, msg);
		int pageSize = parsePageSizeRequired(pageSizeRaw, msg);

		String authorizerAppid =
				hasWx
						? wxappIdRaw.trim()
						: wxaAuthorizerAppIdByTemplateService.requireAuthorizerAppid(
								companyId, templateNameRaw);

		int start = (page - 1) * pageSize;
		String roomid = roomidRaw == null ? "" : roomidRaw.trim();
		String action = actionRaw == null ? "" : actionRaw.trim();

		JsonNode root =
				wxaLiveBroadcastHttpClient.fetchLiveInfoRoot(
						authorizerAppid, start, pageSize, action, roomid);

		List<Map<String, Object>> list;
		if ("get_replay".equals(action)) {
			list = jsonArrayToMapList(root.path("live_replay"));
		} else {
			list = jsonArrayToMapList(root.path("room_info"));
			applyCalTimeText(list);
		}

		long total = root.path("total").asLong(0L);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", total);
		return out;
	}

	public Map<String, Object> getLiveList(
			long companyId, String authorizerAppid, String pageRaw, String pageSizeRaw, Locale locale) {
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		String msgMissing =
				messageSource.getMessage("promotions.live_list.required_params_missing", null, locale);

		String authorizer = authorizerAppid == null ? "" : authorizerAppid.trim();
		if (!StringUtils.hasText(authorizer)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.live_list.wxapp_required", null, locale));
		}

		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page", List.of("validation.required"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		int page;
		try {
			page = Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException e) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page", List.of("validation.integer"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (page < 1) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page", List.of("validation.min.numeric"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (page < 1) {
			page = 1;
		}

		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.required"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.integer"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (pageSize < 1) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.min.numeric"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (pageSize > 20) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.max.numeric"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (pageSize < 1) {
			pageSize = 10;
		}

		int start = (page - 1) * pageSize;
		String action = "";
		String roomid = "";
		JsonNode root =
				wxaLiveBroadcastHttpClient.fetchLiveInfoRoot(authorizerAppid.trim(), start, pageSize, action, roomid);
		List<Map<String, Object>> list = jsonArrayToMapList(root.path("room_info"));
		applyCalTimeText(list);
		long total = root.path("total").asLong(0L);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", total);
		return out;
	}

	public Map<String, Object> getReplayList(
			long companyId,
			String authorizerAppid,
			String pageRaw,
			String pageSizeRaw,
			String roomIdRaw,
			Locale locale) {
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		String msgMissing =
				messageSource.getMessage("promotions.live_list.required_params_missing", null, locale);

		String authorizer = authorizerAppid == null ? "" : authorizerAppid.trim();
		if (!StringUtils.hasText(authorizer)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.live_list.wxapp_required", null, locale));
		}

		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page", List.of("validation.required"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		int page;
		try {
			page = Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException e) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page", List.of("validation.integer"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (page < 1) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page", List.of("validation.min.numeric"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (page < 1) {
			page = 1;
		}

		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.required"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.integer"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (pageSize < 1) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.min.numeric"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (pageSize > 20) {
			LinkedHashMap<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("page_size", List.of("validation.max.numeric"));
			throw new BadRequestException(msgMissing, fieldErrors);
		}
		if (pageSize < 1) {
			pageSize = 10;
		}

		String roomTrim = roomIdRaw == null ? "" : roomIdRaw.trim();
		if (roomTrim.isEmpty()) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.replay_list.live_room_id_required", null, locale));
		}
		long roomLong;
		try {
			roomLong = Long.parseLong(roomTrim);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.replay_list.live_room_id_required", null, locale));
		}
		if (roomLong == 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.replay_list.live_room_id_required", null, locale));
		}

		int start = (page - 1) * pageSize;
		JsonNode root =
				wxaLiveBroadcastHttpClient.fetchLiveInfoRoot(
						authorizerAppid.trim(), start, pageSize, "get_replay", roomTrim);
		List<Map<String, Object>> list = jsonArrayToMapList(root.path("live_replay"));
		long total = root.path("total").asLong(0L);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", total);
		return out;
	}

	private int parsePageRequired(String pageRaw, String msg) {
		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			throw new ResourceException(msg, Map.of("page", List.of("validation.required")));
		}
		int page;
		try {
			page = Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(msg, Map.of("page", List.of("validation.integer")));
		}
		if (page < 1) {
			throw new ResourceException(msg, Map.of("page", List.of("validation.min.numeric")));
		}
		return page;
	}

	private int parsePageSizeRequired(String pageSizeRaw, String msg) {
		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			throw new ResourceException(msg, Map.of("pageSize", List.of("validation.required")));
		}
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(msg, Map.of("pageSize", List.of("validation.integer")));
		}
		if (pageSize < 1) {
			throw new ResourceException(msg, Map.of("pageSize", List.of("validation.min.numeric")));
		}
		if (pageSize > 10) {
			throw new ResourceException(msg, Map.of("pageSize", List.of("validation.max.numeric")));
		}
		return pageSize;
	}

	private List<Map<String, Object>> jsonArrayToMapList(JsonNode arr) {
		if (arr == null || !arr.isArray()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (JsonNode n : arr) {
			if (!n.isObject()) {
				continue;
			}
			LinkedHashMap<String, Object> row =
					objectMapper.convertValue(n, new TypeReference<LinkedHashMap<String, Object>>() {});
			list.add(row);
		}
		return list;
	}

	private void applyCalTimeText(List<Map<String, Object>> list) {
		for (Map<String, Object> item : list) {
			int liveStatus = parseIntFlexible(item.get("live_status"));
			switch (liveStatus) {
				case 103 -> {
					long end = parseLongFlexible(item.get("end_time"));
					long startSec = parseLongFlexible(item.get("start_time"));
					long liveTime = end - startSec;
					Map<String, String> tp = getTimeParam(liveTime);
					String text;
					if (liveTime > 86400L) {
						text =
								tp.get("day")
										+ "天"
										+ tp.get("hour")
										+ "小时"
										+ tp.get("minute")
										+ "分"
										+ tp.get("second")
										+ "秒";
					} else if (liveTime < 86400L && liveTime > 3600L) {
						text = tp.get("hour") + "小时" + tp.get("minute") + "分" + tp.get("second") + "秒";
					} else if (liveTime < 3600L && liveTime > 60L) {
						text = tp.get("minute") + "分" + tp.get("second") + "秒";
					} else {
						text = tp.get("second") + "秒";
					}
					item.put("live_time_text", text);
				}
				default -> item.put("live_time_text", "");
			}
		}
	}

	private static int parseIntFlexible(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseLongFlexible(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, String> getTimeParam(long liveTime) {
		long day = liveTime / 86400L;
		long rem = liveTime % 86400L;
		long hour = rem / 3600L;
		long minute = (rem % 3600L) / 60L;
		long second = rem % 60L;
		Map<String, String> m = new LinkedHashMap<>();
		m.put("day", String.valueOf(day));
		m.put("hour", String.format("%02d", hour));
		m.put("minute", String.format("%02d", minute));
		m.put("second", String.format("%02d", second));
		return m;
	}
}
