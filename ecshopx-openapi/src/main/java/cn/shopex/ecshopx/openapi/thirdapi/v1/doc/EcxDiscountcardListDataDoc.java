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
import java.util.Map;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1DiscountCardGetDiscountCardListService#execute} +
 * {@code OpenapiDiscountCardListPresentationService#apply}。无 pagers 键（显式移除）。
 */
@Schema(
		name = "EcxDiscountcardListData",
		description = "卡券列表：total_count + list + 固定枚举字典 opts；按 created 倒序分页",
		example =
				"{\"total_count\":1,\"list\":[{\"card_id\":3,\"title\":\"满减券\",\"color\":\"#ff1939\","
						+ "\"card_type\":\"cash\",\"date_type\":\"DATE_TYPE_FIX_TERM\",\"card_type_val\":\"代金券\","
						+ "\"discount\":0,\"least_cost\":0,\"reduce_cost\":0,\"quantity\":100,\"get_num\":10,"
						+ "\"card_num\":90,\"takeEffect\":\"领取后当天生效,30天有效\",\"begin_time\":\"\",\"end_time\":\"\"}],"
						+ "\"opts\":{\"card_type\":{\"discount\":\"折扣券\",\"cash\":\"代金券\",\"gift\":\"兑换券\"},"
						+ "\"channel\":{\"wechat\":\"微信·\",\"alipay\":\"支付宝\",\"all\":\"全渠道\"},"
						+ "\"card_source\":{\"local\":\"本地\",\"topi\":\"TOPI\",\"zhibo\":\"zhibo\",\"merch\":\"商家券\"}}}")
public class EcxDiscountcardListDataDoc {

	@Schema(description = "符合条件的总记录数", example = "1")
	public Long total_count;

	@ArraySchema(arraySchema = @Schema(description = "卡券列表；无匹配时为空数组"))
	public List<EcxDiscountcardListItemDoc> list;

	@Schema(description = "固定枚举字典（键值由代码常量决定）")
	public OptsDoc opts;

	@Schema(name = "EcxDiscountcardListOpts", description = "枚举字典")
	public static class OptsDoc {

		@Schema(
				description = "卡券类型字典，固定 {discount:折扣券, cash:代金券, gift:兑换券}",
				example = "{\"discount\":\"折扣券\",\"cash\":\"代金券\",\"gift\":\"兑换券\"}")
		public Map<String, String> card_type;

		@Schema(
				description = "渠道字典，固定 {wechat:微信·, alipay:支付宝, all:全渠道}",
				example = "{\"wechat\":\"微信·\",\"alipay\":\"支付宝\",\"all\":\"全渠道\"}")
		public Map<String, String> channel;

		@Schema(
				description = "卡券来源字典，固定 {local:本地, topi:TOPI, zhibo:zhibo, merch:商家券}",
				example = "{\"local\":\"本地\",\"topi\":\"TOPI\",\"zhibo\":\"zhibo\",\"merch\":\"商家券\"}")
		public Map<String, String> card_source;
	}
}