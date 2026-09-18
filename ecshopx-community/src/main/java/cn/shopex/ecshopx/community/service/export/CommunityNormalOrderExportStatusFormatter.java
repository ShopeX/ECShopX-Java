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

package cn.shopex.ecshopx.community.service.export;

import cn.shopex.ecshopx.community.dto.export.CommunityNormalOrderExportRow;
import org.springframework.util.StringUtils;

/**
 * 导出用订单状态文案，与既有后台展示分支保持一致。
 */
public final class CommunityNormalOrderExportStatusFormatter {

	private CommunityNormalOrderExportStatusFormatter() {}

	public static String format(CommunityNormalOrderExportRow row) {
		if (row == null) {
			return "订单异常";
		}
		String orderStatus = nullToEmpty(row.getOrderStatus());
		String cancelStatus = nullToEmpty(row.getCancelStatus());
		String zitiStatus = nullToEmpty(row.getZitiStatus());
		String deliveryStatus = nullToEmpty(row.getDeliveryStatus());

		switch (orderStatus) {
			case "WAIT_GROUPS_SUCCESS":
				return "等待成团";
			case "NOTPAY":
				return "待支付";
			case "PAYED":
				if ("WAIT_PROCESS".equals(cancelStatus)) {
					return "退款处理中";
				}
				if ("PENDING".equals(zitiStatus)) {
					return "待自提";
				}
				if ("PARTAIL".equals(deliveryStatus)) {
					return "部分发货";
				}
				return "待发货";
			case "REVIEW_PASS":
				if (!"PARTAIL".equals(deliveryStatus)) {
					return "审核完成,待出库";
				}
				// 部分发货时与取消流程共用同一套文案分支
				return cancelBranch(deliveryStatus, zitiStatus, cancelStatus);
			case "CANCEL":
				return cancelBranch(deliveryStatus, zitiStatus, cancelStatus);
			case "WAIT_BUYER_CONFIRM":
				return "待收货";
			case "DONE":
				return "已完成";
			case "REFUND_PROCESS":
				return "退款处理中";
			case "REFUND_SUCCESS":
				return "已退款";
			default:
				return "订单异常";
		}
	}

	private static String cancelBranch(String deliveryStatus, String zitiStatus, String cancelStatus) {
		if ("DONE".equals(deliveryStatus) || "DONE".equals(zitiStatus)) {
			return "已关闭";
		}
		if ("NO_APPLY_CANCEL".equals(cancelStatus)) {
			return "已取消";
		}
		if ("WAIT_PROCESS ".equals(cancelStatus)) {
			return "退款处理中";
		}
		if ("REFUND_PROCESS".equals(cancelStatus)) {
			return "退款处理中";
		}
		if ("SUCCESS".equals(cancelStatus)) {
			return "已取消";
		}
		return "等待退款";
	}

	private static String nullToEmpty(String s) {
		return StringUtils.hasText(s) ? s : "";
	}
}
