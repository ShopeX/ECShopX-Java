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
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，5 键与 query 同名双通道，来源
 * {@code OpenapiMemberV2UpdatePointHandlerParams#mergeAll}（body 含键取原值，
 * 否则 query mergeString trim）。校验与增/减积分见
 * {@code OpenapiThirdApiV2MemberPointUpdateService} + {@code PointMemberAddPointService#addPointForOpenapi}。
 */
@Schema(
		name = "EcxMemberPointUpdateBody",
		description =
				"修改会员积分参数（5 键，body 含键优先否则 query 同名键）。"
						+ " increase_point 与 decrease_point 恰一键存在（键存在性判定，与值无关）；"
						+ " record 必填非空白；external_id 可选标量。"
						+ " 达摩 CRM 积分集成租户走第三方变更（不写本地余额/流水）；"
						+ " 数云开放平台积分启用时以数云端为准跳过本地余额。")
public class EcxMemberPointUpdateBodyDoc {

	@Schema(
			description =
					"会员手机号（明文，定位键）；键必须存在且 trim 后匹配 ^1[3456789][0-9]{9}$"
							+ "（否则 E5001「请求参数错误：会员手机号参数错误」）；查无会员 E5501「会员找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"增加积分；与 decrease_point 恰一键存在（都缺 E5001「增加积分或减去积分二选一必填」，"
							+ "同传 E5001「增加积分和减去积分只能存在一个」）。非负整数：字符串须 ^[0-9]+$ 且 ≤Integer.MAX；"
							+ "数值须整值 ≥0（小数/布尔/null 报错），否则 E5001「请求参数错误：增加积分参数错误」；"
							+ "0 合法（不改余额，仍写流水）；示例值取自 Wire 测试夹具",
			example = "3")
	public String increase_point;

	@Schema(
			description =
					"减去积分；与 increase_point 互斥恰一（同上校验）。非负整数规则同 increase_point"
							+ "（否则 E5001「请求参数错误：减去积分参数错误」）；"
							+ "余额不足或无积分账户（非 0 扣减）报 E0001「积分不足」；示例值取自 Wire 测试夹具",
			example = "3")
	public String decrease_point;

	@Schema(
			description =
					"积分变动原因（备注）；键必须存在且 trim 后非空白"
							+ "（否则 E5001「请求参数错误：积分变动原因（备注）参数错误」）；"
							+ "trim 后写入流水 operater_remark（type=13 开放接口）；示例为格式演示值",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "开放接口调整")
	public String record;

	@Schema(
			description =
					"外部唯一标识；可选，键存在时须标量（字符串/数值；布尔/对象/数组报"
							+ " E5001「请求参数错误：外部唯一标识参数错误」；null 按空串）；写入流水 external_id，不做幂等去重",
			example = "1")
	public String external_id;
}