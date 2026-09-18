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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiImageDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiImageListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiImageSavePort;
import cn.shopex.ecshopx.common.openapi.OpenapiImageUploadTokenPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV1Image")
@RequestMapping("/api/openapi/internal/v1")
public class ImageController extends OpenapiBaseController {

	private final OpenapiImageUploadTokenPort imageUploadTokenPort;
	private final OpenapiImageListPort imageListPort;
	private final OpenapiImageSavePort imageSavePort;
	private final OpenapiImageDeletePort imageDeletePort;

	public ImageController(
			OpenapiImageUploadTokenPort imageUploadTokenPort,
			OpenapiImageListPort imageListPort,
			OpenapiImageSavePort imageSavePort,
			OpenapiImageDeletePort imageDeletePort) {
		this.imageUploadTokenPort = imageUploadTokenPort;
		this.imageListPort = imageListPort;
		this.imageSavePort = imageSavePort;
		this.imageDeletePort = imageDeletePort;
	}

	@PostMapping(value = "/ecx.image.list", name = "开放接口获取图片列表")
	public OpenapiEnvelope getImageList(
			HttpServletRequest request,
			@RequestParam(name = "storage", required = false) String storageParam,
			@RequestParam(name = "image_name", required = false) String imageNameParam,
			@RequestParam(name = "disabled", required = false) String disabledParam,
			@RequestParam(name = "image_cat_id", required = false) String imageCatIdParam,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiImageListParams.BuildResult params = OpenapiImageListParams.build(
				storageParam,
				imageNameParam,
				disabledParam,
				imageCatIdParam,
				distributorIdParam,
				pageParam,
				pageSizeParam,
				body);
		Map<String, Object> data = imageListPort.getImageList(
				request,
				companyId,
				params.queryMap(),
				params.disabledFilterActive(),
				params.disabledFilterValue(),
				params.imageNameFilterActive(),
				params.imageNameFilterValue(),
				params.imageCatIdFilterActive(),
				params.imageCatIdFilterValue());
		return new OpenapiEnvelope("success", "E0000", "ok", data);
	}

	@PostMapping(value = "/ecx.image.del", name = "开放接口删除图片")
	public OpenapiEnvelope deleteImage(
			HttpServletRequest request,
			@RequestParam(name = "image_id", required = false) String imageIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiImageDeleteParams.ParseResult parsed =
				OpenapiImageDeleteParams.parse(imageIdParam, body);
		if (parsed instanceof OpenapiImageDeleteParams.TooMany) {
			return OpenapiEnvelope.fail("E0002", "单次最多删除100个图片", null);
		}
		OpenapiImageDeleteParams.Ok ok = (OpenapiImageDeleteParams.Ok) parsed;
		imageDeletePort.deleteImage(companyId, ok.imageIds());
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return new OpenapiEnvelope("success", "E0000", "ok", data);
	}

	@PostMapping(value = "/ecx.image.save", name = "开放接口保存图片元数据")
	public OpenapiEnvelope saveImage(
			HttpServletRequest request,
			@RequestParam(name = "image_name", required = false) String imageNameParam,
			@RequestParam(name = "storage", required = false) String storageParam,
			@RequestParam(name = "image_url", required = false) String imageUrlParam,
			@RequestParam(name = "image_cat_id", required = false) String imageCatIdParam,
			@RequestParam(name = "brief", required = false) String briefParam,
			@RequestParam(name = "image_type", required = false) String imageTypeParam,
			@RequestParam(name = "image_full_url", required = false) String imageFullUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiImageSaveParams.BuildResult built = OpenapiImageSaveParams.build(
				imageNameParam,
				storageParam,
				imageUrlParam,
				imageCatIdParam,
				briefParam,
				imageTypeParam,
				imageFullUrlParam,
				body);
		Map<String, Object> data = imageSavePort.saveImage(companyId, built.payload());
		return new OpenapiEnvelope("success", "E0000", "ok", data);
	}

	@PostMapping(value = "/ecx.image.upload_token", name = "开放接口获取云存储上传Token")
	public OpenapiEnvelope getUploadToken(
			HttpServletRequest request,
			@RequestParam(name = "filetype", required = false) String filetypeParam,
			@RequestParam(name = "group", required = false) String groupParam,
			@RequestParam(name = "filename", required = false) String filenameParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String fileType = OpenapiRequestParams.mergeString(filetypeParam, body, "filetype");
		String group = OpenapiRequestParams.mergeString(groupParam, body, "group");
		String filename = OpenapiRequestParams.mergeString(filenameParam, body, "filename");
		Map<String, Object> data = imageUploadTokenPort.getUploadToken(
				fileType,
				companyId,
				group != null ? group : "",
				filename);
		return new OpenapiEnvelope("success", "E0000", "ok", data);
	}

	@PostMapping(value = "/ecx.image.upload_localimage", name = "开放接口本地存储上传图片")
	public OpenapiEnvelope uploadeImage(
			HttpServletRequest request,
			@RequestParam(name = "images", required = false) MultipartFile images,
			@RequestParam(name = "filetype", required = false) String filetypeParam,
			@RequestParam(name = "group", required = false) String groupParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		if (images == null || images.isEmpty()) {
			throw new ResourceException("请上传有效图片");
		}
		String fileType = resolveFileTypeForUploade(filetypeParam, body);
		String group = OpenapiRequestParams.mergeString(groupParam, body, "group");
		String imageUrl = imageUploadTokenPort.uploadLocalImage(fileType, companyId, group, images);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("image_url", imageUrl);
		return new OpenapiEnvelope("success", "E0000", "ok", data);
	}

	private static String resolveFileTypeForUploade(String filetypeParam, Map<String, Object> body) {
		String raw = OpenapiRequestParams.originalString(filetypeParam, body, "filetype");
		if (raw == null || raw.isEmpty() || "0".equals(raw)) {
			return "image";
		}
		return raw;
	}
}
