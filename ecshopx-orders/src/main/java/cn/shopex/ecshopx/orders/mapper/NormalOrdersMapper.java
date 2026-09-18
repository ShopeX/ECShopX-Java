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

package cn.shopex.ecshopx.orders.mapper;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.dto.DeliveryStaffOrderAggregateRow;
import cn.shopex.ecshopx.orders.domain.dto.MonitorSourcePaidOrderAggRow;
import cn.shopex.ecshopx.orders.domain.dto.NormalOrderExportItemRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NormalOrdersMapper extends BaseMapper<NormalOrders> {

	List<DeliveryStaffOrderAggregateRow> selectDeliveryStaffAggregates(
			@Param("companyId") long companyId,
			@Param("startEpoch") long startEpoch,
			@Param("endEpoch") long endEpoch,
			@Param("operatorIds") List<Long> operatorIds,
			@Param("distributorIds") List<Long> distributorIds,
			@Param("merchantId") Long merchantId);

	MonitorSourcePaidOrderAggRow selectPaidAggsByMonitorSource(
			@Param("companyId") long companyId,
			@Param("monitorId") Object monitorId,
			@Param("sourceId") Object sourceId,
			@Param("startEpoch") long startEpoch,
			@Param("endEpoch") long endEpoch);

	long countExportNormalOrderItems(@Param("filter") LinkedHashMap<String, Object> filter);

	List<NormalOrderExportItemRow> selectExportNormalOrderItems(
			@Param("filter") LinkedHashMap<String, Object> filter,
			@Param("offset") long offset,
			@Param("limit") int limit);

	long countOfflinePayCancelable(@Param("threshold") long threshold);

	List<NormalOrders> selectOfflinePayCancelable(@Param("threshold") long threshold, @Param("limit") int limit);

	/** 消费累加任务：候选页，offset 恒 0，按 create_time DESC。 */
	List<NormalOrders> selectConsumptionCandidatePage(
			@Param("timeSec") long timeSec,
			@Param("excludeOrderIds") List<Long> excludeOrderIds,
			@Param("limit") int limit);

	/** 仅 is_consumption 仍为 0 时置 1。 */
	int updateIsConsumptionWhenZero(@Param("orderIds") List<Long> orderIds);

	/** 自动完成收货任务：与 PHP {@code FinishOrderJob} 筛选一致。 */
	long countAutoFinishCandidates(@Param("finishBeforeSec") long finishBeforeSec);

	/**
	 * 页下标从 0 起，offset = pageIndex * pageSize（与 PHP {@code getList($filter, $i, 20)} 一致，非
	 * consumpton 的 offset=0 模式）。
	 */
	List<NormalOrders> selectAutoFinishCandidatePage(
			@Param("finishBeforeSec") long finishBeforeSec,
			@Param("offset") long offset,
			@Param("limit") int limit);

	/**
	 * 有数订单日汇总：下单金额（分，{@code total_fee} 之和）；与 PHP
	 * {@code OrderService::countOrderAmount} 窗口一致。
	 */
	Long sumOrderTotalFeeCentsForYoushu(
			@Param("companyId") long companyId,
			@Param("startSec") long startSec,
			@Param("endSec") long endSec);

	/** 有数订单日汇总：下单笔数；与 {@code countOrderNum} 窗口一致。 */
	long countOrdersForYoushu(
			@Param("companyId") long companyId,
			@Param("startSec") long startSec,
			@Param("endSec") long endSec);

	List<LinkedHashMap<String, Object>> sumTotalFeeGroupByUserId(
			@Param("filter") Map<String, Object> filter);

	Long sumTotalFeeCentsByFilter(
			@Param("companyId") long companyId,
			@Param("userId") long userId);
}

