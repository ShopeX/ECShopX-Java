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
 * {@code DigiOsStockPushService#pushStoreStock}：逐行处理，失败行不中断整体（信封恒 success）。
 */
@Schema(
		name = "EcxDigiosStockPushData",
		description = "逐行推送结果：未找到的货品、更新失败明细；整体信封恒为成功，失败信息在 data 内",
		example =
				"{\"not_find_sku\":[\"A2\"],\"not_update_sku\":[],"
						+ "\"update_error\":[{\"sku_id\":\"A1\",\"shop_code\":\"S02\",\"error\":\"门店不存在\"}]}")
public class EcxDigiosStockPushDataDoc {

	@ArraySchema(
			arraySchema = @Schema(description = "按 item_bn 未找到的货品编码列表"),
			schema = @Schema(example = "A2"))
	public List<String> not_find_sku;

	@ArraySchema(arraySchema = @Schema(description = "恒为空数组（对齐 PHP 契约保留字段，当前实现不写入）"))
	public List<Object> not_update_sku;

	@ArraySchema(arraySchema = @Schema(description = "更新失败明细列表"))
	public List<UpdateErrorRowDoc> update_error;

	@Schema(name = "EcxDigiosStockPushUpdateErrorRow", description = "单条更新失败明细")
	public static class UpdateErrorRowDoc {

		@Schema(description = "货品编码", example = "A1")
		public String sku_id;

		@Schema(description = "门店店铺号", example = "S02")
		public String shop_code;

		@Schema(
				description =
						"失败原因：门店不存在；或库存导入校验文案（商品不存在/只能导入所属店铺的商品库存/门店库存为总部库存等）；"
								+ "其他异常统一为「更新失败」",
				example = "门店不存在")
		public String error;
	}
}