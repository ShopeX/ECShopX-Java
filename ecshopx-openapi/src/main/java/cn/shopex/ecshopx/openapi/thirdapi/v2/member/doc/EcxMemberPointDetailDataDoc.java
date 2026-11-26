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
 * 仅 OpenAPI 文档；data 单键对象，来源
 * {@code OpenapiThirdApiV2MemberPointDetailService#executeOpenapiDetail}：
 * point_member 按 (company_id, user_id) LIMIT 1 读 point；
 * 会员查无/无 point_member 行/point 为 null 均返回 0（不报错）。
 */
@Schema(name = "EcxMemberPointDetailData")
public class EcxMemberPointDetailDataDoc {

	@Schema(description = "可用积分（整型；会员查无/无积分账户/point null 均为 0）", example = "10")
	public Integer point;
}