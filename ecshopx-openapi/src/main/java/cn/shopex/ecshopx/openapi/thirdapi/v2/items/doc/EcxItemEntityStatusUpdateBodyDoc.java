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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code NormalItemsController#batchUpdateItemsStatus}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key，
 * 校验见 {@code OpenapiThirdApiV2NormalItemsEntityStatusUpdateService}。
 */
@Schema(
		name = "EcxItemEntityStatusUpdateBody",
		description =
				"批量更新商品上下架参数（body 与 query 同名 key 双通道：body 含键取原始值，否则用 query；解析时 trim）。"
						+ " 按默认 SKU 货号定位 goods_id，整组 SKU 更新 approve_status。")
public class EcxItemEntityStatusUpdateBodyDoc {

	@ArraySchema(
			arraySchema = @Schema(
					description =
							"商品货号 JSON 数组字符串（元素字符串化，null 元素按空串）；必填非空白否则 E5001「货号必填」；"
									+ "非 JSON 数组 E5100「请填写正确的货号格式」；超 1000 个 E5100「商品状态可更改的最大数量为1000」；"
									+ "全部查无默认 SKU E5101「商品找不到」",
					requiredMode = Schema.RequiredMode.REQUIRED),
			schema = @Schema(type = "string", example = "SKU200"))
	public List<String> item_bn;

	@Schema(
			description =
					"目标状态；必填且仅 onsale/instock（trim 后），否则 E5001「状态必填,且必须是 onsale 或 instock 」；"
							+ "onsale 时守卫：供应商商品不可售 / 处方药需医药行业配置 / 处方药待审核（均 E0000+fail PHP 旧契约）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"onsale", "instock"},
			example = "onsale")
	public String status;
}