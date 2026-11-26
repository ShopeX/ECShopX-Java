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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，5 键与 query 同名双通道
 * （{@code OpenapiRequestParams#originalString}：body 含键优先、值 null 视为未传、不 trim；
 * mobile/start_date/end_date 另按键存在性判定 present），分页解析见
 * {@code OpenapiThirdApiV2MemberListParams#resolve}（E5002），过滤见
 * {@code OpenapiMemberOperateLogV2ListFilterBuilder}。
 */
@Schema(
		name = "EcxMemberOperateLogListBody",
		description =
				"会员信息操作日志列表参数（5 键，body 含键优先否则 query 同名键）。"
						+ " 按租户查 member_operate_log，created 降序。")
public class EcxMemberOperateLogListBodyDoc {

	@Schema(
			description =
					"会员手机号过滤（明文）；键存在且非空白时 trim 后按固定加密等值定位会员："
							+ "命中 → 按该 user_id 过滤；**查无/空白不报错且不过滤**（回落全租户日志，PHP isset 语义）",
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"起始时间（created ≥）；须 yyyy-MM-dd HH:mm:ss（系统时区）；键存在且非空非\"0\"时解析，"
							+ "纯数字（时间戳）或其他格式报 E4002「时间格式有误」；空串/\"0\"/键缺省不过滤（示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String start_date;

	@Schema(
			description =
					"截止时间（created ≤）；格式与解析同 start_date（示例为格式演示值）",
			example = "2026-12-31 23:59:59")
	public String end_date;

	@Schema(
			description =
					"页码；默认 1。PHP 式解析（空取默认、纯空白/非法→0、小数截断取整），再取 max(…,1)",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。PHP 式解析后原始值 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错），"
							+ "再取 max(…,20)，实际生效范围 [20,499]",
			example = "20")
	public String page_size;
}