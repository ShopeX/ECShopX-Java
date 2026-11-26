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
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，3 键与 query 同名双通道，来源
 * {@code OpenapiMemberV2UpdateMobileHandlerParams#mergeAll}（body 含键取原值，
 * 否则 query mergeString trim）。校验与更新见
 * {@code OpenapiThirdApiV2MemberUpdateMobileService}。
 */
@Schema(
		name = "EcxMemberMobileUpdateBody",
		description =
				"修改会员手机号参数（3 键，body 含键优先否则 query 同名键）。"
						+ " mobile 定位旧会员、new_mobile 为新号；user_id 被接收但忽略。"
						+ " 新号被其他会员占用报错；同号重设幂等成功（不写操作日志）。")
public class EcxMemberMobileUpdateBodyDoc {

	@Schema(
			description = "遗留参数：被接收合并但服务不消费（定位仅按 mobile）",
			deprecated = true)
	public String user_id;

	@Schema(
			description =
					"当前会员手机号（明文，定位键）；必填，trim 后须 ^1[3456789][0-9]{9}$；"
							+ "缺失/空白/格式错统一报 E5502「会员手机号已存在」（PHP 旧契约文案，实为格式校验）；"
							+ "查无会员 E5501「会员找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"新手机号（明文）；必填：null/空白串/\"0\"/false/数值 0 报 E5001「会员新手机号参数错误」；"
							+ "归一化（整型数值转长整字符串、trim）后须 ^1[3456789][0-9]{9}$"
							+ "（否则 E5001「新手机号填写错误」）；被其他会员占用报 E5502「会员新手机号已存在」"
							+ "（占用者即本人放行，同号重设幂等成功且不写操作日志）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800000000")
	public String new_mobile;
}