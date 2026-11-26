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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），两种形态：
 * <ul>
 *   <li>E0001（{@code ResourceException}）、data=null：scope参数错误，应为fp或customer / 页码必须为正整数 /
 *       每页数量必须在1-500之间 / 生日开始(结束)日期格式错误，应为YYYY-MM-DD / 标签ID必须为数组 /
 *       标签ID数组中的每个元素必须为整数 / 分配状态类型错误，应为noassign或assign / 导购编号必须为字符串 /
 *       门店编号必须为字符串 / 积分开始(结束)值必须为非负整数 / 等级ID必须为正整数 /
 *       购买金额开始(结束)值必须为非负整数 / 关键词（用于按手机号查询）/ scope为customer时，salesperson_code必填。</li>
 *   <li>E9999（{@code OpenapiMemberListCustomerUnionidsFailException}）、data=[]：
 *       customer 范围系统异常「获取导购客户unionid列表失败：{异常信息}」。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」、data=null（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxMemberListFpFailEnvelope")
public class EcxMemberListFpFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 参数校验失败；E9999 customer 系统异常；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "scope参数错误，应为fp或customer")
	public String message;

	@ArraySchema(
			arraySchema = @Schema(description = "E0001/鉴权失败为 null；E9999 时为空数组 []"),
			schema = @Schema(nullable = true))
	public List<Object> data;
}