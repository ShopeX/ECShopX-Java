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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiProductStockUpdatePortImpl#updateItemStore}：逐行处理，行级失败不中断（信封恒成功）。
 */
@Schema(
		name = "EcxProductStockUpdateData",
		description = "逐行同步结果；整体信封恒成功，失败信息在三个数组中",
		example =
				"{\"not_find_sku\":[\"A2\"],\"not_update_sku\":[],"
						+ "\"update_error\":[{\"sku_id\":\"A1\",\"shop_code\":\"S01\",\"error\":\"请填写店铺ID\"}]}")
public class EcxProductStockUpdateDataDoc {

	@ArraySchema(
			arraySchema = @Schema(description = "按 item_bn 未找到的货品编码列表"),
			schema = @Schema(example = "A2"))
	public List<String> not_find_sku;

	@ArraySchema(
			arraySchema = @Schema(description = "活动商品暂不更新的编码列表（当前实现活动名单恒空，此数组恒为 []）"))
	public List<String> not_update_sku;

	@ArraySchema(arraySchema = @Schema(description = "更新失败明细列表"))
	public List<UpdateErrorRowDoc> update_error;

	@Schema(name = "EcxProductStockUpdateErrorRow", description = "单条更新失败明细")
	public static class UpdateErrorRowDoc {

		@Schema(description = "货品编码", example = "A1")
		public String sku_id;

		@Schema(description = "门店店铺号（仅门店分支错误行存在；总部分支错误行无此键）", example = "S01")
		public String shop_code;

		@Schema(
				description =
						"失败原因：门店不存在 / 请填写店铺ID（v1 门店分支当前固定返回）/ 更新失败（总部分支异常）/ 异常 message",
				example = "请填写店铺ID")
		public String error;
	}
}