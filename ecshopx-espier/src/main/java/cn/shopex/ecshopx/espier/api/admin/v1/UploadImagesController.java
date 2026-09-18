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

package cn.shopex.ecshopx.espier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.UploadImagesService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("espierAdminV1UploadImages")
@RequestMapping("/api/v1/espier")
public class UploadImagesController {

	private final FileStorageService fileStorageService;
	private final UploadImagesService uploadImagesService;
	private final LangueProperties langueProperties;

	public UploadImagesController(
			FileStorageService fileStorageService,
			UploadImagesService uploadImagesService,
			LangueProperties langueProperties) {
		this.fileStorageService = fileStorageService;
		this.uploadImagesService = uploadImagesService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "espier.image.uploadToken.get")
	@PostMapping(value = "/image_upload_token", name = "获取上传图片token")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPicUploadToken(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object raw = merged.get("filename");
		String filename = null;
		if (raw != null) {
			String s = String.valueOf(raw).trim();
			filename = s.isEmpty() ? null : s;
		}
		Map<String, Object> result = fileStorageService.getPicUploadToken(companyId, filename);
		Object tokenObj = result.get("token");
		if (tokenObj == null || !(tokenObj instanceof Map)) {
			throw new ResourceException("上传凭证生成失败");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> token = (Map<String, Object>) tokenObj;
		return ResponseEntity.ok(ApiResult.ok(token));
	}

	@Activated(routeAlias = "espier.image.uploadToken.get")
	@PostMapping(value = "/video_upload_token", name = "获取上传视频token")
	public ResponseEntity<ApiResult<Map<String, Object>>> getVideoUploadToken(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object raw = merged.get("filename");
		String filename = null;
		if (raw != null) {
			String s = String.valueOf(raw).trim();
			filename = s.isEmpty() ? null : s;
		}
		Map<String, Object> result = fileStorageService.getUploadToken("videos", companyId, "", filename);
		Object tokenObj = result.get("token");
		if (tokenObj == null || !(tokenObj instanceof Map)) {
			throw new ResourceException("上传凭证生成失败");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> token = (Map<String, Object>) tokenObj;
		return ResponseEntity.ok(ApiResult.ok(token));
	}

	@Activated(routeAlias = "espier.oss.uploadToken.get")
	@PostMapping(value = "/oss_upload_token", name = "获取云存储上传token")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUploadToken(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		// 与 Laravel/Dingo $request->input() 一致：query/form/json body 均可；原先仅用 @RequestParam 无法读取 JSON body。
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String normalizedFileType = EspierAdminJwtControllerSupport.optionalTrimmedString(merged.get("filetype"));
		String normalizedGroup = EspierAdminJwtControllerSupport.optionalTrimmedString(merged.get("group"));
		String normalizedFilename = EspierAdminJwtControllerSupport.optionalTrimmedString(merged.get("filename"));
		Map<String, Object> result = fileStorageService.getUploadToken(
				normalizedFileType,
				companyId,
				normalizedGroup != null ? normalizedGroup : "",
				normalizedFilename);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "espier.oss.upload")
	@PostMapping(value = "/oss_upload", name = "OSS直传文件")
	public ResponseEntity<ApiResult<Map<String, Object>>> ossUpload(
			HttpServletRequest request,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Map<String, Object> map = FlexibleHttpServletParameterMap.toObjectMap(request);
		String fileType = EspierAdminJwtControllerSupport.optionalTrimmedString(map.get("filetype"));
		String group = EspierAdminJwtControllerSupport.optionalTrimmedString(map.get("group"));
		String filename = EspierAdminJwtControllerSupport.optionalTrimmedString(map.get("filename"));
		if (fileType == null || fileType.isEmpty()) {
			throw new BadRequestException("文件类型必填");
		}
		if (file == null) {
			throw new BadRequestException("上传文件内容必填");
		}
		if (file.getSize() == 0 || file.isEmpty()) {
			throw new BadRequestException("上传文件内容为空", 400);
		}
		Map<String, Object> result = fileStorageService.upload(fileType, companyId, group, filename, file);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "espier.localimage.upload")
	@PostMapping(value = "/uploade_image", name = "上传图片")
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadeImage(
			HttpServletRequest request,
			@RequestParam(value = "images", required = false) MultipartFile images,
			@RequestParam(value = "filetype", required = false) String fileType,
			@RequestParam(value = "group", required = false) String group,
			@RequestParam(value = "newfilename", required = false) String newFilename) {
		Map<String, Object> map = FlexibleHttpServletParameterMap.toObjectMap(request);
		String effectiveFileType = resolveEspierFileTypeForUploade(map);
		String effectiveGroup = resolveEspierGroup(map);
		return respondEspierImageUrl(request, images, effectiveFileType, effectiveGroup);
	}

	@Activated(routeAlias = "espier.image.cat.edit")
	@PostMapping(value = "/image/cat", name = "添加图片分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> editImageCat(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.remove("company_id");

		@SuppressWarnings("unchecked")
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		Long operatorId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id"));
		Long merchantIdFromJwt = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("merchant_id"));
		long merchantIdArg = merchantIdFromJwt == null ? 0L : merchantIdFromJwt;

		Map<String, Object> resultMap = uploadImagesService.editImageCat(
				companyId, operatorType, operatorId, merchantIdArg, merged);
		return ResponseEntity.ok(ApiResult.ok(resultMap));
	}

	@Activated(routeAlias = "espier.image.cat.children")
	@GetMapping(value = "/image/cat/children", name = "获取分类的子类")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCatChildren(
			HttpServletRequest request,
			@RequestParam(value = "image_cat_id", required = false) String imageCatId,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);

		@SuppressWarnings("unchecked")
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		Long operatorId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id"));
		Long merchantIdFromJwt = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("merchant_id"));

		String effectiveDistributorId = distributorId;
		if (effectiveDistributorId == null || effectiveDistributorId.isBlank()) {
			Object jwtDistributorId = user.get("distributor_id");
			if (jwtDistributorId != null) {
				effectiveDistributorId = String.valueOf(jwtDistributorId);
			}
		}

		String acceptLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data = uploadImagesService.getCatChildren(
				companyId,
				operatorType,
				operatorId,
				merchantIdFromJwt == null ? 0L : merchantIdFromJwt,
				imageCatId,
				effectiveDistributorId,
				acceptLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.image.cat.info")
	@GetMapping(value = "/image/cat/{image_cat_id}", name = "获取分类详情")
	public ResponseEntity<ApiResult<Object>> getCatInfo(
			HttpServletRequest request, @PathVariable("image_cat_id") String imageCatId) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		String acceptLang = RequestLangTag.current(langueProperties);
		Object data = uploadImagesService.getCatInfo(companyId, imageCatId, acceptLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.image.cat.del")
	@DeleteMapping(
			value = "/image/cat/{image_cat_id}",
			name = "删除图片分类",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<Map<String, Object>>> delImgCat(
			HttpServletRequest request,
			@PathVariable("image_cat_id") String imageCatId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		String path = imageCatId == null ? "" : imageCatId;
		Map<String, Object> resultMap = uploadImagesService.delImgCat(companyId, path);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(resultMap));
	}

	@Activated(routeAlias = "espier.image.save")
	@PostMapping(value = "/image", name = "保存图片")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveImage(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.remove("company_id");

		@SuppressWarnings("unchecked")
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		Long operatorId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id"));
		Long merchantIdFromJwt = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("merchant_id"));
		long merchantIdArg = merchantIdFromJwt == null ? 0L : merchantIdFromJwt;

		Map<String, Object> resultMap =
				uploadImagesService.saveImage(request, companyId, operatorType, operatorId, merchantIdArg, merged);
		return ResponseEntity.ok(ApiResult.ok(resultMap));
	}

	@Activated(routeAlias = "espier.image.list")
	@GetMapping(value = "/images", name = "获取图片列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getImageList(HttpServletRequest request) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Map<String, Object> queryMap = FlexibleHttpServletParameterMap.toObjectMap(request);

		@SuppressWarnings("unchecked")
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		Long operatorId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id"));
		Long merchantIdFromJwt = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("merchant_id"));
		long merchantIdArg = merchantIdFromJwt == null ? 0L : merchantIdFromJwt;

		Map<String, Object> data = uploadImagesService.getImageList(
				request, companyId, operatorType, operatorId, merchantIdArg, queryMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.image.del")
	@DeleteMapping(value = "/images", name = "删除图片")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteImage(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String imageId = EspierAdminJwtControllerSupport.optionalTrimmedString(merged.get("image_id"));
		uploadImagesService.deleteImage(companyId, imageId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "espier.image.move.cat")
	@PostMapping(value = "/image/movecat", name = "移动图片到指定分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> moveImageCat(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.remove("company_id");

		uploadImagesService.moveImageCat(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "espier.localimage.upload")
	@PostMapping(value = "/upload_localimage", name = "上传本地图片")
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadLocalImage(
			HttpServletRequest request,
			@RequestParam(value = "images", required = false) MultipartFile images,
			@RequestParam(value = "filetype", required = false) String fileType,
			@RequestParam(value = "group", required = false) String group) {
		Map<String, Object> map = FlexibleHttpServletParameterMap.toObjectMap(request);
		String effectiveFileType = resolveEspierFileTypeForUploade(map);
		String effectiveGroup = resolveEspierGroup(map);
		return respondEspierImageUrl(request, images, effectiveFileType, effectiveGroup);
	}

	private static String resolveEspierFileTypeForUploade(Map<String, Object> parameterMap) {
		String raw = EspierAdminJwtControllerSupport.optionalTrimmedString(parameterMap.get("filetype"));
		if (raw == null || "0".equals(raw)) {
			return "image";
		}
		return raw;
	}

	private static String resolveEspierGroup(Map<String, Object> parameterMap) {
		return EspierAdminJwtControllerSupport.optionalTrimmedString(parameterMap.get("group"));
	}

	private ResponseEntity<ApiResult<Map<String, Object>>> respondEspierImageUrl(
			HttpServletRequest request,
			MultipartFile images,
			String effectiveFileType,
			String group) {
		if (images == null) {
			throw new ResourceException("请上传图片");
		}
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Map<String, Object> result = fileStorageService.uploadeImage(effectiveFileType, companyId, group, images);
		Object tokenObj = result.get("token");
		if (tokenObj == null || !(tokenObj instanceof Map)) {
			throw new ResourceException("上传失败");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> token = (Map<String, Object>) tokenObj;
		Object keyObj = token.get("key");
		if (!(keyObj instanceof String)) {
			throw new ResourceException("上传失败");
		}
		String imageUrl = (String) keyObj;
		if (imageUrl.isBlank()) {
			throw new ResourceException("上传失败");
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("image_url", imageUrl)));
	}

}
