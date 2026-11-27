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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 结构（3 固定键，键序 not_find_sku/not_update_sku/update_error），来源
 * {@code OpenapiThirdApiV2ProductStockUpdateService#executeOpenapiProductStockUpdate}。
 * 成功更新的货号不回传（三个数组均空即全部成功）。not_update_sku 为活动商品保留位——
 * 当前实现 activityBns 恒空列表，本数组恒为空（按现状记录）。
 */
@Schema(name = "EcxProductStockUpdateData")
public class EcxProductStockUpdateDataDoc {

	@Schema(description = "查不到的货号数组（字符串；部分未命中不报错，全部未命中报 E5101）")
	public List<String> not_find_sku;

	@Schema(description = "活动商品暂不更新的货号数组（当前实现恒为空——活动清单保留位）")
	public List<String> not_update_sku;

	@Schema(description = "更新失败明细数组（总店分支 2 键；店铺分支 3 键）")
	public List<UpdateErrorDoc> update_error;

	/** 更新失败行（总店分支 sku_id/error；店铺分支多 shop_code 键）。 */
	@Schema(name = "EcxProductStockUpdateError")
	public static class UpdateErrorDoc {

		@Schema(description = "货号（trim 后）", example = "SKU200")
		public String sku_id;

		@Schema(
				description = "店铺编码（仅店铺分支携带；原值 String 化）",
				example = "SHOP001")
		public String shop_code;

		@Schema(
				description =
						"失败原因：总店分支「更新失败」（写 Redis/DB 异常）；店铺分支「门店不存在」/"
								+ "「请填写店铺ID」（查到店铺时当前实现恒此文案）/其他异常 message",
				example = "更新失败")
		public String error;
	}
}