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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOfficialAccountMenuCreateClient;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatMenuService {

	private enum MenuTreeRemoteSyncOutcome {
		REDIS_WRITE_FAILED,
		WECHAT_CREATE_FAILED,
		SUCCESS
	}

	private static final String REDIS_KEY_PREFIX = "menuTree:";
	private static final String SHA1_INPUT_SUFFIX = "menu_tree";

	private static final Map<String, String> WXSYS_LABELS = Map.ofEntries(
			Map.entry("scancode_waitmsg", "扫码带提示"),
			Map.entry("scancode_push", "扫码推事件"),
			Map.entry("pic_sysphoto", "系统拍照发图"),
			Map.entry("pic_photo_or_album", "拍照或者相册发图"),
			Map.entry("pic_weixin", "微信相册发图"),
			Map.entry("location_select", "发送位置"));

	private final StringRedisTemplate wechatStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final WechatOfficialAccountMenuCreateClient menuCreateClient;

	public WechatMenuService(
			@Qualifier("wechatStringRedisTemplate") StringRedisTemplate wechatStringRedisTemplate,
			ObjectMapper objectMapper,
			WechatOpenPlatformAuthorizerTokenService tokenService,
			WechatOfficialAccountMenuCreateClient menuCreateClient) {
		this.wechatStringRedisTemplate = wechatStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.tokenService = tokenService;
		this.menuCreateClient = menuCreateClient;
	}

	public List<Map<String, Object>> addMenu(
			String authorizerAppId, String companyIdRaw, List<Map<String, Object>> postdata) {
		List<Map<String, Object>> validated = validateWechatMenuPostdata(postdata);
		String appId = authorizerAppId == null ? "" : authorizerAppId.trim();
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		addMenuTree(appId, companyIdRaw, validated);
		return validated;
	}

	public Map<String, Object> removeMenu(
			String authorizerAppId, String companyIdRaw, List<Map<String, Object>> postdata) {
		List<Map<String, Object>> validated = validateWechatMenuPostdata(postdata == null ? List.of() : postdata);
		String appId = authorizerAppId == null ? "" : authorizerAppId.trim();
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		String companyKey = companyIdRaw == null ? "" : String.valueOf(companyIdRaw).trim();
		MenuTreeRemoteSyncOutcome outcome = syncMenuTreeRemote(appId, companyKey, validated);
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		switch (outcome) {
			case REDIS_WRITE_FAILED:
				map.put("status", List.of());
				break;
			case WECHAT_CREATE_FAILED:
				map.put("status", Boolean.FALSE);
				break;
			case SUCCESS:
				map.put("status", Boolean.TRUE);
				break;
		}
		return map;
	}

	public List<Map<String, Object>> getMenuTree(String authorizerAppId, String companyIdRaw) {
		String key = menuTreeRedisKey(authorizerAppId, companyIdRaw);
		String raw = wechatStringRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<Map<String, Object>> parsed;
		try {
			parsed = objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
		} catch (JsonProcessingException e) {
			return List.of();
		}
		if (parsed == null || parsed.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> item : parsed) {
			if (item == null) {
				continue;
			}
			result.add(normalizeTopMenuMap(item));
		}
		return result;
	}

	private List<Map<String, Object>> validateWechatMenuPostdata(List<Map<String, Object>> postdata) {
		for (Map<String, Object> value : postdata) {
			if (value == null) {
				throw new BadRequestException("主菜单名称必填", 422);
			}
			Object nameObj = value.get("name");
			if (nameObj == null || String.valueOf(nameObj).trim().isEmpty()) {
				throw new BadRequestException("主菜单名称必填", 422);
			}
			String name = String.valueOf(nameObj).trim();
			if (name.getBytes(StandardCharsets.UTF_8).length > 12) {
				throw new BadRequestException("主菜单名称不超过4个汉字或12个字母", 422);
			}
			if (!value.containsKey("second_menu")) {
				throw new BadRequestException("子菜单列表格式错误", 422);
			}
			Object second = value.get("second_menu");
			if (second == null || !(second instanceof List)) {
				throw new BadRequestException("子菜单列表格式错误", 422);
			}
			@SuppressWarnings("unchecked")
			List<Object> secondList = (List<Object>) second;
			for (Object rawVal : secondList) {
				if (!(rawVal instanceof Map)) {
					throw new BadRequestException("子菜单列表格式错误", 422);
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> val = (Map<String, Object>) rawVal;
				Object sn = val.get("name");
				if (sn == null || String.valueOf(sn).trim().isEmpty()) {
					throw new BadRequestException("子菜单名称必填", 422);
				}
				if (name.getBytes(StandardCharsets.UTF_8).length > 24) {
					throw new BadRequestException("子菜单名称不超过8个汉字或16个字母", 422);
				}
				Object content = val.get("content");
				if (isSubmenuContentEmpty(content)) {
					throw new BadRequestException("子菜单内容必填", 422);
				}
			}
		}
		return postdata;
	}

	private static boolean isSubmenuContentEmpty(Object content) {
		if (content == null) {
			return true;
		}
		if (content instanceof String) {
			return ((String) content).trim().isEmpty();
		}
		if (content instanceof Map) {
			return ((Map<?, ?>) content).isEmpty();
		}
		return String.valueOf(content).trim().isEmpty();
	}

	private MenuTreeRemoteSyncOutcome syncMenuTreeRemote(
			String authorizerAppId, String companyIdRaw, List<Map<String, Object>> menuTree) {
		String redisKey = menuTreeRedisKey(authorizerAppId, companyIdRaw);
		String json;
		try {
			json = objectMapper.writeValueAsString(menuTree);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("menu tree serialization failed", e);
		}
		/*
		 * Treat Redis SET as successful when the driver reports true/OK for the write.
		 * If the return is inconclusive (e.g. null), confirm by reading back identical bytes.
		 */
		boolean setOk = Boolean.TRUE.equals(wechatStringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
			byte[] kb = redisKey.getBytes(StandardCharsets.UTF_8);
			byte[] vb = json.getBytes(StandardCharsets.UTF_8);
			Boolean written = connection.set(kb, vb);
			if (Boolean.TRUE.equals(written)) {
				return true;
			}
			if (Boolean.FALSE.equals(written)) {
				return false;
			}
			byte[] readBack = connection.get(kb);
			return readBack != null && java.util.Arrays.equals(readBack, vb);
		}));
		if (!setOk) {
			return MenuTreeRemoteSyncOutcome.REDIS_WRITE_FAILED;
		}
		boolean pushed = pushWechatMenu(authorizerAppId, menuTree);
		return pushed ? MenuTreeRemoteSyncOutcome.SUCCESS : MenuTreeRemoteSyncOutcome.WECHAT_CREATE_FAILED;
	}

	private void addMenuTree(String authorizerAppId, String companyIdRaw, List<Map<String, Object>> menuTree) {
		syncMenuTreeRemote(authorizerAppId, companyIdRaw, menuTree);
	}

	private boolean pushWechatMenu(String authorizerAppId, List<Map<String, Object>> menuTree) {
		List<Map<String, Object>> button = new ArrayList<>();
		for (Map<String, Object> fmenu : menuTree) {
			if (!hasSecondMenu(fmenu)) {
				button.add(buildMenuData(fmenu));
			} else {
				@SuppressWarnings("unchecked")
				List<Object> rawSecond = (List<Object>) fmenu.get("second_menu");
				LinkedHashMap<String, Object> parent = new LinkedHashMap<>();
				parent.put("name", String.valueOf(fmenu.get("name")));
				List<Map<String, Object>> sub = new ArrayList<>();
				for (Object smenuObj : rawSecond) {
					if (smenuObj instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> smenu = (Map<String, Object>) smenuObj;
						sub.add(buildMenuData(smenu));
					}
				}
				parent.put("sub_button", sub);
				button.add(parent);
			}
		}
		String accessToken = tokenService.getAuthorizerAccessToken(authorizerAppId.trim());
		return menuCreateClient.createMenu(accessToken, button);
	}

	private static boolean hasSecondMenu(Map<String, Object> fmenu) {
		Object sm = fmenu.get("second_menu");
		if (sm == null) {
			return false;
		}
		if (!(sm instanceof List)) {
			return false;
		}
		return !((List<?>) sm).isEmpty();
	}

	private Map<String, Object> buildMenuData(Map<String, Object> menu) {
		int menuType;
		try {
			menuType = normalizeMenuType(menu.get("menu_type"));
		} catch (NumberFormatException e) {
			throw new BadRequestException("菜单类型参数错误", 422);
		}
		if (menuType == 0) {
			return new LinkedHashMap<>();
		}
		switch (menuType) {
			case 1:
				return buildMenuTypeOne(menu);
			case 2:
				return buildViewMenu(menu);
			case 3:
				return buildMiniprogramMenu(menu);
			case 4:
				return buildWxsysMenu(menu);
			default:
				return new LinkedHashMap<>();
		}
	}

	private static int normalizeMenuType(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number) {
			return ((Number) raw).intValue();
		}
		if (raw instanceof String) {
			String t = ((String) raw).trim();
			if (t.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(t);
		}
		if (raw instanceof Boolean) {
			return Boolean.TRUE.equals(raw) ? 1 : 0;
		}
		return 0;
	}

	private Map<String, Object> buildMenuTypeOne(Map<String, Object> menu) {
		Object nt = menu.get("news_type");
		String newsType = nt == null ? "" : String.valueOf(nt).trim();
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("name", String.valueOf(menu.get("name")));
		if ("text".equals(newsType)) {
			out.put("type", "click");
			out.put("key", "text:" + String.valueOf(menu.get("content")));
			return out;
		}
		if ("news".equals(newsType)) {
			out.put("type", "click");
			String mediaId = "";
			Object content = menu.get("content");
			if (content instanceof Map) {
				Map<?, ?> cm = (Map<?, ?>) content;
				Object mid = cm.get("media_id");
				if (mid != null) {
					mediaId = String.valueOf(mid);
				}
			}
			out.put("key", "news:" + mediaId);
			return out;
		}
		if ("image".equals(newsType)) {
			out.put("type", "media_id");
			Object content = menu.get("content");
			if (content instanceof Map) {
				Map<?, ?> cm = (Map<?, ?>) content;
				Object mid = cm.get("media_id");
				out.put("media_id", mid == null ? "" : String.valueOf(mid));
			} else {
				out.put("media_id", "");
			}
			return out;
		}
		return new LinkedHashMap<>();
	}

	private static Map<String, Object> buildViewMenu(Map<String, Object> menu) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("type", "view");
		out.put("name", String.valueOf(menu.get("name")));
		out.put("url", menu.get("url") == null ? "" : String.valueOf(menu.get("url")));
		return out;
	}

	private static Map<String, Object> buildMiniprogramMenu(Map<String, Object> menu) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("type", "miniprogram");
		out.put("name", String.valueOf(menu.get("name")));
		out.put("url", menu.get("url") == null ? "" : String.valueOf(menu.get("url")));
		out.put("appid", firstNonBlank(menu, "appid", "app_id"));
		out.put("pagepath", menu.get("pagepath") == null ? "" : String.valueOf(menu.get("pagepath")));
		return out;
	}

	private static String firstNonBlank(Map<String, Object> menu, String k1, String k2) {
		for (String k : List.of(k1, k2)) {
			if (!menu.containsKey(k)) {
				continue;
			}
			Object v = menu.get(k);
			if (v == null) {
				continue;
			}
			String s = String.valueOf(v).trim();
			if (!s.isEmpty()) {
				return s;
			}
		}
		return "";
	}

	private Map<String, Object> buildWxsysMenu(Map<String, Object> menu) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("type", String.valueOf(menu.get("wxsys")));
		out.put("key", wxsysChineseLabel(menu.get("wxsys")));
		out.put("name", String.valueOf(menu.get("name")));
		return out;
	}

	private static String wxsysChineseLabel(Object wxsysRaw) {
		String k = wxsysRaw == null ? "" : String.valueOf(wxsysRaw).trim();
		return WXSYS_LABELS.getOrDefault(k, k);
	}

	private String menuTreeRedisKey(String authorizerAppId, String companyIdRaw) {
		String salt = (authorizerAppId == null ? "" : authorizerAppId) + (companyIdRaw == null ? "" : companyIdRaw);
		return REDIS_KEY_PREFIX + sha1HexUtf8(salt + SHA1_INPUT_SUFFIX);
	}

	private static Map<String, Object> normalizeTopMenuMap(Map<String, Object> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(src);
		out.put("menu_type", menuTypeIntValForTree(src.get("menu_type")));
		Object sm = src.get("second_menu");
		if (sm instanceof List && !((List<?>) sm).isEmpty()) {
			List<Map<String, Object>> subOut = new ArrayList<>();
			for (Object el : (List<?>) sm) {
				if (el instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> subSrc = (Map<String, Object>) el;
					subOut.add(normalizeSubMenuMap(subSrc));
				}
			}
			out.put("second_menu", subOut);
		} else {
			out.put("second_menu", new ArrayList<>());
		}
		return out;
	}

	private static Map<String, Object> normalizeSubMenuMap(Map<String, Object> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(src);
		out.put("menu_type", menuTypeIntValForTree(src.get("menu_type")));
		return out;
	}

	private static int menuTypeIntValForTree(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number) {
			return ((Number) raw).intValue();
		}
		if (raw instanceof String) {
			String t = ((String) raw).trim();
			if (t.isEmpty()) {
				return 0;
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		if (raw instanceof Boolean) {
			return Boolean.TRUE.equals(raw) ? 1 : 0;
		}
		return 0;
	}

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
