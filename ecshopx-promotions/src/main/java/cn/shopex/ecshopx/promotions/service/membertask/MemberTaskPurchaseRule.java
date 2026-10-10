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

package cn.shopex.ecshopx.promotions.service.membertask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record MemberTaskPurchaseRule(boolean needReceipt, long amountFen) {

	public static MemberTaskPurchaseRule parse(String ruleConfigJson, ObjectMapper objectMapper) {
		if (ruleConfigJson == null || ruleConfigJson.isBlank()) {
			return new MemberTaskPurchaseRule(false, 0L);
		}
		try {
			JsonNode node = objectMapper.readTree(ruleConfigJson);
			boolean needReceipt = node.path("need_receipt").asBoolean(false);
			long amountFen = 0L;
			if (node.has("amount_fen") && node.get("amount_fen").isNumber()) {
				amountFen = node.get("amount_fen").asLong();
			}
			return new MemberTaskPurchaseRule(needReceipt, amountFen);
		} catch (Exception e) {
			return new MemberTaskPurchaseRule(false, 0L);
		}
	}

	public boolean matchesAmount(long totalFeeFen) {
		return amountFen <= 0L || totalFeeFen >= amountFen;
	}

	/** 签收前部分退：按订单剩余应付（实付累计退后）是否仍达门槛。 */
	public boolean matchesRemainingAfterRefund(long orderTotalFeeFen, long refundedFeeFen) {
		long remaining = Math.max(0L, orderTotalFeeFen - Math.max(0L, refundedFeeFen));
		return matchesAmount(remaining);
	}
}
