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

package cn.shopex.ecshopx.common.port.order;

import java.util.List;

/**
 * 企业购预充点售后部分退：仅在退款 SUCCESS 时还点。
 */
public interface EmployeePurchasePrepaidAftersalesRestorePort {

	record RefundLine(long itemId, int num, int itemFee) {}

	/**
	 * @param requestedRestoreFee 本次申请还点数（分）= 商品实退 +（若退运费）运费
	 * @param refundLines 本次售后涉及的商品行（用于 SKU 限购还点）；可为空
	 */
	void restoreOnRefundSuccess(
			long companyId,
			long orderId,
			long refundBn,
			int requestedRestoreFee,
			List<RefundLine> refundLines);
}
