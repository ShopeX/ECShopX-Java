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
 * 仅 OpenAPI 文档；POST 请求 JSON body，2 键与 query 同名双通道（均 {@code originalString}：
 * body 含键优先、值 null 视为未传，取值不 trim；服务内 trim 后校验）。
 * 编排见 {@code OpenapiThirdApiV2OrderWriteoffService#executeOpenapiWriteoff}，
 * 核销落库见 {@code NormalOrderZitiWriteoffService#orderZitiWriteoffForOpenapi}（进程内按 company:order 互斥）。
 */
@Schema(
		name = "EcxOrderWriteoffBody",
		description =
				"自提订单核销参数（2 键，body 含键优先否则 query 同名键）。"
						+ " 仅自提（receipt_type=ziti）实体单可核销；租户提货码开关开启时须校验并消费提货码。")
public class EcxOrderWriteoffBodyDoc {

	@Schema(
			description =
					"订单 ID；必填（trim 后非空，否则 E5001「请填写订单编号」）。"
							+ "trim 后 Long 解析，非整数/≤0/关联表或订单表无此单 → E5301「订单相应的明细不存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "501")
	public String order_id;

	@Schema(
			description =
					"提货码；租户提货码开关（pickupcode_status，Redis 设置）开启时必填"
							+ "（trim 后非空，否则 E5001「请填写提货码」；community 社区活动单强制视为开关关闭）。"
							+ "校验双通道：Redis admin-pickupcode:{order_id}|{下单手机号} 精确或数值宽松相等，"
							+ "或 orderziticode:{code} 映射到本单；通过后两键即删除（一码一用）。"
							+ "验证失败 → E5310「提货码验证错误！」；订单无下单手机号 → E5310「未查询到提货人联系手机！」。"
							+ "开关关闭时忽略本键（仅 trim 后记入流程日志）",
			example = "760773986531")
	public String pickupcode;
}