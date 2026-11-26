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
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageDelBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageDelSuccessEnvelopeDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-图片")
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

	@Operation(
			summary = "开放接口删除图片",
			description =
					"按签名鉴权得到的租户 company_id 批量软删除图片（upload_images.disabled=true，不删除物理文件）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.image.del, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " image_id 双通道：body 含键时取原始值（null 不回退 query），否则用 query 原样；"
							+ " 逗号分隔多个，单次最多 100 个（超出返回 E0002）；空/\"0\" 报 E0001；"
							+ " 更新影响 0 行返回 status=fail 且 code=E0000（未查询到更新数据，PHP 旧契约）。"
							+ " 副作用：写库批量更新 upload_images.disabled；无异步/第三方调用。",
			operationId = "ecx.image.del")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=ok，data 仅含 status=true；"
								+ " 超过100个：status=fail, code=E0002 单次最多删除100个图片；"
								+ " 未选图片：status=fail, code=E0001 请选择要删除的图片；"
								+ " 无匹配记录：status=fail 且 code=E0000 未查询到更新数据（PHP 旧契约）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权。失败 data 均为 null，见 EcxImageDelFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation = EcxImageDelSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-too-many",
											summary = "超过 100 个（E0002）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0002\","
															+ "\"message\":\"单次最多删除100个图片\",\"data\":null}"),
									@ExampleObject(
											name = "fail-no-rows",
											summary = "无匹配记录（PHP 旧契约：code=E0000 但 status=fail）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\",\"message\":\"未查询到更新数据\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.image.del", name = "开放接口删除图片")
	public OpenapiEnvelope deleteImage(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"图片 ID，逗号分隔多个；必填（空/\"0\" 报错）；单次最多 100 个"
											+ "（body 同名键优先，原样不 trim）",
							required = true,
							example = "99")
					@RequestParam(name = "image_id", required = false)
					String imageIdParam,
			@RequestBody(
							description =
									"可选 JSON body；image_id 与 query 同名双通道，body 含键时取原始值"
											+ "（null 视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxImageDelBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
