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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 分页结构，来源
 * {@code OpenapiThirdApiV2MemberTagListService#formatListStruct}（4 固定键）。
 * is_last_page 为整型 0/1（ceil(total_count/page_size) ≤ page → 1）。
 */
@Schema(name = "EcxMemberRechargeTradeListData")
public class EcxMemberRechargeTradeListDataDoc {

	@Schema(description = "满足过滤条件的成功交易总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "交易行数组（time_start 降序；无结果为空数组）")
	public List<EcxMemberRechargeTradeListRowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberRechargeTradeListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 交易行（15 固定键），来源 formatOpenApiListRow（deposit_trade）。 */
	@Schema(name = "EcxMemberRechargeTradeListRow")
	public static class EcxMemberRechargeTradeListRowDoc {

		@Schema(description = "储值交易单号（deposit_trade_id，字符串）", example = "d1")
		public String trade_id;

		@Schema(description = "交易类型（recharge 充值/consume 消费）", example = "recharge")
		public String trade_type;

		@Schema(description = "金额（元，字符串 2 位小数；库内分字符串 HALF_UP 除 100；null/空白按 0 → \"0.00\"）", example = "80.00")
		public String money;

		@Schema(description = "会员手机号（密文解密明文；库值 null 时透出 null）", nullable = true, example = "13800138000")
		public String mobile;

		@Schema(description = "充值会员卡号（库值原样）", example = "card")
		public String member_card_code;

		@Schema(description = "门店 ID（库值字符串原样）", example = "0")
		public String shop_id;

		@Schema(description = "门店名称（库值原样）", example = "")
		public String shop_name;

		@Schema(description = "用户 open_id（库值，可为 null）", nullable = true)
		public String open_id;

		@Schema(description = "第三方交易号（trim；null/空白归 null）", nullable = true)
		public String transaction_id;

		@Schema(description = "储值面额规则 ID（trim；null/空白归 null）", nullable = true)
		public String recharge_rule_id;

		@Schema(description = "银行/支付类型（trim；null/空白归 null）", nullable = true)
		public String bank_type;

		@Schema(description = "微信授权方 appid（库值原样）", example = "")
		public String authorizer_appid;

		@Schema(description = "交易详情（库值原样字符串）", example = "")
		public String detail;

		@Schema(
				description = "交易开始时间（yyyy-MM-dd HH:mm:ss，系统时区；库内 Unix 秒字符串解析，null/非法按 epoch 0 格式化；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String time_start;

		@Schema(
				description = "交易过期时间（格式化同 time_start；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String time_expire;
	}
}