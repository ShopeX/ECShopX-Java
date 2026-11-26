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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（8 键统计），来源
 * {@code MemberTagLibraryPushService#newStats} 与处理过程递增。
 */
@Schema(
		name = "EcxMemberTagLibraryPushData",
		description = "推送统计（组级失败计入 fail_count 不中断；deleted_* 为清理的企微来源多余组/标签数）",
		example =
				"{\"success_count\":1,\"fail_count\":0,\"created_groups\":1,\"updated_groups\":0,"
						+ "\"created_tags\":1,\"updated_tags\":0,\"deleted_groups\":0,\"deleted_tags\":0}")
public class EcxMemberTagLibraryPushDataDoc {

	@Schema(description = "成功处理的标签组数", example = "1")
	public Integer success_count;

	@Schema(description = "失败的标签组数（group_name 为空/处理异常）", example = "0")
	public Integer fail_count;

	@Schema(description = "新建标签组数", example = "1")
	public Integer created_groups;

	@Schema(description = "更新标签组数（按 wechat_group_id 命中）", example = "0")
	public Integer updated_groups;

	@Schema(description = "新建标签数", example = "1")
	public Integer created_tags;

	@Schema(description = "更新标签数（按 wechat_tag_id 命中）", example = "0")
	public Integer updated_tags;

	@Schema(description = "删除的多余企微标签组数（本次推送未包含的 wechat_group_id 非空组，连带组关系）", example = "0")
	public Integer deleted_groups;

	@Schema(description = "删除的多余企微标签数（连带组关系与会员打标关系）", example = "0")
	public Integer deleted_tags;
}