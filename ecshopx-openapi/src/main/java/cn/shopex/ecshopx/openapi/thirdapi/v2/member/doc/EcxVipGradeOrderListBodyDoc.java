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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，2 键与 query 同名双通道
 * （{@code OpenapiRequestParams#originalString}：body 含键优先、值 null 视为未传、不 trim），
 * 分页解析复用 {@code OpenapiThirdApiV2ShippingTemplatesListParams#resolve}
 * （失败走 ResourceException → E0001）。
 */
@Schema(
		name = "EcxVipGradeOrderListBody",
		description =
				"付费会员卡等级购买记录列表参数（2 键，body 含键优先否则 query 同名键）。"
						+ " 固定过滤 kaquan_vip_grade_order.order_status=DONE（已完成订单），无其他过滤键。")
public class EcxVipGradeOrderListBodyDoc {

	@Schema(
			description =
					"页码；默认 1。PHP 式解析（空取默认、纯空白/非法→0、小数截断取整），再取 max(…,1)；"
							+ "「当前页面最小值为1」分支不可达",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。PHP 式解析后原始值 ≥500 报 E0001「每页的大小最大为500！」（500 本身也报错；"
							+ "分页失败走 ResourceException 通道），再取 max(…,20)，实际生效范围 [20,499]；"
							+ "「每页显示数量1-500」分支不可达",
			example = "20")
	public String page_size;
}