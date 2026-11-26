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
 * mobile/order_id 另按键存在性判定 present），分页解析见
 * {@code OpenapiThirdApiV2MemberListParams#resolve}（E5002），过滤见
 * {@code OpenapiMemberPointOrderV2ListFilterBuilder}。
 */
@Schema(
		name = "EcxMemberPointOrderListBody",
		description =
				"会员订单积分列表参数（4 键，body 含键优先否则 query 同名键）。"
						+ " 按租户查 normal_orders，order_id 降序；create_time 下界**固定当年 1 月 1 日零点**"
						+ "（系统时区，无时间入参不可放宽）——仅返回今年以来订单。")
public class EcxMemberPointOrderListBodyDoc {

	@Schema(
			description =
					"会员手机号过滤（明文）；键存在且非空白时 trim 后按固定加密等值定位会员："
							+ "命中 → 按该 user_id 过滤；**查无/空白 → 按 user_id=0 过滤（通常空结果，不报错）**",
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"订单号过滤；键存在且非 null 非空串时 trim+parseLong 等值过滤"
							+ "（\"0\" 参与过滤；非数字按 -1 过滤恒空结果，不报错）；null/空串/键缺省不过滤",
			example = "100")
	public String order_id;

	@Schema(
			description = "页码；默认 1。PHP 式解析（空取默认、纯空白/非法→0、小数截断取整），再取 max(…,1)",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。PHP 式解析后原始值 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错），"
							+ "再取 max(…,20)，实际生效范围 [20,499]",
			example = "20")
	public String page_size;
}