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

package cn.shopex.ecshopx.shuyun.service.openplatform;

/** 退款状态 / 售后类型 → 数云字典。对齐 PHP {@code ShuyunOpenPlatformNormalOrderRefundStatusMapper}。 */
public final class RefundStatusMapper {

	private RefundStatusMapper() {}

	public static String mapRefundStatus(String refundStatus) {
		return switch (refundStatus == null ? "" : refundStatus) {
			case "SUCCESS" -> "SY_REFUND_SUCC";
			case "REFUSE", "REFUNDCLOSE", "CANCEL", "CHANGE" -> "SY_REFUND_FAIL";
			case "READY", "AUDIT_SUCCESS" -> "SY_CHECKING";
			case "PROCESSING" -> "SY_REFUNDING";
			default -> "SY_REFUNDING";
		};
	}

	public static String mapGoodReturnFromAftersalesDetailType(String aftersalesType) {
		return switch (aftersalesType == null ? "" : aftersalesType) {
			case "REFUND_GOODS", "EXCHANGING_GOODS" -> "SY_RETURN_FEE_GOOD";
			default -> "SY_ONLY_FEE";
		};
	}

	public static int resolveRefundPhase(String orderStatus, Object refundType) {
		if ("1".equals(String.valueOf(refundType == null ? "" : refundType).trim())
				|| Integer.valueOf(1).equals(refundType)) {
			return 1;
		}
		return "DONE".equals(orderStatus == null ? "" : orderStatus) ? 2 : 1;
	}
}
