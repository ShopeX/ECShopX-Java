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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（4 键统计），来源
 * {@code MemberTagRelationInboundPushService}：正常模式逐会员统计；清空模式固定
 * {success_count:1, fail_count:0, processed_members:1, processed_tags:0}。
 */
@Schema(
		name = "EcxMemberTagRelationPushData",
		description = "推送统计（行级失败计入 fail_count，不中断整体，信封恒成功）",
		example = "{\"success_count\":1,\"fail_count\":0,\"processed_members\":1,\"processed_tags\":1}")
public class EcxMemberTagRelationPushDataDoc {

	@Schema(description = "成功处理的会员数（清空模式恒 1）", example = "1")
	public Integer success_count;

	@Schema(description = "失败的会员数（查无会员/系统标签不可编辑等；清空模式恒 0）", example = "0")
	public Integer fail_count;

	@Schema(description = "参与处理的会员分组数（relations 中非空 user_id 去重）", example = "1")
	public Integer processed_members;

	@Schema(description = "成功匹配到的标签关系条数（查无标签不计；清空模式恒 0）", example = "1")
	public Integer processed_tags;
}