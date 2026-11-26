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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null（同步校验失败整批拒绝，未入队）：
 * E5500「批量创建最多支持50个！」（>50 条）；
 * E5502「会员手机号已存在」（mobile 格式错——PHP 旧契约文案，实为格式校验）；
 * E5001：手机号必填 / 来源渠道填写错误 / 推荐人手机号填写错误 / 绑定导购手机号填写错误 /
 * 微信unionid填写错误 / 会员状态填写错误 / 性别填写错误 / 生日填写错误 / 爱好填写错误 /
 * 学历填写错误 / 年收入填写错误 / 行业填写错误 / email填写错误 / 会员等级ID填写错误；
 * E5000「系统错误」（校验阶段其他异常兜底）。
 * 异步 job 内失败（占用 E5502/推荐人 E5511/导购 E5601·E5612/等级 E5531/卡号 E5522 等）
 * 不影响本接口同步响应，需经日志/会员查询接口观测。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberBatchCreateFailEnvelope")
public class EcxMemberBatchCreateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5500 超 50 条；E5502 手机号格式（文案「已存在」）；E5001 各字段校验；E5000 系统错误；E4004 未授权",
			example = "E5500")
	public String code;

	@Schema(description = "错误说明", example = "批量创建最多支持50个！")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}