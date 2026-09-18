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

package cn.shopex.ecshopx.hfpay.mapper.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** MyBatis parameter object for {@code HfpayStatisticsOrderListMapper}. */
@Value
@Builder
public class HfpayStatisticsOrderListParams {

	long companyId;
	long startUnix;
	long endUnix;
	Long distributorId;
	/** Optional exact {@code a.order_id} filter. */
	String narrowOrderId;
	String appPayType;
	Integer profitsharingStatus;
	/**
	 * NONE | REFUNDING | REFUND_SUCCESS | REFUND_FAIL | PAY — controls refund join and status predicates.
	 */
	String statusKind;
	/** For REFUND_FAIL or PAY: order ids from refund-partition scan; ignored when empty for PAY (means
	 * {@code c}-null only). */
	List<String> partitionIds;
	/** PAY + non-empty partition: SQL {@code (a.order_id IN (...) OR c.order_id IS NULL)}. */
	boolean payOrPartitionOrNullC;

	public boolean isJoinRefundC() {
		return statusKind != null
				&& !"NONE".equals(statusKind);
	}
}
