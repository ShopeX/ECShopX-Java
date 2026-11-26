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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；list 行（18 键）。
 * {@code OpenapiThirdApiV2AftersalesListFormatSupport#formatOpenApiAftersalesRow}：
 * 与详情行的差异——多 sendconfirm_data、aftersales_address 空时回退门店默认地址、无 refund_bn。
 */
@Schema(name = "EcxAftersalesListRow")
public class EcxAftersalesListRowDoc {

	@Schema(description = "售后单号（字符串形态）", example = "202605101200001")
	public String aftersales_bn;

	@Schema(description = "订单号（字符串形态）", example = "88001")
	public String order_id;

	@Schema(
			description = "售后类型：ONLY_REFUND 仅退款 / REFUND_GOODS 退货退款 / EXCHANGING_GOODS 换货",
			allowableValues = {"ONLY_REFUND", "REFUND_GOODS", "EXCHANGING_GOODS"},
			example = "REFUND_GOODS")
	public String aftersales_type;

	@Schema(description = "售后状态：0 待处理 / 1 处理中 / 2 已处理 / 3 已驳回 / 4 已撤销", example = "1")
	public Integer aftersales_status;

	@Schema(
			description =
					"处理进度：0 等待商家处理 / 1 商家接受待回寄 / 2 消费者回寄待收货确认 / 8 确认收货待审核退款 /"
							+ " 3 已驳回 / 4 已处理 / 7 已撤销 / 9 退款处理中 / 5 退款驳回 / 6 退款完成",
			example = "2")
	public Integer progress;

	@Schema(description = "应退总金额（分）", example = "100")
	public Integer refund_fee;

	@Schema(description = "应退总积分", example = "0")
	public Integer refund_point;

	@Schema(description = "申请售后原因（最长 300）", example = "测试售后")
	public String reason;

	@Schema(description = "申请描述（可为 null）", nullable = true)
	public String description;

	@ArraySchema(
			arraySchema = @Schema(description = "图片凭证 URL 数组；库内非 JSON 数组/空时恒为 []"),
			schema = @Schema(type = "string"))
	public List<String> evidence_pic;

	@Schema(description = "拒绝原因（可为 null）", nullable = true)
	public String refuse_reason;

	@Schema(description = "售后备注（可为 null）", nullable = true)
	public String memo;

	@Schema(
			description = "消费者回寄物流对象（未提交/解析失败/\"0\" 时为 null）",
			nullable = true,
			implementation = EcxAftersalesDetailSendbackDataDoc.class)
	public EcxAftersalesDetailSendbackDataDoc sendback_data;

	@Schema(
			description =
					"商家重新发货物流信息：aftersales.sendconfirm_data JSON 对象透传（空/\"0\"/解析失败为 null）。"
							+ " 当前 Java 代码无该列写入路径（换货重发流程未迁入），键集无法从代码证实，"
							+ " 故不声明内部结构、不虚构示例；存量数据如有则原样返回",
			nullable = true,
			type = "object")
	public Object sendconfirm_data;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（系统时区；null/≤0 按 epoch 0）", example = "2023-11-15 08:00:01")
	public String create_time;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss（null/≤0 按 epoch 0）", example = "2023-11-15 08:00:01")
	public String update_time;

	@Schema(
			description = "售后回寄地址对象；主单 JSON 或门店默认地址回退（键集见 schema），均为空时 {}",
			implementation = EcxAftersalesListAddressDoc.class)
	public EcxAftersalesListAddressDoc aftersales_address;

	@ArraySchema(
			arraySchema = @Schema(description = "售后明细行（按 company_id+aftersales_bn+user_id 查）；无明细为 []"),
			schema = @Schema(implementation = EcxAftersalesListItemDoc.class))
	public List<EcxAftersalesListItemDoc> detail;
}