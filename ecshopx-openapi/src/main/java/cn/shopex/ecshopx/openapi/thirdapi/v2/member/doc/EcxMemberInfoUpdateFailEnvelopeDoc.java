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
 * E5502「会员手机号已存在」（mobile 缺失/空白/格式错——PHP 旧契约文案，实为格式校验）；
 * E5001：推荐人手机号填写错误 / 会员状态填写错误 / 备注填写错误 / 姓名·昵称填写错误 / 头像url填写错误 /
 * 地址填写错误 / 性别填写错误 / 生日填写错误 / 生日格式填写错误（含 birthday 缺失）/
 * 学历填写错误 / 年收入填写错误 / 行业填写错误 / email填写错误 / 爱好填写错误；
 * E5501「会员找不到」（mobile 查无 / members_info 行缺失 / 更新后回读为空）；
 * E5511「会员的推荐人找不到」；
 * E5000「系统错误」（其他异常统一兜底，事务回滚）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberInfoUpdateFailEnvelope")
public class EcxMemberInfoUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5502 手机号格式（文案「已存在」）；E5001 各字段校验；E5501 会员找不到；E5511 推荐人找不到；E5000 系统错误；E4004 未授权",
			example = "E5502")
	public String code;

	@Schema(description = "错误说明", example = "会员手机号已存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}