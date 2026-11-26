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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，7 键与 query 同名双通道：
 * page/page_size 走 {@code originalString}（不 trim），其余 5 键走 {@code mergeString}（trim）；
 * body 含键优先、值 null 视为未传。过滤见
 * {@code OpenapiThirdApiV2MemberRechargeTradeListService}（PHP truthy：空串/"0" 忽略）。
 */
@Schema(
		name = "EcxMemberRechargeTradeListBody",
		description =
				"储值交易记录查询参数（7 键，body 含键优先否则 query 同名键）。"
						+ " 固定过滤 deposit_trade.trade_status=SUCCESS（仅成功交易），time_start 降序。"
						+ " 过滤键按 PHP truthy 判断（空串/\"0\" 视为未传）。")
public class EcxMemberRechargeTradeListBodyDoc {

	@Schema(
			description = "页码；默认 1。PHP 式解析后 max(…,1)；「当前页面最小值为1」分支不可达",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。原始解析值 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错），"
							+ "钳制后生效 [20,499]；「每页显示数量为1-500」分支不可达",
			example = "20")
	public String page_size;

	@Schema(
			description = "会员手机号（明文）；truthy 时 trim 加密后等值过滤 deposit_trade.mobile；空串/\"0\" 忽略",
			example = "13800138000")
	public String mobile;

	@Schema(
			description = "储值交易单号；truthy 时 trim 等值过滤 deposit_trade_id；空串/\"0\" 忽略",
			example = "d1")
	public String trade_id;

	@Schema(
			description = "门店 ID；truthy 时 trim 等值过滤 shop_id（字符串比较）；空串/\"0\" 忽略（示例为格式演示值）",
			example = "1")
	public String shop_id;

	@Schema(
			description =
					"起始时间（time_start ≥）；truthy 时按 yyyy-MM-dd HH:mm:ss（系统时区）解析为 Unix 秒；"
							+ "**解析失败静默按 0（等于不过滤），不报时间格式错误**；空串/\"0\" 忽略（示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String date_begin;

	@Schema(
			description =
					"截止时间（time_start ≤）；解析同 date_begin；**解析失败按 0 → time_start ≤ 0（通常空结果）**（示例为格式演示值）",
			example = "2026-12-31 23:59:59")
	public String date_end;
}