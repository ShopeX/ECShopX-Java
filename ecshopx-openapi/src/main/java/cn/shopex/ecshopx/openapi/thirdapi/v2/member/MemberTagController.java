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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagBatchCoverPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagBatchUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagTaggedDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagTaggedGetPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagMembersGetPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagAddBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagAddSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagDeleteSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTagUpdateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTaggedDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberTaggedDeleteSuccessEnvelopeDoc;
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
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-会员标签V2")
@OpenapiResponse
@RestController("openapiV2MemberTag")
@RequestMapping("/api/openapi/internal/v2")
public class MemberTagController extends OpenapiBaseController {

	private final OpenapiMemberTagCreatePort memberTagCreatePort;
	private final OpenapiMemberTagDeletePort memberTagDeletePort;
	private final OpenapiMemberTagUpdatePort memberTagUpdatePort;
	private final OpenapiMemberTagListPort memberTagListPort;
	private final OpenapiMemberTagBatchCoverPort memberTagBatchCoverPort;
	private final OpenapiMemberTagBatchUpdatePort memberTagBatchUpdatePort;
	private final OpenapiMemberTagTaggedDeletePort memberTagTaggedDeletePort;
	private final OpenapiMemberTagTaggedGetPort memberTagTaggedGetPort;
	private final OpenapiMemberTagMembersGetPort memberTagMembersGetPort;

	public MemberTagController(
			OpenapiMemberTagCreatePort memberTagCreatePort,
			OpenapiMemberTagDeletePort memberTagDeletePort,
			OpenapiMemberTagUpdatePort memberTagUpdatePort,
			OpenapiMemberTagListPort memberTagListPort,
			OpenapiMemberTagBatchCoverPort memberTagBatchCoverPort,
			OpenapiMemberTagBatchUpdatePort memberTagBatchUpdatePort,
			OpenapiMemberTagTaggedDeletePort memberTagTaggedDeletePort,
			OpenapiMemberTagTaggedGetPort memberTagTaggedGetPort,
			OpenapiMemberTagMembersGetPort memberTagMembersGetPort) {
		this.memberTagCreatePort = memberTagCreatePort;
		this.memberTagDeletePort = memberTagDeletePort;
		this.memberTagUpdatePort = memberTagUpdatePort;
		this.memberTagListPort = memberTagListPort;
		this.memberTagBatchCoverPort = memberTagBatchCoverPort;
		this.memberTagBatchUpdatePort = memberTagBatchUpdatePort;
		this.memberTagTaggedDeletePort = memberTagTaggedDeletePort;
		this.memberTagTaggedGetPort = memberTagTaggedGetPort;
		this.memberTagMembersGetPort = memberTagMembersGetPort;
	}

	@Operation(
			summary = "开放接口新增会员标签",
			description =
					"按签名鉴权租户 company_id 新增会员标签（5 键 mergeString 双通道 trim，"
							+ " body 含键优先、值 null 视为未传）："
							+ " tag_name 必填非空白（E5001「分类名称必填」——源码文案如此，实为标签名；"
							+ " 同租户同名已存在 E5562「该标签名已存在」）；"
							+ " category_id 宽松 parseInt（非法按 0，不校验分类存在）；"
							+ " description 空白落库 null；tag_color/font_color PHP elvis："
							+ " null/空白/\"0\" 取默认 rgba(255, 25, 57, 1)/rgba(255, 255, 255, 1)，"
							+ " 其他值原样落库不校验格式。"
							+ " 成功 data 8 键：tag_id/tag_name/category_id/description(nullable)/tag_color/"
							+ " font_color/created/updated 格式化时间。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.tag.add,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：INSERT members_tags 单行（无显式事务）+ zh-CN 多语言分表 INSERT"
							+ "（失败降级 upsert）；无异步、无第三方调用。",
			operationId = "ecx.member.tag.add.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberTagAddDataDoc；"
								+ " 业务失败：status=fail, code=E5001 分类名称必填 / E5562 该标签名已存在，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTagAddFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTagAddSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "新增成功（默认色；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"tag_id\":1,\"tag_name\":\"聚合测试类目\","
															+ "\"category_id\":0,\"description\":null,"
															+ "\"tag_color\":\"rgba(255, 25, 57, 1)\","
															+ "\"font_color\":\"rgba(255, 255, 255, 1)\","
															+ "\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-tag-exist",
											summary = "同名标签已存在",
											value =
													"{\"status\":\"fail\",\"code\":\"E5562\","
															+ "\"message\":\"该标签名已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-name-missing",
											summary = "tag_name 缺失/空白（源码文案「分类名称必填」）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"分类名称必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.tag.add", name = "开放接口新增会员标签")
	public Map<String, Object> createTag(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "标签名；必填非空白（E5001「分类名称必填」）；同名已存在 E5562",
							required = true,
							example = "聚合测试类目")
					@RequestParam(name = "tag_name", required = false)
					String tagNameParam,
			@Parameter(
							description = "标签分类 ID；宽松整数（非法按 0）；不校验分类存在",
							example = "0")
					@RequestParam(name = "category_id", required = false)
					String categoryIdParam,
			@Parameter(description = "标签描述；空白落库 null")
					@RequestParam(name = "description", required = false)
					String descriptionParam,
			@Parameter(
							description = "标签背景色；空/\"0\" 取默认 rgba(255, 25, 57, 1)；不校验格式",
							example = "rgba(255, 25, 57, 1)")
					@RequestParam(name = "tag_color", required = false)
					String tagColorParam,
			@Parameter(
							description = "标签字体色；空/\"0\" 取默认 rgba(255, 255, 255, 1)",
							example = "rgba(255, 255, 255, 1)")
					@RequestParam(name = "font_color", required = false)
					String fontColorParam,
			@RequestBody(
							description = "可选 JSON body；5 键与 query 同名双通道（mergeString trim，body 含键优先）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTagAddBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String tagNameRaw = OpenapiRequestParams.mergeString(tagNameParam, body, "tag_name");
		String categoryIdRaw = OpenapiRequestParams.mergeString(categoryIdParam, body, "category_id");
		String descriptionRaw = OpenapiRequestParams.mergeString(descriptionParam, body, "description");
		String tagColorRaw = OpenapiRequestParams.mergeString(tagColorParam, body, "tag_color");
		String fontColorRaw = OpenapiRequestParams.mergeString(fontColorParam, body, "font_color");
		return memberTagCreatePort.createTag(
				companyId, tagNameRaw, categoryIdRaw, descriptionRaw, tagColorRaw, fontColorRaw);
	}

	@Operation(
			summary = "开放接口修改会员标签",
			description =
					"按签名鉴权租户 company_id 修改会员标签（6 键双通道，body 含键优先）："
							+ " tag_id/tag_name/tag_color/font_color 必填非空白"
							+ "（E5001「标签ID必填」/「标签名称必填」/「标签颜色必填」/「标签字体颜色必填」）；"
							+ " tag_id trim+parseLong（非整数/查无/回读为空 E5561「该标签不存在」）；"
							+ " 系统标签禁改（E0001「系统标签不可编辑」）；同名排除自身冲突 E5562「该标签名已存在」。"
							+ " **description 恒写：键缺席也清空为 null**（originalString 不 trim + 存在性判定，"
							+ " 非保留原值语义）；category_id 键存在且 PHP truthy（非空非\"0\"）才更新"
							+ "（PHP intval 前导数字截断，如 \"12abc\"→12），否则保留原分类。"
							+ " 颜色 trim 原样落库（无默认色，区别于新增）。"
							+ " **成功信封为 Handler 显式构造：message 固定「操作成功」**（非 Advice 默认「成功」）；"
							+ " data 为更新后回读 8 键行（同新增格式）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.tag.update,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional UPDATE members_tags（tag_name/description/tag_color/"
							+ " font_color/updated + 条件 category_id）+ zh-CN 分表 upsert；无异步、无第三方调用。",
			operationId = "ecx.member.tag.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Handler 显式信封）。"
								+ " 成功：status=success, code=E0000, **message=操作成功**，data 为更新后 8 键行；"
								+ " 业务失败：status=fail, code=E5001 各必填 / E5561 该标签不存在 /"
								+ " E5562 该标签名已存在 / E0001 系统标签不可编辑，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTagUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTagUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "修改成功（message 为「操作成功」；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"tag_id\":1,\"tag_name\":\"聚合测试类目\","
															+ "\"category_id\":0,\"description\":null,"
															+ "\"tag_color\":\"rgba(255, 25, 57, 1)\","
															+ "\"font_color\":\"rgba(255, 255, 255, 1)\","
															+ "\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "标签查无/ID 非整数",
											value =
													"{\"status\":\"fail\",\"code\":\"E5561\","
															+ "\"message\":\"该标签不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-tag-exist",
											summary = "同名标签冲突（排除自身）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5562\","
															+ "\"message\":\"该标签名已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-system-tag",
											summary = "系统标签禁改",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"系统标签不可编辑\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.tag.update", name = "开放接口修改会员标签")
	public OpenapiEnvelope updateTag(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "标签 ID；必填（E5001）；非整数/查无 E5561；系统标签 E0001",
							required = true,
							example = "1")
					@RequestParam(name = "tag_id", required = false)
					String tagIdParam,
			@Parameter(
							description = "标签名；必填（E5001「标签名称必填」）；同名冲突 E5562",
							required = true,
							example = "聚合测试类目")
					@RequestParam(name = "tag_name", required = false)
					String tagNameParam,
			@Parameter(
							description = "标签描述；恒写——键缺席也清空为 null（非保留原值）")
					@RequestParam(name = "description", required = false)
					String descriptionParam,
			@Parameter(
							description = "标签背景色；必填（E5001「标签颜色必填」）；无默认色",
							required = true,
							example = "rgba(255, 25, 57, 1)")
					@RequestParam(name = "tag_color", required = false)
					String tagColorParam,
			@Parameter(
							description = "标签字体色；必填（E5001「标签字体颜色必填」）",
							required = true,
							example = "rgba(255, 255, 255, 1)")
					@RequestParam(name = "font_color", required = false)
					String fontColorParam,
			@Parameter(
							description = "标签分类 ID；键存在且非空非\"0\"才更新（PHP intval 前导数字）；否则保留原分类",
							example = "1")
					@RequestParam(name = "category_id", required = false)
					String categoryIdParam,
			@RequestBody(
							description =
									"可选 JSON body；6 键与 query 同名双通道（body 含键优先）；"
											+ "description 恒写清空语义与 category_id 条件更新见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTagUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String tagIdRaw = OpenapiRequestParams.mergeString(tagIdParam, body, "tag_id");
		String tagNameRaw = OpenapiRequestParams.mergeString(tagNameParam, body, "tag_name");
		String descriptionRaw = OpenapiRequestParams.originalString(descriptionParam, body, "description");
		boolean descriptionParamPresent =
				(body != null && body.containsKey("description")) || descriptionParam != null;
		String tagColorRaw = OpenapiRequestParams.mergeString(tagColorParam, body, "tag_color");
		String fontColorRaw = OpenapiRequestParams.mergeString(fontColorParam, body, "font_color");
		String categoryIdRaw = OpenapiRequestParams.mergeString(categoryIdParam, body, "category_id");
		boolean categoryIdParamPresent =
				(body != null && body.containsKey("category_id")) || categoryIdParam != null;
		Map<String, Object> data = memberTagUpdatePort.updateTag(
				companyId, tagIdRaw, tagNameRaw, descriptionRaw, descriptionParamPresent,
				tagColorRaw, fontColorRaw, categoryIdRaw, categoryIdParamPresent);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@GetMapping(value = "/ecx.member.tags.get", name = "开放接口查询会员标签列表")
	public Map<String, Object> getTagsList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "tag_name", required = false) String tagNameParam,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2MemberTagsListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberTagsListParams.resolve(pageParam, pageSizeParam, body);
		String tagNameRaw = OpenapiRequestParams.originalString(tagNameParam, body, "tag_name");
		String langRaw = OpenapiRequestParams.mergeString(countryCodeParam, body, "country_code");
		return memberTagListPort.getTagsList(
				companyId, pageSpec.page(), pageSpec.pageSize(), tagNameRaw, langRaw);
	}

	@PostMapping(value = "/ecx.member.tagging.batch.cover", name = "开放接口批量覆盖会员标签")
	public Map<String, Object> batchCoverMemberTags(
			HttpServletRequest request,
			@RequestParam(name = "mobiles", required = false) String mobilesParam,
			@RequestParam(name = "tag_ids", required = false) String tagIdsParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobilesRaw = OpenapiRequestParams.mergeString(mobilesParam, body, "mobiles");
		String tagIdsRaw = OpenapiRequestParams.mergeString(tagIdsParam, body, "tag_ids");
		return memberTagBatchCoverPort.batchCoverMemberTags(companyId, mobilesRaw, tagIdsRaw);
	}

	@PostMapping(value = "/ecx.member.tagging.batch.update", name = "开放接口批量增量打标")
	public Map<String, Object> batchUpdateMemberTags(
			HttpServletRequest request,
			@RequestParam(name = "mobiles", required = false) String mobilesParam,
			@RequestParam(name = "tag_ids", required = false) String tagIdsParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobilesRaw = OpenapiRequestParams.mergeString(mobilesParam, body, "mobiles");
		String tagIdsRaw = OpenapiRequestParams.mergeString(tagIdsParam, body, "tag_ids");
		return memberTagBatchUpdatePort.batchUpdateMemberTags(companyId, mobilesRaw, tagIdsRaw);
	}

	@GetMapping(value = "/ecx.tag.members.get", name = "开放接口查询标签关联会员列表")
	public Map<String, Object> getTagMembers(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "tag_id", required = false) String tagIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2MemberTagsListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberTagsListParams.resolve(pageParam, pageSizeParam, body);
		String tagIdRaw = OpenapiRequestParams.mergeString(tagIdParam, body, "tag_id");
		return memberTagMembersGetPort.getTagMembers(
				companyId, pageSpec.page(), pageSpec.pageSize(), tagIdRaw);
	}

	@GetMapping(value = "/ecx.member.tagged.get", name = "开放接口查询会员已打标签")
	public Object getMemberTagged(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		return memberTagTaggedGetPort.getMemberTagged(companyId, mobileRaw);
	}

	@Operation(
			summary = "开放接口删除会员已打标签",
			description =
					"按签名鉴权租户 company_id 解除会员与指定标签的关联（2 键均必填，mergeString 双通道 trim，"
							+ " body 含键优先、值 null 视为未传）："
							+ " mobile 缺失 E5001「会员手机号必填」；tag_ids 缺失 E5001「会员标签ID必填」。"
							+ " tag_ids 须 JSON 数组（body 数组字符串化后解析），元素标量整数/整数字符串"
							+ "（嵌套/非整数/空串元素 E5560「会员标签ID格式错误」）；≤0 元素静默剔除，"
							+ " 剔除后为空 → 无操作成功（不校验会员）；含系统标签 E0001「系统标签不可编辑」。"
							+ " **mobile trim 明文与 members.mobile（加密存储列）直接等值，无加密转换**——"
							+ " 加密启用部署下通常查无 E5501「会员找不到」（源码差异如实记录）。"
							+ " 命中后 userRelTagDeleteNoPush 解除关联并递减标签 self_tag_count，不推送导购平台。"
							+ " 成功 data={status:true}（布尔）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.tagged.delete,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional DELETE members_rel_tags 关联行 + UPDATE members_tags"
							+ ".self_tag_count 递减；无异步、无第三方推送。",
			operationId = "ecx.member.tagged.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}"
								+ "（含 tag_ids 剔除后为空的无操作场景）；"
								+ " 业务失败：status=fail, code=E5001 会员手机号必填·会员标签ID必填 /"
								+ " E5560 会员标签ID格式错误 / E0001 系统标签不可编辑 / E5501 会员找不到，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTaggedDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTaggedDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "解除关联成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-tag-ids-format",
											summary = "tag_ids 非数组/元素非法",
											value =
													"{\"status\":\"fail\",\"code\":\"E5560\","
															+ "\"message\":\"会员标签ID格式错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-system-tag",
											summary = "请求含系统标签",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"系统标签不可编辑\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5501\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping(value = "/ecx.member.tagged.delete", name = "开放接口删除会员已打标签")
	public Map<String, Object> deleteMemberTagged(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号；必填（E5001）；trim 明文直接等值（无加密转换）；查无 E5501",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "标签 ID JSON 数组；必填（E5001）；格式错 E5560；≤0 剔除；系统标签 E0001",
							required = true,
							example = "[1]")
					@RequestParam(name = "tag_ids", required = false)
					String tagIdsParam,
			@RequestBody(
							description = "可选 JSON body；2 键与 query 同名双通道（body 含键优先，tag_ids 可直接传数组）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTaggedDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String tagIdsRaw = OpenapiRequestParams.mergeString(tagIdsParam, body, "tag_ids");
		return memberTagTaggedDeletePort.deleteMemberTagged(companyId, mobileRaw, tagIdsRaw);
	}

	@Operation(
			summary = "开放接口删除会员标签",
			description =
					"按签名鉴权租户 company_id 删除会员标签：tag_id 必填非空白"
							+ "（mergeString 双通道 trim，body 含键优先、值 null 视为未传；缺失 E5001「标签ID必填」）。"
							+ " trim 后须可 parseLong（非整数按查无处理），company_id+tag_id 定位 members_tags"
							+ "（LIMIT 1），非整数/查无均 E5561「该标签不存在」。"
							+ " **系统标签禁删**（tag_kind 系统类 → ResourceException E0001「系统标签不可编辑」）。"
							+ " 删除全部语言分表行（zhCN/enCN/arSA/zhTW/jaJP/ruRU）+ members_tags 行；"
							+ " **不清理会员关联 members_rel_tags**（关联行残留，如实记录）。"
							+ " 成功 data={status:true}（布尔）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.tag.delete,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional DELETE members_tags 单行 + 6 语言分表 DELETE；"
							+ " 无异步、无第三方调用。",
			operationId = "ecx.member.tag.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 标签ID必填 / E5561 该标签不存在 /"
								+ " E0001 系统标签不可编辑，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberTagDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberTagDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "标签查无/ID 非整数",
											value =
													"{\"status\":\"fail\",\"code\":\"E5561\","
															+ "\"message\":\"该标签不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-system-tag",
											summary = "系统标签禁删",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"系统标签不可编辑\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping(value = "/ecx.member.tag.delete", name = "开放接口删除会员标签")
	public Map<String, Object> deleteTag(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "标签 ID；必填（E5001）；非整数/查无 E5561；系统标签 E0001「系统标签不可编辑」",
							required = true,
							example = "1")
					@RequestParam(name = "tag_id", required = false)
					String tagIdParam,
			@RequestBody(
							description = "可选 JSON body；tag_id 与 query 同名双通道（mergeString trim，body 含键优先）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberTagDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String tagIdRaw = OpenapiRequestParams.mergeString(tagIdParam, body, "tag_id");
		return memberTagDeletePort.deleteTag(companyId, tagIdRaw);
	}
}
