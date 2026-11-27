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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，9 键与 query 同名双通道（均 {@code originalString}：
 * body 含键优先、值 null 视为未传，取值不 trim；过滤判定用 PHP truthy/present 语义）。
 * 分页见 {@code OpenapiThirdApiV2OrderSoldListParams#resolve}（ResourceException → E0001，
 * 与增量接口的差异：本解析器不校验非整数文本——非法文本静默取 0 后回退默认），
 * 过滤见 {@code OpenapiThirdApiV2OrderSoldListService#execute/buildWrapper}
 * （固定 supplier_id=0，create_time 窗口过滤、create_time 降序）。
 */
@Schema(
		name = "EcxOrdersSoldGetBody",
		description =
				"订单搜索参数（9 键，body 含键优先否则 query 同名键）。"
						+ " 按 create_time 窗口搜索平台/店铺订单（不含供应商单），create_time 降序。")
public class EcxOrdersSoldGetBodyDoc {

	@Schema(
			description =
					"页码；默认 1。PHP 式解析（小数截断、非法/空白→0）后 max(…,1)；"
							+ "不校验非整数文本（如 \"abc\" 静默回退 1）；「当前页面最小值为1」分支不可达",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。原始解析值 ≥500 报 E0001「每页的大小最大为500！」（500 本身也报错）；"
							+ "再 max(…,20)，实际生效范围 [20,499]；非法文本静默回退 20；「每页显示数量1-500」分支不可达",
			example = "20")
	public String page_size;

	@Schema(
			description =
					"会员手机号过滤；PHP truthy（非 null/空串/\"0\"）才生效，trim 后查会员，"
							+ "**查无会员返回成功空数组 data=[]（非分页结构，不报错）**",
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"下单时间窗口起点（create_time ≥）；PHP truthy 才生效。仅接受 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd"
							+ "（系统时区，日期取 00:00:00）；其他格式静默解析为 0（epoch 1970，等于不设下限）",
			example = "2026-01-01 00:00:00")
	public String time_begin;

	@Schema(
			description =
					"下单时间窗口终点（create_time ≤）；语义同 time_begin；格式非法静默为 0 → 过滤 create_time≤0 通常查空",
			example = "2026-01-02 00:00:00")
	public String time_end;

	@Schema(
			description =
					"店铺编码过滤；PHP truthy 才生效，trim 后按 distribution_distributor.shop_code 查店铺"
							+ "（**该查询无 company_id 条件**，按现状记录）；查不到按 distributor_id=-1 过滤（空结果）。"
							+ "is_self 键出现时覆盖本键效果（is_self 后应用）",
			example = "SHOP001")
	public String shop_code;

	@Schema(
			description =
					"平台/店铺单过滤；**键出现即生效**（present 语义，含 null）：值精确等于字符串 \"true\" → 仅平台单"
							+ "（distributor_id=0）；其他任意值（含 null/\"false\"/\"1\"）→ 仅店铺单（distributor_id>0）。"
							+ "覆盖 shop_code 过滤",
			example = "true")
	public String is_self;

	@Schema(
			description =
					"订单状态精确过滤；键出现即生效（null 视为空串匹配）。可选值 DONE/PAYED/NOTPAY/CANCEL/WAIT_BUYER_CONFIRM",
			example = "PAYED")
	public String order_status;

	@Schema(
			description =
					"支付状态精确过滤；键出现即生效（null 视为空串匹配）。可选值 NOTPAY/PAYED/ADVANCE_PAY/TAIL_PAY",
			example = "PAYED")
	public String pay_status;
}