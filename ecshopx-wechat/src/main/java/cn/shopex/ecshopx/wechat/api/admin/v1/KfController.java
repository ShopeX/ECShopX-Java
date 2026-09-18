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

package cn.shopex.ecshopx.wechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountKfCreateService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountKfDeleteService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountKfListService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountKfUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("wechatAdminV1Kf")
@RequestMapping("/api/v1/wechat")
public class KfController {

	private final WechatOfficialAccountKfCreateService wechatOfficialAccountKfCreateService;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WechatOfficialAccountKfUpdateService wechatOfficialAccountKfUpdateService;
	private final WechatOfficialAccountKfListService wechatOfficialAccountKfListService;
	private final WechatOfficialAccountKfDeleteService wechatOfficialAccountKfDeleteService;

	public KfController(
			WechatOfficialAccountKfCreateService wechatOfficialAccountKfCreateService,
			WechatAuthQueryService wechatAuthQueryService,
			WechatOfficialAccountKfUpdateService wechatOfficialAccountKfUpdateService,
			WechatOfficialAccountKfListService wechatOfficialAccountKfListService,
			WechatOfficialAccountKfDeleteService wechatOfficialAccountKfDeleteService) {
		this.wechatOfficialAccountKfCreateService = wechatOfficialAccountKfCreateService;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wechatOfficialAccountKfUpdateService = wechatOfficialAccountKfUpdateService;
		this.wechatOfficialAccountKfListService = wechatOfficialAccountKfListService;
		this.wechatOfficialAccountKfDeleteService = wechatOfficialAccountKfDeleteService;
	}

	@Activated(routeAlias = "wechat.create.kfs")
	@PostMapping(value = "/kfs", name = "添加客服", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createWechatKf(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body,
			@RequestParam(value = "avatar", required = false) MultipartFile avatar) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String wxName = merged.get("wx_name") == null ? null : String.valueOf(merged.get("wx_name")).trim();
		String nick = merged.get("nick") == null ? null : String.valueOf(merged.get("nick")).trim();
		String authorizerAppid = resolveAuthorizerAppid(request);

		Path avatarLocalPathOrNull = null;
		if (avatar != null && !avatar.isEmpty()) {
			if (avatar.getSize() <= 0) {
				throw new ResourceException("上传头像失败");
			}
			String original = avatar.getOriginalFilename();
			String suffix = null;
			if (original != null) {
				int dot = original.lastIndexOf('.');
				if (dot >= 0 && dot < original.length() - 1) {
					suffix = original.substring(dot);
				}
			}
			try {
				avatarLocalPathOrNull = Files.createTempFile("wechat-kf-avatar-", suffix);
				try (InputStream in = avatar.getInputStream()) {
					Files.copy(in, avatarLocalPathOrNull, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception e) {
				if (avatarLocalPathOrNull != null) {
					try {
						Files.deleteIfExists(avatarLocalPathOrNull);
					} catch (Exception ignored) {
						// ignore cleanup failure
					}
				}
				throw new ResourceException("上传头像失败");
			}
		}

		try {
			wechatOfficialAccountKfCreateService.createWechatKf(authorizerAppid, wxName, nick, avatarLocalPathOrNull);
			return ApiResult.ok(Map.of("status", Boolean.TRUE));
		} finally {
			if (avatarLocalPathOrNull != null) {
				try {
					Files.deleteIfExists(avatarLocalPathOrNull);
				} catch (Exception ignored) {
					// ignore cleanup failure
				}
			}
		}
	}

	private String resolveAuthorizerAppid(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object appidObj = jwt.get("authorizer_appid");
		if (appidObj != null) {
			String fromJwt = String.valueOf(appidObj).trim();
			if (StringUtils.hasText(fromJwt)) {
				return fromJwt;
			}
		}
		Long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		if (companyId == null) {
			return "";
		}
		String fromDb = wechatAuthQueryService.getAuthorizerAppid(companyId);
		return fromDb != null && StringUtils.hasText(fromDb) ? fromDb.trim() : "";
	}

	private static Long readCompanyIdFromOperatorJwtMap(Map<?, ?> jwt) {
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			return null;
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Activated(routeAlias = "wechat.lists.kfs")
	@GetMapping(value = "/kfs", name = "客服列表")
	public ApiResult<Map<String, Object>> lists(HttpServletRequest request) {
		String authorizerAppid = resolveAuthorizerAppid(request);
		List<Map<String, Object>> list = wechatOfficialAccountKfListService.lists(authorizerAppid);
		return ApiResult.ok(Map.of("list", list));
	}

	@Activated(routeAlias = "wechat.delete.kfs")
	@DeleteMapping(
			value = "/kfs",
			name = "删除客服",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteWechatKf(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String authorizerAppid = resolveAuthorizerAppid(request);
		wechatAuthQueryService.requireWechatAuthWithAliasForKf(authorizerAppid);

		Object accountObj = merged.get("account");
		if (isMissingRequiredKfAccount(accountObj)) {
			throw new BadRequestException("更改的账号必填");
		}
		String account = accountObj instanceof String s ? s.trim() : String.valueOf(accountObj).trim();
		wechatOfficialAccountKfDeleteService.deleteWechatKf(authorizerAppid, account);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	private static boolean isMissingRequiredKfAccount(Object accountObj) {
		if (accountObj == null) {
			return true;
		}
		if (accountObj instanceof Boolean b) {
			return !b;
		}
		if (accountObj instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (accountObj instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return !StringUtils.hasText(t) || "0".equals(t);
		}
		if (accountObj instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (accountObj instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		String s = String.valueOf(accountObj).trim();
		return !StringUtils.hasText(s) || "0".equals(s);
	}

	@Activated(routeAlias = "wechat.update.kfs")
	@PostMapping(value = "/update/kfs", name = "修改客服", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateWechatKf(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body,
			@RequestParam(value = "avatar", required = false) MultipartFile avatar) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String authorizerAppid = resolveAuthorizerAppid(request);
		wechatAuthQueryService.requireWechatAuthWithAliasForKf(authorizerAppid);

		Object accountObj = merged.get("account");
		String account = accountObj == null ? null : String.valueOf(accountObj);
		if (!StringUtils.hasText(account == null ? null : account.trim())) {
			throw new BadRequestException("更改的账号必填");
		}
		Object nickObj = merged.get("nick");
		String nick = nickObj == null ? null : String.valueOf(nickObj);
		if (nick != null) {
			nick = nick.trim();
		}

		Path avatarLocalPathOrNull = null;
		if (avatar != null && !avatar.isEmpty()) {
			if (avatar.getSize() <= 0) {
				throw new ResourceException("更新头像失败");
			}
			String original = avatar.getOriginalFilename();
			String suffix = null;
			if (original != null) {
				int dot = original.lastIndexOf('.');
				if (dot >= 0 && dot < original.length() - 1) {
					suffix = original.substring(dot);
				}
			}
			try {
				avatarLocalPathOrNull = Files.createTempFile("wechat-kf-avatar-", suffix);
				try (InputStream in = avatar.getInputStream()) {
					Files.copy(in, avatarLocalPathOrNull, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception e) {
				if (avatarLocalPathOrNull != null) {
					try {
						Files.deleteIfExists(avatarLocalPathOrNull);
					} catch (Exception ignored) {
						// ignore cleanup failure
					}
				}
				throw new ResourceException("更新头像失败");
			}
		}

		if (!StringUtils.hasText(nick) && avatarLocalPathOrNull == null) {
			throw new BadRequestException("请填写需要修改的昵称或头像");
		}

		try {
			wechatOfficialAccountKfUpdateService.updateWechatKf(
					authorizerAppid,
					account.trim(),
					StringUtils.hasText(nick) ? nick.trim() : null,
					avatarLocalPathOrNull);
			return ApiResult.ok(Map.of("status", Boolean.TRUE));
		} finally {
			if (avatarLocalPathOrNull != null) {
				try {
					Files.deleteIfExists(avatarLocalPathOrNull);
				} catch (Exception ignored) {
					// ignore cleanup failure
				}
			}
		}
	}
}
