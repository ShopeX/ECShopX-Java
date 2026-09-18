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
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialListService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialNewsService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialStatsService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialUploadService;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController("wechatAdminV1Material")
@RequestMapping("/api/v1/wechat")
public class MaterialController {

	private static final Set<String> ALLOWED_TYPES = Set.of("image", "thumb", "video", "voice");

	private final WechatOfficialAccountMaterialUploadService wechatOfficialAccountMaterialUploadService;
	private final WechatOfficialAccountMaterialNewsService wechatOfficialAccountMaterialNewsService;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService;
	private final WechatOfficialAccountMaterialListService wechatOfficialAccountMaterialListService;
	private final WechatOfficialAccountMaterialStatsService wechatOfficialAccountMaterialStatsService;

	public MaterialController(
			cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialUploadService wechatOfficialAccountMaterialUploadService,
			cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialNewsService wechatOfficialAccountMaterialNewsService,
			cn.shopex.ecshopx.wechat.service.WechatAuthQueryService wechatAuthQueryService,
			cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService,
			cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialListService wechatOfficialAccountMaterialListService,
			cn.shopex.ecshopx.wechat.service.WechatOfficialAccountMaterialStatsService wechatOfficialAccountMaterialStatsService) {
		this.wechatOfficialAccountMaterialUploadService = wechatOfficialAccountMaterialUploadService;
		this.wechatOfficialAccountMaterialNewsService = wechatOfficialAccountMaterialNewsService;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wechatOpenPlatformAuthorizerTokenService = wechatOpenPlatformAuthorizerTokenService;
		this.wechatOfficialAccountMaterialListService = wechatOfficialAccountMaterialListService;
		this.wechatOfficialAccountMaterialStatsService = wechatOfficialAccountMaterialStatsService;
	}

	@Activated(routeAlias = "wechat.material.upload")
	@PostMapping(
			value = "/material",
			name = "上传素材",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> uploadMaterial(
			HttpServletRequest request,
			@RequestParam(value = "is_temp", required = false) String isTempRaw,
			@RequestParam("type") String type,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "description", required = false) String description) {
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		boolean isTemporary = "true".equals(isTempRaw == null ? null : isTempRaw.trim());
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid);
		if (!isMultipartFormDataContentType(request)) {
			throw new BadRequestException("缺少上传文件");
		}
		String normalizedType = type == null ? "" : type.trim().toLowerCase();
		if (!ALLOWED_TYPES.contains(normalizedType)) {
			throw new BadRequestException("素材类型不正确");
		}
		if (isTemporary && "video".equals(normalizedType)) {
			throw new BadRequestException(
					"微信公众平台「上传临时素材」接口仅接受 media 文件与 type 参数，不提供标题与简介字段；临时视频无法与永久素材 add_material 的 description JSON 对齐");
		}
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("缺少上传文件");
		}
		if (file.getSize() <= 0) {
			throw new ResourceException("上传素材失败");
		}
		Map<String, Object> payload = wechatOfficialAccountMaterialUploadService.uploadMaterial(
				authorizerAppid, isTemporary, normalizedType, file, title, description);
		return ApiResult.ok(payload);
	}

	private static boolean isMultipartFormDataContentType(HttpServletRequest request) {
		String raw = request.getContentType();
		if (raw == null) {
			return false;
		}
		String ct = raw.trim();
		int semi = ct.indexOf(';');
		if (semi >= 0) {
			ct = ct.substring(0, semi).trim();
		}
		return MediaType.MULTIPART_FORM_DATA_VALUE.equalsIgnoreCase(ct);
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

	@Activated(routeAlias = "wechat.news.image.upload")
	@PostMapping(
			value = "/news/image",
			name = "上传图文内图片",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> uploadArticleImage(
			HttpServletRequest request, @RequestParam(value = "file", required = false) MultipartFile file) {
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid);
		if (!isMultipartFormDataContentType(request)) {
			throw new BadRequestException("缺少上传文件");
		}
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("缺少上传文件");
		}
		if (file.getSize() <= 0) {
			throw new ResourceException("上传图片失败");
		}
		Map<String, Object> payload = wechatOfficialAccountMaterialUploadService.uploadArticleImage(authorizerAppid, file);
		Object u = payload.get("url");
		boolean validUrl = u instanceof String s && StringUtils.hasText(s);
		if (!validUrl) {
			throw new ResourceException("微信接口调用失败");
		}
		return ApiResult.ok(payload);
	}

	@Activated(routeAlias = "wechat.material.delete")
	@DeleteMapping(
			value = "/material",
			name = "删除素材",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> deleteMaterial(
			HttpServletRequest request,
			@RequestParam(value = "media_id", required = false) String mediaId) {
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		if (mediaId == null || !StringUtils.hasText(mediaId.trim())) {
			throw new BadRequestException("请传入素材media_id");
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		wechatOfficialAccountMaterialUploadService.deleteMaterial(authorizerAppid.trim(), mediaId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "wechat.material.list")
	@GetMapping(value = "/material", name = "永久素材列表")
	public ApiResult<Object> getMaterialLists(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw) {
		boolean pageMissing = pageRaw == null || pageRaw.trim().isEmpty();
		boolean pageSizeMissing = pageSizeRaw == null || pageSizeRaw.trim().isEmpty();
		if (pageMissing || pageSizeMissing) {
			Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
			if (pageMissing) {
				fieldErrors.put("page", List.of("validation.required"));
			}
			if (pageSizeMissing) {
				fieldErrors.put("pageSize", List.of("validation.required"));
			}
			throw new ResourceException("获取微信图片列表出错.", fieldErrors);
		}
		int page;
		try {
			page = Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("获取微信图片列表出错.", Map.of("page", List.of("validation.integer")));
		}
		if (page < 1) {
			throw new BadRequestException("获取微信图片列表出错.", Map.of("page", List.of("validation.min.numeric")));
		}
		int pageSize;
		try {
			pageSize = Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("获取微信图片列表出错.", Map.of("pageSize", List.of("validation.integer")));
		}
		if (pageSize < 1) {
			throw new BadRequestException(
					"获取微信图片列表出错.", Map.of("pageSize", List.of("validation.min.numeric")));
		}
		if (pageSize > 50) {
			throw new BadRequestException(
					"获取微信图片列表出错.", Map.of("pageSize", List.of("validation.max.numeric")));
		}
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		Object body = wechatOfficialAccountMaterialListService.getMaterialLists(authorizerAppid.trim(), type, page, pageSize);
		return ApiResult.ok(body);
	}

	@Activated(routeAlias = "wechat.material.stats")
	@GetMapping(value = "/material/stats", name = "素材状态")
	public ApiResult<Map<String, Object>> getMaterialStats(HttpServletRequest request) {
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		Map<String, Object> data = wechatOfficialAccountMaterialStatsService.getMaterialStats(authorizerAppid.trim());
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "wechat.material.news.add")
	@PostMapping(
			value = "/news",
			name = "创建图文",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createNews(
			HttpServletRequest request, @FlexibleBody(required = false) JsonNode root) {
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid);
		if (root == null) {
			throw new BadRequestException("请填写图文参数");
		}
		if (!root.isObject()) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		JsonNode body = root.path("body");
		if (body.isMissingNode() || body.isNull()) {
			throw new BadRequestException("请填写图文参数");
		}
		if (isInvalidNewsBodyNode(body)) {
			throw new BadRequestException("请填写图文参数");
		}
		List<Map<String, Object>> articles = new ArrayList<>();
		for (JsonNode item : body) {
			if (!item.isObject()) {
				throw new BadRequestException("请填写图文参数");
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			item.fields().forEachRemaining(e -> row.put(e.getKey(), jsonNodeToPlain(e.getValue())));
			articles.add(row);
		}
		Map<String, Object> data = wechatOfficialAccountMaterialNewsService.createNews(authorizerAppid, articles);
		return ApiResult.ok(data);
	}

	private static boolean isInvalidNewsBodyNode(JsonNode raw) {
		if (raw.isTextual()) {
			String t = raw.asText();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			if ("0".contentEquals(t.trim())) {
				return true;
			}
		}
		if (raw.isNumber() && raw.intValue() == 0) {
			return true;
		}
		if (raw.isBoolean() && !raw.booleanValue()) {
			return true;
		}
		if (raw.isArray() && raw.isEmpty()) {
			return true;
		}
		return !raw.isArray();
	}

	private static Object jsonNodeToPlain(JsonNode n) {
		if (n == null || n.isNull() || n.isMissingNode()) {
			return null;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isNumber()) {
			if (n.isIntegralNumber()) {
				return n.longValue();
			}
			return n.doubleValue();
		}
		if (n.isTextual()) {
			return n.asText();
		}
		if (n.isArray()) {
			List<Object> list = new ArrayList<>();
			for (JsonNode c : n) {
				list.add(jsonNodeToPlain(c));
			}
			return list;
		}
		if (n.isObject()) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			n.fields().forEachRemaining(e -> m.put(e.getKey(), jsonNodeToPlain(e.getValue())));
			return m;
		}
		return null;
	}

	@Activated(routeAlias = "wechat.material.news.get")
	@GetMapping(value = "/news/**", name = "图文详情")
	public ApiResult<Object> getNewsMaterial(HttpServletRequest request) {
		String materialId = extractNewsMaterialIdFromRequestUri(request);
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		Object body = wechatOfficialAccountMaterialListService.getNewsMaterial(authorizerAppid.trim(), materialId);
		return ApiResult.ok(body);
	}

	private static String extractNewsMaterialIdFromRequestUri(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (!StringUtils.hasText(uri)) {
			throw new BadRequestException("缺少图文素材ID");
		}
		final String marker = "/news/";
		int idx = uri.indexOf(marker);
		String encodedTail;
		if (idx >= 0) {
			encodedTail = uri.substring(idx + marker.length());
		} else if (uri.endsWith("/news")) {
			encodedTail = "";
		} else {
			throw new BadRequestException("缺少图文素材ID");
		}
		if (!StringUtils.hasText(encodedTail)) {
			throw new BadRequestException("缺少图文素材ID");
		}
		String decoded = URLDecoder.decode(encodedTail, StandardCharsets.UTF_8);
		String materialId = decoded.trim();
		if (!StringUtils.hasText(materialId)) {
			throw new BadRequestException("缺少图文素材ID");
		}
		return materialId;
	}

	@Activated(routeAlias = "wechat.material.news.update")
	@PutMapping(
			value = "/news",
			name = "修改图文素材",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateArticle(
			HttpServletRequest request,
			@RequestParam(value = "media_id", required = false) String mediaId,
			@FlexibleBody(required = false) JsonNode root) {
		String authorizerAppid = resolveAuthorizerAppid(request);
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid);
		String mediaIdTrimmed = mediaId == null ? null : mediaId.trim();
		if (!StringUtils.hasText(mediaIdTrimmed)) {
			throw new BadRequestException("请传入图文素材ID");
		}
		if (root == null) {
			throw new BadRequestException("请填写图文参数");
		}
		if (!root.isObject()) {
			throw new ResourceException("当前账号未绑定公众号，请先绑定公众号");
		}
		JsonNode body = root.path("body");
		if (body.isMissingNode() || body.isNull()) {
			throw new BadRequestException("请填写图文参数");
		}
		if (isInvalidNewsBodyNode(body)) {
			throw new BadRequestException("请填写图文参数");
		}
		List<Map<String, Object>> articles = new ArrayList<>();
		for (JsonNode item : body) {
			if (!item.isObject()) {
				throw new BadRequestException("请填写图文参数");
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			item.fields().forEachRemaining(e -> row.put(e.getKey(), jsonNodeToPlain(e.getValue())));
			articles.add(row);
		}
		wechatOfficialAccountMaterialNewsService.updateArticle(authorizerAppid.trim(), mediaIdTrimmed, articles);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}
}
