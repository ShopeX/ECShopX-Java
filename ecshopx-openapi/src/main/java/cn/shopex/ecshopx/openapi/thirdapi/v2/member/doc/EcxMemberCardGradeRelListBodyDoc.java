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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，4 键与 query 同名双通道
 * （{@code OpenapiRequestParams#originalString}：body 含键优先、值 null 视为未传、不 trim；
 * grade_id/vip_grade_id 另按键存在性判定 present），分页解析见
 * {@code OpenapiThirdApiV2MemberListParams#resolve}，分支校验见
 * {@code OpenapiThirdApiV2MemberCardGradeRelListService#validateAndResolveBranch}。
 */
@Schema(
		name = "EcxMemberCardGradeRelListBody",
		description =
				"会员等级关联会员列表参数（4 键，body 含键优先否则 query 同名键）。"
						+ " grade_id 与 vip_grade_id 恰一键存在（键存在性判定，与值无关）："
						+ " 都缺 E5001「会员等级ID参数错误」；同传 E5001「会员等级ID与付费会员等级ID不能同时存在」。")
public class EcxMemberCardGradeRelListBodyDoc {

	@Schema(
			description =
					"页码；默认 1。PHP 式解析（空取默认、纯空白/非法→0、小数截断取整），再取 max(…,1)，"
							+ "负数/0 最终按 1 处理",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。PHP 式解析后原始值 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错），"
							+ "再取 max(…,20)，实际生效范围 [20,499]",
			example = "20")
	public String page_size;

	@Schema(
			description =
					"普通会员等级 ID；与 vip_grade_id 互斥恰一。键存在即走 members.grade_id 等值分支；"
							+ "值须严格整数 ^-?\\d+$（允许负数；空串/小数/非数字报 E5001「会员等级ID参数错误」）；"
							+ "不校验等级存在，查无返回空列表",
			example = "1")
	public String grade_id;

	@Schema(
			description =
					"付费会员等级 ID；与 grade_id 互斥恰一。键存在即走 vip_grade_rel_user 关联分支"
							+ "（company_id+vip_grade_id 等值，不过滤 end_date——已过期关联也计入；"
							+ " total_count 为关联行数非去重会员数，同一会员多行时 list 可重复）；"
							+ "值须严格整数 ^-?\\d+$（否则 E5001「付费会员等级ID参数错误」）；不校验等级存在",
			example = "2")
	public String vip_grade_id;
}