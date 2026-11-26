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
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageSaveBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageSaveSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageUploadLocalFormDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxImageUploadLocalSuccessEnvelopeDoc;
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

	@Operation(
			summary = "开放接口获取图片列表",
			description =
					"按签名鉴权得到的租户 company_id 查询图片素材列表（upload_images），created 倒序分页，"
							+ " 每行 5 字段（image_id/image_name/image_type/image_url/create_time）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.image.list, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 全部参数双通道（body 含键优先）；分页键为 pageSize（驼峰），非整数回退默认（page=1、pageSize=20），"
							+ " pageSize=0 时 list 为空但 total_count 为真实总数。"
							+ " 筛选：storage 空/\"0\" 按 image；disabled PHP-truthy 查已删除否则未删除；"
							+ " image_name 模糊；image_cat_id 整数精确；distributor_id 缺省 0（平台）。"
							+ " 只读，无写库/异步/第三方副作用；无业务校验失败分支。",
			operationId = "ecx.image.list")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=ok，data 含 total_count 与 list，"
								+ " 行字段见 EcxImageListItemDoc；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxImageListFailEnvelopeDoc。"
								+ " 本接口无业务校验失败分支。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation = EcxImageListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"total_count\":1,\"list\":[{\"image_id\":99,"
															+ "\"image_name\":\"示例图片\",\"image_type\":\"item\","
															+ "\"image_url\":\"https://example.test/key.jpg\","
															+ "\"create_time\":1764115200}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "查询成功（无匹配）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"total_count\":0,\"list\":[]}}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.image.list", name = "开放接口获取图片列表")
	public OpenapiEnvelope getImageList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "存储类型：image 图片；videos 视频；空/\"0\"/缺省按 image（body 同名键优先）",
							example = "image")
					@RequestParam(name = "storage", required = false)
					String storageParam,
			@Parameter(description = "图片名称，模糊匹配；空/\"0\" 不过滤（body 同名键优先）")
					@RequestParam(name = "image_name", required = false)
					String imageNameParam,
			@Parameter(
							description = "删除状态：PHP-truthy（非空且非 \"0\"）查已删除，否则查未删除（body 同名键优先）",
							example = "0")
					@RequestParam(name = "disabled", required = false)
					String disabledParam,
			@Parameter(description = "图片分类 ID 精确匹配；空/\"0\" 不过滤，非整数忽略（body 同名键优先）", example = "10")
					@RequestParam(name = "image_cat_id", required = false)
					String imageCatIdParam,
			@Parameter(description = "店铺 ID 精确匹配；缺省/非整数按 0（平台图片）（body 同名键优先）", example = "0")
					@RequestParam(name = "distributor_id", required = false)
					String distributorIdParam,
			@Parameter(description = "页码，从 1 起；非整数/缺省按 1（body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；非整数/缺省按 20；0 返回空列表；键名为驼峰 pageSize（body 同名键优先）",
							example = "20")
					@RequestParam(name = "pageSize", required = false)
					String pageSizeParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先；分页键为 pageSize（驼峰）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxImageListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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

	@Operation(
			summary = "开放接口保存图片元数据",
			description =
					"按签名鉴权得到的租户 company_id 新建图片素材记录（upload_images 插入一行，"
							+ "supplier_id/merchant_id=0，created/updated=当前时间），返回自增 image_id。"
							+ " 不上传文件本体，仅登记元数据（文件先经 upload_token/upload_localimage 上传）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.image.save, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 全部参数双通道（body 含键优先）。校验：image_name 必填≤100（文案写 50）、storage 必填、"
							+ " image_url 必填、image_type 提供时≤20；多条错误以「，」拼接一次性返回 E0001。"
							+ " 副作用：写库插入 upload_images；无异步/第三方调用。",
			operationId = "ecx.image.save")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=ok，data 仅含 image_id；"
								+ " 校验失败：status=fail, code=E0001（图片名称必填,不能超过50个字符/存储类型必填/"
								+ "图片分类长度不能超过20个字符/图片链接必填，多条以「，」拼接），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxImageSaveFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation = EcxImageSaveSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "保存成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"image_id\":99}}"),
									@ExampleObject(
											name = "fail-validation",
											summary = "校验失败（多条错误拼接）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"图片名称必填,不能超过50个字符，图片链接必填，\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.image.save", name = "开放接口保存图片元数据")
	public OpenapiEnvelope saveImage(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "图片名称；必填非空白，长度上限 100（错误文案写 50）（body 同名键优先）",
							required = true,
							example = "示例图片")
					@RequestParam(name = "image_name", required = false)
					String imageNameParam,
			@Parameter(description = "存储类型；必填（如 image/videos）（body 同名键优先）", required = true, example = "image")
					@RequestParam(name = "storage", required = false)
					String storageParam,
			@Parameter(
							description = "图片存储 key/链接；必填（body 同名键优先）",
							required = true,
							example = "path/key.jpg")
					@RequestParam(name = "image_url", required = false)
					String imageUrlParam,
			@Parameter(description = "图片分类 ID；可选，缺省/空/\"0\"/非整数按 0（body 同名键优先）", example = "10")
					@RequestParam(name = "image_cat_id", required = false)
					String imageCatIdParam,
			@Parameter(description = "图片简介；可选（body 同名键优先）")
					@RequestParam(name = "brief", required = false)
					String briefParam,
			@Parameter(description = "图片业务类型；可选，长度上限 20；缺省落库默认 item（body 同名键优先）", example = "item")
					@RequestParam(name = "image_type", required = false)
					String imageTypeParam,
			@Parameter(description = "图片完整访问 URL；可选，仅存档（body 同名键优先）")
					@RequestParam(name = "image_full_url", required = false)
					String imageFullUrlParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxImageSaveBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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

	@Operation(
			summary = "开放接口本地存储上传图片",
			description =
					"multipart 直传文件到当前配置的存储驱动（local/oss/qiniu/cosv5/aws 由部署决定），"
							+ " 返回存储对象键 image_url（非完整 URL），不写 upload_images 元数据"
							+ "（登记元数据需另调 ecx.image.save）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.image.upload_localimage, version=1.0）签名校验后转发到本 internal 路径；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 文件 part 名 images 必填非空；filetype 空/\"0\" 按 image，仅支持 image（jpeg/jpg/png/gif）"
							+ " 与 videos（mp4），file 类型拒绝；单文件上限 50MB；Content-Type 须与类型匹配。"
							+ " filetype/group 双通道（body 含键优先）。"
							+ " 副作用：写存储盘一个对象；无写库/异步/第三方 API 调用（对象存储 put 即本接口目的）。",
			operationId = "ecx.image.upload_localimage")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=ok，data 仅含 image_url（对象键）；"
								+ " 业务失败：status=fail, code=E0001（请上传有效图片/不支持的文件存储类型{t}/"
								+ "不支持的图片存储类型{mime}/图片上传最大为50M/上传失败），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxImageUploadLocalFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxImageUploadLocalSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "上传成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"image_url\":\"path/key.jpg\"}}"),
									@ExampleObject(
											name = "fail-empty-file",
											summary = "文件缺失或为空",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"请上传有效图片\",\"data\":null}"),
									@ExampleObject(
											name = "fail-bad-mime",
											summary = "Content-Type 不支持",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"不支持的图片存储类型image/bmp\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.image.upload_localimage", name = "开放接口本地存储上传图片")
	public OpenapiEnvelope uploadeImage(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "图片/视频文件（multipart part，名 images）；必填非空，单文件≤50MB",
							required = true,
							content =
									@Content(
											mediaType = "multipart/form-data",
											schema =
													@Schema(
															type = "string",
															format = "binary",
															description = "文件本体")))
					@RequestParam(name = "images", required = false)
					MultipartFile images,
			@Parameter(
							description =
									"文件类型：image（jpeg/jpg/png/gif）或 videos（mp4）；空/\"0\"/缺省按 image；file 拒绝"
											+ "（body 同名键优先）",
							example = "image")
					@RequestParam(name = "filetype", required = false)
					String filetypeParam,
			@Parameter(description = "存储分组（对象键可选路径段）；可选（body 同名键优先）")
					@RequestParam(name = "group", required = false)
					String groupParam,
			@RequestBody(
							description =
									"multipart 表单字段说明（filetype/group）；文件本体为独立 part images。"
											+ " 同名字段也可经 query 或 JSON body 提供",
							required = false,
							content =
									@Content(
											mediaType = "multipart/form-data",
											schema =
													@Schema(
															implementation =
																	EcxImageUploadLocalFormDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
