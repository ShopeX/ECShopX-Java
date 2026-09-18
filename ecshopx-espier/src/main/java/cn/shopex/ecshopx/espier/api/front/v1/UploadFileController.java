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

package cn.shopex.ecshopx.espier.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.espier.service.front.FrontWxappEspierUploadImageService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("espierFrontV1UploadFile")
@RequestMapping("/api/v1/h5app")
public class UploadFileController {

	private final FileStorageService fileStorageService;
	private final FrontWxappEspierUploadImageService uploadImageService;

	public UploadFileController(
			FileStorageService fileStorageService,
			FrontWxappEspierUploadImageService uploadImageService) {
		this.fileStorageService = fileStorageService;
		this.uploadImageService = uploadImageService;
	}

	@GetMapping(value = "/wxapp/espier/image_upload_token", name = "图片上传凭证")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPicUploadToken(
			HttpServletRequest request,
			@RequestParam(value = "filename", required = false) String filename,
			@RequestParam(value = "group", required = false) String group,
			@RequestParam(value = "filetype", required = false) String filetype) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		String normalizedFilename = normalizeScalarFormField(filename);
		String normalizedGroup = normalizeScalarFormField(group);
		String normalizedFiletype = normalizeScalarFormField(filetype);
		Map<String, Object> data = uploadImageService.getPicUploadToken(
				companyId, userId, normalizedFilename, normalizedGroup, normalizedFiletype);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(
			value = "/wxapp/espier/image_upload",
			name = "上传OSS图片",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadOssImage(
			HttpServletRequest request,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		Map<String, Object> merged = FlexibleHttpServletParameterMap.toObjectMap(request);
		String filename = normalizeScalarFormField(merged.get("filename"));
		String group = normalizeScalarFormField(merged.get("group"));
		if (file == null) {
			throw new BadRequestException("请上传文件数据");
		}
		if (file.isEmpty()) {
			throw new BadRequestException("上传文件内容为空", 400);
		}
		Map<String, Object> result = fileStorageService.upload("image", companyId, group, filename, file);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@PostMapping(value = "/wxapp/espier/upload", name = "上传图片")
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadImage(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		long userId = parsePositiveLongOrZero(claims.get("user_id"));
		MultipartFile resolvedFile = null;
		if (request instanceof MultipartHttpServletRequest mpr) {
			resolvedFile = mpr.getFile("file");
		}
		Map<String, Object> data = uploadImageService.uploadImage(companyId, userId, claims, resolvedFile);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(
			value = "/wxapp/espier/uploadlocal",
			name = "本地上传",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@FrontNoAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadeLocalImage(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		Map<String, Object> merged = FlexibleHttpServletParameterMap.toObjectMap(request);
		String rawFt = normalizeScalarFormField(merged.get("filetype"));
		String fileType = StringUtils.hasText(rawFt) ? rawFt.trim() : "image";
		String group = normalizeScalarFormField(merged.get("group"));
		MultipartFile images = request instanceof MultipartHttpServletRequest mpr ? mpr.getFile("images") : null;
		if (images == null) {
			throw new BadRequestException("请上传图片");
		}
		Map<String, Object> body = uploadImageService.uploadeLocalImage(companyId, fileType, group, images);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static String normalizeScalarFormField(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() ? null : s;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long resolveCompanyId(HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		throw new UnauthorizedException("Unable to authenticate user.");
	}

	private static long resolveCompanyIdForFrontNoAuth(HttpServletRequest request, Map<String, Object> claims) {
		long fromAttr = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		long fromQuery = parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
		if (fromQuery > 0L) {
			return fromQuery;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
