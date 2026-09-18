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
import cn.shopex.ecshopx.common.dispatch.SyncWechatTagsDispatchPublisher;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.WechatFansTagsBatchSetService;
import cn.shopex.ecshopx.members.service.WechatFansTagsCreateService;
import cn.shopex.ecshopx.members.service.WechatFansTagsDeleteService;
import cn.shopex.ecshopx.members.service.WechatFansTagsListService;
import cn.shopex.ecshopx.members.service.WechatFansTagsUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("membersAdminV1WechatFansTags")
@RequestMapping("/api/v1/wechat")
public class WechatFansTagsController {

	private final WechatFansTagsCreateService wechatFansTagsCreateService;
	private final WechatFansTagsBatchSetService wechatFansTagsBatchSetService;
	private final WechatFansTagsUpdateService wechatFansTagsUpdateService;
	private final WechatFansTagsDeleteService wechatFansTagsDeleteService;
	private final SyncWechatTagsDispatchPublisher syncWechatTagsDispatchPublisher;
	private final WechatFansTagsListService wechatFansTagsListService;

	public WechatFansTagsController(
			WechatFansTagsCreateService wechatFansTagsCreateService,
			WechatFansTagsBatchSetService wechatFansTagsBatchSetService,
			WechatFansTagsUpdateService wechatFansTagsUpdateService,
			WechatFansTagsDeleteService wechatFansTagsDeleteService,
			SyncWechatTagsDispatchPublisher syncWechatTagsDispatchPublisher,
			WechatFansTagsListService wechatFansTagsListService) {
		this.wechatFansTagsCreateService = wechatFansTagsCreateService;
		this.wechatFansTagsBatchSetService = wechatFansTagsBatchSetService;
		this.wechatFansTagsUpdateService = wechatFansTagsUpdateService;
		this.wechatFansTagsDeleteService = wechatFansTagsDeleteService;
		this.syncWechatTagsDispatchPublisher = syncWechatTagsDispatchPublisher;
		this.wechatFansTagsListService = wechatFansTagsListService;
	}

	@Activated(routeAlias = "wxTags.create")
	@PostMapping(value = "/tag", name = "微信标签创建", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> wxtagCreate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object raw = merged.get("tag_name");
		boolean invalid =
				(raw == null)
						|| (raw instanceof Boolean b && !b)
						|| (raw instanceof Number n && n.doubleValue() == 0.0);
		if (!invalid) {
			String s = String.valueOf(raw);
			if (s.isEmpty() || "0".equals(s)) {
				invalid = true;
			}
		}
		if (invalid) {
			throw new BadRequestException("用户标签必填", 411);
		}
		String tagName = String.valueOf(raw);
		if (tagName.getBytes(StandardCharsets.UTF_8).length > 30) {
			throw new BadRequestException("标签名长度必须小于30个字符!", 411);
		}

		long companyId = readCompanyIdForWechatTag(request);
		String authorizerAppid = requireAuthorizerAppidForWechat(request);

		Map<String, Object> data =
				wechatFansTagsCreateService.wxtagCreate(authorizerAppid, companyId, tagName);
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

	private static String readAuthorizerAppidFromJwtAllowBlank(HttpServletRequest request) {
		return readAuthorizerAppidFromJwt(request);
	}

	private static String requireAuthorizerAppidForWechat(HttpServletRequest request) {
		String appid = readAuthorizerAppidFromJwt(request);
		if (appid == null || appid.isBlank()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		return appid;
	}

	private static List<Long> parseTagIdsFromMergedOrEmpty(Map<String, Object> merged) {
		Object raw = merged.get("tagIds");
		if (raw == null
				|| (raw instanceof Boolean b && !b)
				|| (raw instanceof Number n && n.doubleValue() == 0.0)) {
			return List.of();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : s.split(",")) {
			String t = part.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException e) {
				throw new BadRequestException("标签参数格式错误", 411);
			}
		}
		return out;
	}

	private static long readCompanyIdForWechatTag(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("company_id不能为空！", 411);
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("company_id不能为空！", 411);
		}
		if (companyId <= 0L) {
			throw new BadRequestException("company_id不能为空！", 411);
		}
		return companyId;
	}

	@Activated(routeAlias = "wxTags.update")
	@PutMapping(value = "/tag", name = "微信标签更新", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> wxtagUpdate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object rawTagId = merged.get("tag_id");
		if (mergedParamLooksUnchecked(rawTagId)) {
			throw new BadRequestException("标签Id必填", 411);
		}
		long tagId;
		try {
			tagId = Long.parseLong(String.valueOf(rawTagId));
		} catch (NumberFormatException e) {
			throw new BadRequestException("标签Id格式不正确", 400);
		}

		Object rawTagName = merged.get("tag_name");
		if (mergedParamLooksUnchecked(rawTagName)) {
			throw new BadRequestException("用户标签名必填", 411);
		}
		String tagName = String.valueOf(rawTagName);
		if (tagName.getBytes(StandardCharsets.UTF_8).length > 30) {
			throw new BadRequestException("标签名长度必须小于30个字符!", 411);
		}

		String authorizerAppid = requireAuthorizerAppidForWechat(request);
		Long companyId = readCompanyIdNullableFromJwt(request);

		Map<String, Object> data =
				wechatFansTagsUpdateService.wxtagUpdate(authorizerAppid, companyId, tagId, tagName);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static boolean mergedParamLooksUnchecked(Object raw) {
		boolean invalid =
				(raw == null)
						|| (raw instanceof Boolean b && !b)
						|| (raw instanceof Number n && n.doubleValue() == 0.0);
		if (!invalid) {
			String s = String.valueOf(raw);
			if (s.isEmpty() || "0".equals(s)) {
				invalid = true;
			}
		}
		return invalid;
	}

	private static Long readCompanyIdNullableFromJwt(HttpServletRequest request) {
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

	@Activated(routeAlias = "wxTags.delete")
	@DeleteMapping(value = "/tag", name = "删除微信标签", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> wxtagDelete(HttpServletRequest request) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		Object rawTagId = merged.get("tag_id");
		String tagIdText = rawTagId == null ? "" : String.valueOf(rawTagId).trim();
		long tagId = LeadingNumberParser.parseAsLong(tagIdText);
		if (tagId == 0L || tagId == 1L || tagId == 2L) {
			throw new BadRequestException("不能修改0/1/2这三个系统默认保留的标签", 411);
		}

		String authorizerAppid = requireAuthorizerAppidForWechat(request);
		long companyId = readCompanyIdForWechatTag(request);

		Map<String, Object> data =
				wechatFansTagsDeleteService.wxtagDelete(authorizerAppid, companyId, tagId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wxTags.list")
	@GetMapping(value = "/tags", name = "微信标签列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getWxtagList(HttpServletRequest request) {
		long companyId = readCompanyIdForWechatTag(request);
		String authorizerAppid = readAuthorizerAppidFromJwt(request);
		List<Map<String, Object>> list = wechatFansTagsListService.getWxtagList(authorizerAppid, companyId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(list));
	}

	@Activated(routeAlias = "wxTags.sync")
	@GetMapping(value = "/tag/sync", name = "同步标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> syncWechatTags(HttpServletRequest request) {
		long companyId = readCompanyIdForWechatTag(request);
		String authorizerAppid = requireAuthorizerAppidForWechat(request);
		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", companyId);
		payload.put("authorizer_appid", authorizerAppid);
		syncWechatTagsDispatchPublisher.publish(payload);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "wxTags.batchSet")
	@PatchMapping(value = "/tag/batchSet", name = "批量打标签", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> batchSetUserTags(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object rawOpenIds = merged.get("openIds");
		boolean openIdsInvalid =
				(rawOpenIds == null)
						|| (rawOpenIds instanceof Boolean b && !b)
						|| (rawOpenIds instanceof Number n && n.doubleValue() == 0.0);
		if (!openIdsInvalid) {
			String s = String.valueOf(rawOpenIds).trim();
			if (s.isEmpty() || "0".equals(s)) {
				openIdsInvalid = true;
			}
		}
		if (openIdsInvalid) {
			throw new BadRequestException("用户标识必填", 411);
		}
		String openIdsStr = String.valueOf(rawOpenIds).trim();
		List<String> openIdList = new ArrayList<>();
		for (String part : openIdsStr.split(",")) {
			String t = part.trim();
			if (!t.isEmpty()) {
				openIdList.add(t);
			}
		}
		if (openIdList.isEmpty()) {
			throw new BadRequestException("用户标识必填", 411);
		}

		long companyId = readCompanyIdForWechatTag(request);

		if (openIdList.size() == 1) {
			String openId = openIdList.get(0);
			String authorizerProbe = readAuthorizerAppidFromJwtAllowBlank(request);
			if (wechatFansTagsBatchSetService.isFanAbsentForBatchSetTriple(openId, authorizerProbe, companyId)) {
				return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
			}
		}

		String authorizerAppid = requireAuthorizerAppidForWechat(request);
		List<Long> tagIds = parseTagIdsFromMergedOrEmpty(merged);
		wechatFansTagsBatchSetService.batchSetUserTags(authorizerAppid, companyId, openIdList, tagIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
