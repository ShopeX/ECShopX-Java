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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#memberCreate} 经
 * {@code OpenapiRequestParams#originalString} 读取的 key。
 */
@Schema(
		name = "EcxMemberCreateBody",
		description =
				"会员创建参数（body 与 query 同名 key 双通道：body 含键时取原始值，"
						+ "null 视为未提供且不回退 query；body 无该键时读 query 原样，不 trim）")
public class EcxMemberCreateBodyDoc {

	@Schema(
			description =
					"手机号；必填非空（空→「手机号必填」）；已是会员→「当前手机号已经是会员」；"
							+ "本接口不做格式正则校验，落库前 trim 后加密存储",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800000000")
	public String mobile;
}