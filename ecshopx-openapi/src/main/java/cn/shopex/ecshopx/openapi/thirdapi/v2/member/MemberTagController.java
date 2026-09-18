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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping(value = "/ecx.member.tag.add", name = "开放接口新增会员标签")
	public Map<String, Object> createTag(
			HttpServletRequest request,
			@RequestParam(name = "tag_name", required = false) String tagNameParam,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@RequestParam(name = "description", required = false) String descriptionParam,
			@RequestParam(name = "tag_color", required = false) String tagColorParam,
			@RequestParam(name = "font_color", required = false) String fontColorParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String tagNameRaw = OpenapiRequestParams.mergeString(tagNameParam, body, "tag_name");
		String categoryIdRaw = OpenapiRequestParams.mergeString(categoryIdParam, body, "category_id");
		String descriptionRaw = OpenapiRequestParams.mergeString(descriptionParam, body, "description");
		String tagColorRaw = OpenapiRequestParams.mergeString(tagColorParam, body, "tag_color");
		String fontColorRaw = OpenapiRequestParams.mergeString(fontColorParam, body, "font_color");
		return memberTagCreatePort.createTag(
				companyId, tagNameRaw, categoryIdRaw, descriptionRaw, tagColorRaw, fontColorRaw);
	}

	@PostMapping(value = "/ecx.member.tag.update", name = "开放接口修改会员标签")
	public OpenapiEnvelope updateTag(
			HttpServletRequest request,
			@RequestParam(name = "tag_id", required = false) String tagIdParam,
			@RequestParam(name = "tag_name", required = false) String tagNameParam,
			@RequestParam(name = "description", required = false) String descriptionParam,
			@RequestParam(name = "tag_color", required = false) String tagColorParam,
			@RequestParam(name = "font_color", required = false) String fontColorParam,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@DeleteMapping(value = "/ecx.member.tagged.delete", name = "开放接口删除会员已打标签")
	public Map<String, Object> deleteMemberTagged(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "tag_ids", required = false) String tagIdsParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String tagIdsRaw = OpenapiRequestParams.mergeString(tagIdsParam, body, "tag_ids");
		return memberTagTaggedDeletePort.deleteMemberTagged(companyId, mobileRaw, tagIdsRaw);
	}

	@DeleteMapping(value = "/ecx.member.tag.delete", name = "开放接口删除会员标签")
	public Map<String, Object> deleteTag(
			HttpServletRequest request,
			@RequestParam(name = "tag_id", required = false) String tagIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String tagIdRaw = OpenapiRequestParams.mergeString(tagIdParam, body, "tag_id");
		return memberTagDeletePort.deleteTag(companyId, tagIdRaw);
	}
}
