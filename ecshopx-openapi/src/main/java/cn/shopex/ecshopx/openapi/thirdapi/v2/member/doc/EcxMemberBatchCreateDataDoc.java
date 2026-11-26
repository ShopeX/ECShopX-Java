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
 * 仅 OpenAPI 文档；data 结果（单键 status），来源
 * {@code OpenapiThirdApiV2MemberBatchCreateService#batchCreate}：
 * data 键缺席 → status=0；键存在且校验通过全部入队 → status=1
 * （含解析失败/空数组的 0 条入队场景，仍 status=1）。
 */
@Schema(name = "EcxMemberBatchCreateData")
public class EcxMemberBatchCreateDataDoc {

	@Schema(
			description = "受理状态（整型）：0=data 键缺席未受理；1=已受理入队（异步创建，条数可为 0）",
			example = "1")
	public Integer status;
}