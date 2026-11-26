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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，4 键与 query 同名双通道：
 * page/page_size/grade_id 走 {@code OpenapiRequestParams#originalString}（不 trim），
 * country_code 走 {@code mergeString}（trim）；body 含键优先、值 null 视为未传。
 * 分页解析复用 {@code OpenapiThirdApiV2ShippingTemplatesListParams#resolve}
 * （失败走 ResourceException → E0001），过滤与多语言见
 * {@code OpenapiThirdApiV2MemberCardGradeListService}。
 */
@Schema(
		name = "EcxMemberCardGradeListBody",
		description =
				"会员卡等级列表参数（4 键，body 含键优先否则 query 同名键）。"
						+ " 按租户查询 membercard_grade；无显式排序（库默认行序）。")
public class EcxMemberCardGradeListBodyDoc {

	@Schema(
			description =
					"页码；默认 1。PHP 式解析（空取默认、纯空白/非法→0、小数截断取整），再取 max(…,1)；"
							+ "「当前页面最小值为1」分支不可达",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。PHP 式解析后原始值 ≥500 报 E0001「每页的大小最大为500！」（500 本身也报错；"
							+ "本接口分页失败走 ResourceException 通道，码为 E0001 而非 E5002），"
							+ "再取 max(…,20)，实际生效范围 [20,499]；「每页显示数量1-500」分支不可达",
			example = "20")
	public String page_size;

	@Schema(
			description =
					"等级 ID 过滤；PHP isset 非空语义（null/空串忽略，\"0\" 参与过滤）："
							+ "trim 后可 parseLong → grade_id 等值；非数字 → 按 grade_id=-1 过滤（恒空结果，不报错）",
			example = "1")
	public String grade_id;

	@Schema(
			description =
					"多语言代码（如 zh-CN/en-US）；mergeString trim，空白默认 zh-CN；"
							+ "按对应语言分表覆盖 grade_name/description/background_pic_url/grade_background"
							+ "（分表缺失/查询异常静默跳过覆盖）",
			example = "zh-CN")
	public String country_code;
}