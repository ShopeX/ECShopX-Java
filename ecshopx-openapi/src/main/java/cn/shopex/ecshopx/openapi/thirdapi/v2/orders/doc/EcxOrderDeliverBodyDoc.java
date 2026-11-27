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
 * 仅 OpenAPI 文档；POST 请求 JSON body，4 键与 query 同名双通道（均 {@code mergeString}：
 * body 含键优先——值 null/空白串归一为 null（未传）；否则 query trim 后非空才生效；
 * body 值为数组/对象时序列化为 JSON 字符串，数字/布尔转字符串）。
 * 校验见 {@code OpenapiThirdApiV2OrderDeliverService#parseItemInfoFormat/validateParamsV2}，
 * 发货执行见 {@code AdminNormalOrderDeliveryCoreService#deliveryNormalPhysical}
 * （delivery_type 固定 sep 拆单、operator_type=system、operator_id=0、supplier_id=0）。
 */
@Schema(
		name = "EcxOrderDeliverBody",
		description =
				"订单发货参数（4 键，body 含键优先否则 query 同名键）。"
						+ " 按 sku_id 匹配订单待发商品行的 item_bn（商品编码），qty 记入发货数量；"
						+ " 全部待发行发完则订单置 DONE/WAIT_BUYER_CONFIRM，否则 PARTAIL（部分发货）。")
public class EcxOrderDeliverBodyDoc {

	@Schema(
			description =
					"订单 ID；必填（非空否则 E5001「订单号必填」）。Long 解析失败或该租户无此单 →"
							+ " E5301「订单相应的明细不存在」；delivery_status=DONE → E5312「订单已发货，请勿重复发货」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "100")
	public String order_id;

	@Schema(
			description =
					"发货商品信息；必填，JSON 数组字符串（body 内也可直接传 JSON 数组，mergeString 自动序列化）。"
							+ "空/非法 JSON/空数组/元素非对象 → E5002「发货商品信息格式错误」。"
							+ "每元素仅读取 3 键：name（商品名称，必填仅校验非空，不参与匹配）、"
							+ "sku_id（商品货号，必填，String 化后匹配订单行 item_bn）、"
							+ "qty（发货数量，必填非空；须为正整数字符串 ^[1-9][0-9]*$，"
							+ "违反 → E5310「订单号为{orderId}的订单,发货商品数量错误:{值}」；"
							+ "大于剩余可发数量（购买-已发-已取消）→ E5310「…发货商品数量{num}大于购买数量{remain}」）。"
							+ "字符串值 trim 后空视为缺失（E5001 对应文案）；sku_id 重复时后者覆盖前者",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "[{\"name\":\"Item-A\",\"sku_id\":\"SKU-A\",\"qty\":1}]")
	public String item_info;

	@Schema(
			description =
					"快递公司编码；必填（非空否则 E5001「快递公司编码必填」）。trim 后记入发货单与订单；"
							+ "租户启用 kuaidi100 时按 kuaidi_code 忽略大小写匹配物流公司，否则按 corp_code 精确匹配；"
							+ "未匹配到显示名时用「其他」，不报错",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SF")
	public String lc_code;

	@Schema(
			description = "快递单号；必填（非空否则 E5001「快递单编码必填」）。trim 后记入发货单与订单",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SFTRACK001")
	public String l_code;
}