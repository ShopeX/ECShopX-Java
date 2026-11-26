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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，2 键与 query 同名双通道
 * （{@code OpenapiRequestParams#mergeString}：body 含键优先、值 null 视为未传、trim；
 * body 数组字符串化后 JSON 解析），校验与覆盖见
 * {@code OpenapiThirdApiV2MemberTagBatchCoverService}。
 */
@Schema(
		name = "EcxMemberTagBatchCoverBody",
		description =
				"批量覆盖会员标签参数（2 键，body 含键优先否则 query 同名键）。"
						+ " 覆盖语义：逐会员先删除全部非系统标签关联，再打上 tag_ids 指定标签"
						+ "（tag_ids=[] 时清空非系统标签；系统标签关联保留不动）。"
						+ " 任一手机号/标签查无即整体报错（逐会员事务，已处理会员不回滚）。")
public class EcxMemberTagBatchCoverBodyDoc {

	@Schema(
			description =
					"会员手机号数组（JSON 数组或 JSON 数组字符串，body 可直接传数组）；必填非空白"
							+ "（E5001「会员手机号必填」）；须 JSON 数组且元素标量（嵌套报 E5560「会员手机号格式错误」）；"
							+ "元素原样字符串化与 members.mobile（加密存储列）IN 匹配（无加密转换，"
							+ "加密启用部署下通常查无）；**任一查无报 E5501「{手机号}会员不存在」**；"
							+ "空数组 [] 合法（无会员可处理，直接成功）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "[\"13800138000\"]")
	public String mobiles;

	@Schema(
			description =
					"标签 ID 数组（JSON 数组或 JSON 数组字符串）；必填非空白且须 JSON 数组"
							+ "（否则 E5560「会员标签ID格式错误」——本键缺失也报格式错误而非必填）；"
							+ "元素标量（整数/数字字符串/布尔/null 均字符串化宽松匹配）；"
							+ "**任一标签查无报 E5561「{标签ID}标签不存在」**；含系统标签报 E0001「系统标签不可编辑」；"
							+ "**空数组 [] 合法：清空各会员全部非系统标签**",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "[1]")
	public String tag_ids;
}