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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1MemberMemberCreateService#successData}（mobile 入参原样回显 + 新会员 uid）。
 */
@Schema(name = "EcxMemberCreateData", description = "创建结果", example = "{\"mobile\":\"13800000000\",\"uid\":5}")
public class EcxMemberCreateDataDoc {

	@Schema(description = "手机号（入参原样回显，未 trim）", example = "13800000000")
	public String mobile;

	@Schema(description = "新会员 ID（members.user_id 自增）", example = "5")
	public Long uid;
}