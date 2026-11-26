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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E5001「分类ID必填」/「分类名称必填」；
 * E5571「该标签分类不存在」（非整数/查无/更新后回读为空）；
 * E5572「该分类名已存在」（同名排除自身冲突）。
 * sort 非法值不报错（PHP intval 归 0；空/\"0\" 保留原值）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberTagCategoryUpdateFailEnvelope")
public class EcxMemberTagCategoryUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 分类ID/名称必填；E5571 该标签分类不存在；E5572 该分类名已存在；E4004 未授权",
			example = "E5571")
	public String code;

	@Schema(description = "错误说明", example = "该标签分类不存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}