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
 * 仅 OpenAPI 文档；DELETE 请求可选 JSON body，2 键与 query 同名双通道
 * （{@code OpenapiRequestParams#mergeString}：body 含键优先、值 null 视为未传、trim；
 * body 数组会被字符串化后 JSON 解析），校验与删除见
 * {@code OpenapiThirdApiV2MemberTaggedDeleteService}。
 */
@Schema(
		name = "EcxMemberTaggedDeleteBody",
		description =
				"删除会员已打标签参数（2 键均必填，body 含键优先否则 query 同名键）。"
						+ " 按 mobile 定位会员后解除指定标签关联；系统标签禁操作；不推送导购平台。"
						+ " 注意：mobile 以 trim 明文与 members.mobile（加密存储列）直接等值——"
						+ "加密启用时通常查无报 E5501（源码未做加密转换，如实记录）。")
public class EcxMemberTaggedDeleteBodyDoc {

	@Schema(
			description =
					"会员手机号（明文）；必填非空白（E5001「会员手机号必填」）；trim 后与 members.mobile"
							+ " 直接等值（无格式校验、无加密转换——加密存储部署下通常查无）；查无 E5501「会员找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"标签 ID 数组（JSON 数组或 JSON 数组字符串，body 可直接传数组）；必填非空白"
							+ "（E5001「会员标签ID必填」）；须 JSON 数组且元素为标量整数/整数字符串"
							+ "（对象/数组元素/非整数/空串元素报 E5560「会员标签ID格式错误」）；"
							+ "≤0 的元素静默剔除；剔除后为空 → 无操作成功；含系统标签报 E0001「系统标签不可编辑」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "[1]")
	public String tag_ids;
}