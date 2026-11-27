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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，2 键与 query 同名双通道（均 {@code originalString}：
 * body 含键优先、值 null 视为未传，取值不 trim；服务内按 PHP falsy 判定并 trim）。
 * 解析见 {@code OpenapiThirdApiV2OrderDetailService#execute}：order_id 与 ziti_code 二选一，
 * order_id 优先；ziti_code 经 Redis orderziticode:{code} 映射订单号。
 */
@Schema(
		name = "EcxOrderGetBody",
		description =
				"订单详情参数（2 键，body 含键优先否则 query 同名键）。"
						+ " PHP falsy 语义：null/trim 后空串/\"0\" 均视为未传；两者都未传 → E0001「请填写订单编号」。")
public class EcxOrderGetBodyDoc {

	@Schema(
			description =
					"订单 ID；与 ziti_code 二选一（本键优先）。trim 后 Long 解析，非整数 → HTTP 400"
							+ " ApiResult{code:400,msg:参数错误}（非信封）；无此单 → HTTP 200 信封"
							+ " status=fail、code=E0000（PHP 零码契约）「订单号为{id}的订单不存在」",
			example = "100")
	public String order_id;

	@Schema(
			description =
					"自提核销码；order_id 未传（PHP falsy）时生效：经 Redis orderziticode:{code} 查订单号，"
							+ "无映射/过期 → E0001「订单相应的明细不存在」。"
							+ "注意实现细节：仅当 \"ZT_\" 出现在下标 >0 处才截掉前 3 字符——以前缀 \"ZT_\" 开头（下标 0）"
							+ "反而不截断，按原串查 Redis（按现状记录）",
			example = "760773986531")
	public String ziti_code;
}