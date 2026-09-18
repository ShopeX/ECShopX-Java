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

package cn.shopex.ecshopx.members.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.WechatFansInfoService;
import cn.shopex.ecshopx.members.service.WechatFansListService;
import cn.shopex.ecshopx.members.service.WechatFansRemarkService;
import cn.shopex.ecshopx.members.service.WechatFansSyncService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("membersAdminV1WechatFans")
@RequestMapping("/api/v1/wechat")
public class WechatFansController {

	private final WechatFansRemarkService wechatFansRemarkService;
	private final WechatFansInfoService wechatFansInfoService;
	private final WechatFansListService wechatFansListService;
	private final WechatFansSyncService wechatFansSyncService;

	public WechatFansController(
			WechatFansRemarkService wechatFansRemarkService,
			WechatFansInfoService wechatFansInfoService,
			WechatFansListService wechatFansListService,
			WechatFansSyncService wechatFansSyncService) {
		this.wechatFansRemarkService = wechatFansRemarkService;
		this.wechatFansInfoService = wechatFansInfoService;
		this.wechatFansListService = wechatFansListService;
		this.wechatFansSyncService = wechatFansSyncService;
	}

	@Activated(routeAlias = "wxFans.list")
	@GetMapping(value = "/fans/list", name = "微信用户列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxFansList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") String pageStr,
			@RequestParam(value = "pageSize", defaultValue = "100") String pageSizeStr,
			@RequestParam(value = "nickname", required = false) String nickname,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "tag_id", required = false) String tagId,
			@RequestParam(value = "subscribed", required = false) String subscribed) {
		int page = parseIntQueryOrDefault(pageStr, 1);
		int pageSize = parseIntQueryOrDefault(pageSizeStr, 100);
		String authorizerAppid = readAuthorizerAppidFromJwt(request);
		Long companyId = readNullableCompanyIdFromJwt(request);
		Map<String, Object> data =
				wechatFansListService.getWxFansList(
						authorizerAppid,
						companyId,
						page,
						pageSize,
						nickname,
						remark,
						tagId,
						subscribed);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseIntQueryOrDefault(String raw, int defaultValue) {
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Activated(routeAlias = "wxFans.info")
	@GetMapping(value = "/fans", name = "微信用户基本信息")
	public ResponseEntity<ApiResult<Object>> getWxFansInfo(
			@RequestParam(value = "open_id", required = false) String openId) {
		Object body = wechatFansInfoService.getWxFansInfo(openId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "wxFans.remark")
	@PutMapping(value = "/fans/remark", name = "修改备注", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> wxremark(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object rawOpenId = merged.get("open_id");
		boolean invalid =
				(rawOpenId == null)
						|| (rawOpenId instanceof Boolean b && !b)
						|| (rawOpenId instanceof Number n && n.doubleValue() == 0.0);
		if (!invalid) {
			String s = String.valueOf(rawOpenId);
			if (s.isEmpty() || "0".equals(s)) {
				invalid = true;
			}
		}
		if (invalid) {
			throw new BadRequestException("用户标识必填", 411);
		}
		String openId = String.valueOf(rawOpenId);

		Object rawRemark = merged.get("remark");
		String remarkForCheck = (rawRemark == null) ? "" : String.valueOf(rawRemark);
		if (remarkForCheck.getBytes(StandardCharsets.UTF_8).length > 30) {
			throw new BadRequestException("备注长度必须小于30个字符!", 411);
		}

		String authorizerAppid = readAuthorizerAppidFromJwt(request);
		Long companyId = readNullableCompanyIdFromJwt(request);

		Map<String, Object> data =
				wechatFansRemarkService.wxremark(authorizerAppid, companyId, openId, remarkForCheck);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String readAuthorizerAppidFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object appidObj = jwt.get("authorizer_appid");
		if (appidObj == null) {
			return null;
		}
		return String.valueOf(appidObj);
	}

	private static Long readNullableCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
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

	@Activated(routeAlias = "wxFans.sync")
	@GetMapping(value = "/fans/sync", name = "同步粉丝")
	public ResponseEntity<ApiResult<Map<String, Object>>> syncWechatFans(HttpServletRequest request) {
		Long companyId = readNullableCompanyIdFromJwt(request);
		if (companyId == null) {
			throw new BadRequestException("登录上下文无效");
		}
		String authorizerAppidOrNull = readAuthorizerAppidFromJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object rawCompany = jwt.get("company_id");
		String companyIdRawForSha1 = rawCompany == null ? "" : String.valueOf(rawCompany).trim();

		wechatFansSyncService.syncWechatFans(authorizerAppidOrNull, companyId.longValue(), companyIdRawForSha1);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wxFans.tags")
	@GetMapping(value = "/fans/tags", name = "用户标签列表")
	public ResponseEntity<Void> getWxTagsOfUser() {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "wxTags.fans")
	@GetMapping(value = "/tag/fans", name = "标签下用户列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxFansOfTag(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") String pageStr,
			@RequestParam(value = "pageSize", defaultValue = "100") String pageSizeStr,
			@RequestParam(value = "tag_id", required = false) String tagId) {
		int page = parseIntQueryOrDefault(pageStr, 1);
		int pageSize = parseIntQueryOrDefault(pageSizeStr, 100);
		String authorizerAppid = readAuthorizerAppidFromJwt(request);
		Long companyId = readNullableCompanyIdFromJwt(request);
		if (tagId == null || tagId.isBlank() || "0".equals(tagId.trim())) {
			throw new BadRequestException("tag_id必填！", 411);
		}
		Map<String, Object> data =
				wechatFansListService.getWxFansOfTag(authorizerAppid, companyId, page, pageSize, tagId.trim());
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
