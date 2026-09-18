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

package cn.shopex.ecshopx.companys.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompanysNoticeStatisticsReadMapper {

	@Select("""
			SELECT COUNT(*) FROM items
			WHERE company_id = #{companyId}
			  AND type = 0
			  AND item_type = 'normal'
			  AND is_default = 1
			  AND store <= #{warningStore}
			  AND distributor_id = 0
			""")
	long countLowStockItemsAll(@Param("companyId") long companyId, @Param("warningStore") int warningStore);

	@Select("""
			SELECT COUNT(*) FROM items
			WHERE company_id = #{companyId}
			  AND type = 0
			  AND item_type = 'normal'
			  AND is_default = 1
			  AND store <= #{warningStore}
			  AND distributor_id = 0
			  AND supplier_item_id = 0
			""")
	long countLowStockItemsPlat(@Param("companyId") long companyId, @Param("warningStore") int warningStore);

	@Select("""
			SELECT COUNT(*) FROM promotions_seckill_activity
			WHERE company_id = #{companyId}
			  AND activity_start_time <= #{now}
			  AND activity_end_time > #{now}
			  AND seckill_type = 'normal'
			  AND disabled = 0
			""")
	long countActiveSeckill(@Param("companyId") long companyId, @Param("now") int now);

	@Select("""
			SELECT COUNT(*) FROM promotion_groups_activity
			WHERE company_id = #{companyId}
			  AND begin_time <= #{now}
			  AND end_time >= #{now}
			  AND disabled = 0
			""")
	long countActiveGroups(@Param("companyId") long companyId, @Param("now") int now);

	@Select("""
			SELECT COUNT(*) FROM orders_normal_orders
			WHERE company_id = #{companyId}
			  AND order_type = 'normal'
			  AND order_status = 'PAYED'
			  AND receipt_type = 'logistics'
			  AND order_class NOT IN ('drug', 'pointsmall')
			  AND cancel_status IN ('NO_APPLY_CANCEL', 'FAILS')
			""")
	long countWaitDeliveryOrders(@Param("companyId") long companyId);

	@Select("""
			SELECT COUNT(*) FROM aftersales
			WHERE company_id = #{companyId}
			  AND aftersales_status = 0
			""")
	long countAftersalesPending(@Param("companyId") long companyId);

	@Select("""
			SELECT COUNT(*) FROM refund_error_logs
			WHERE company_id = #{companyId}
			  AND is_resubmit = 0
			""")
	long countRefundErrorLogsNotResubmit(@Param("companyId") long companyId);

	@Select("""
			SELECT COUNT(*) FROM orders_normal_orders
			WHERE company_id = #{companyId}
			  AND order_type = 'normal'
			  AND order_status = 'PAYED'
			  AND receipt_type = 'logistics'
			  AND order_class NOT IN ('drug', 'pointsmall')
			  AND cancel_status IN ('NO_APPLY_CANCEL', 'FAILS')
			  AND ziti_status = 'NOTZITI'
			  AND merchant_id = #{merchantId}
			""")
	long countWaitDeliveryOrdersForMerchant(@Param("companyId") long companyId, @Param("merchantId") long merchantId);

	@Select("""
			SELECT COUNT(*) FROM aftersales
			WHERE company_id = #{companyId}
			  AND aftersales_status = 0
			  AND merchant_id = #{merchantId}
			""")
	long countAftersalesPendingForMerchant(@Param("companyId") long companyId, @Param("merchantId") long merchantId);

	@Select("""
			SELECT COUNT(*) FROM refund_error_logs
			WHERE company_id = #{companyId}
			  AND is_resubmit = 0
			  AND merchant_id = #{merchantId}
			""")
	long countRefundErrorLogsNotResubmitForMerchant(
			@Param("companyId") long companyId, @Param("merchantId") long merchantId);
}
