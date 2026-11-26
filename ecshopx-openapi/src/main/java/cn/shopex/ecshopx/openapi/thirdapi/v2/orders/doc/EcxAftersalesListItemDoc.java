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
 * 仅 OpenAPI 文档；列表行 detail 元素（aftersales_detail，按 company_id + aftersales_bn + user_id 查，无显式排序）。
 * {@code OpenapiThirdApiV2AftersalesListFormatSupport#formatDetail} 的 13 键。
 */
@Schema(name = "EcxAftersalesListItem")
public class EcxAftersalesListItemDoc {

	@Schema(description = "商品 goods_id（整型）", example = "1001")
	public Long goods_id;

	@Schema(description = "货品编码 item_bn", example = "A1")
	public String item_bn;

	@Schema(description = "商品名称", example = "示例商品")
	public String item_name;

	@Schema(description = "商品图片（可为 null）", nullable = true, example = "https://example.test/key.jpg")
	public String item_pic;

	@Schema(description = "售后数量", example = "1")
	public Integer num;

	@Schema(description = "该行应退金额（分）", example = "100")
	public Integer refund_fee;

	@Schema(description = "该行应退积分", example = "0")
	public Integer refund_point;

	@Schema(description = "售后类型：ONLY_REFUND / REFUND_GOODS / EXCHANGING_GOODS", example = "REFUND_GOODS")
	public String aftersales_type;

	@Schema(description = "处理进度（同主单 progress 枚举）", example = "2")
	public Integer progress;

	@Schema(description = "售后状态（同主单 aftersales_status 枚举）", example = "1")
	public Integer aftersales_status;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（系统时区；null/≤0 按 epoch 0）", example = "2023-11-15 08:00:01")
	public String create_time;

	@Schema(description = "更新时间——当前实现取的是 create_time（PHP 旧契约，如实记录）", example = "2023-11-15 08:00:01")
	public String update_time;

	@Schema(description = "自动拒绝时间：库内 Unix 秒字符串 >0 时格式化，否则空串", example = "")
	public String auto_refuse_time;
}