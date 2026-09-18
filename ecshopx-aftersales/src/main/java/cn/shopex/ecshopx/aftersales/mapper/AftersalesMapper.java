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

package cn.shopex.ecshopx.aftersales.mapper;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.dto.DeliveryStaffAftersalesAggregateRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AftersalesMapper extends BaseMapper<Aftersales> {

	List<DeliveryStaffAftersalesAggregateRow> selectDeliveryStaffAftersalesAggregates(
			@Param("companyId") long companyId,
			@Param("startEpoch") long startEpoch,
			@Param("endEpoch") long endEpoch,
			@Param("operatorIds") List<Long> operatorIds,
			@Param("distributorIds") List<Long> distributorIds);

	long countAdminList(@Param("filter") Map<String, Object> filter);

	List<Aftersales> selectAdminList(
			@Param("filter") Map<String, Object> filter, @Param("offset") long offset, @Param("limit") int limit);

	long countAdminListJoinOrder(@Param("filter") Map<String, Object> filter);

	List<Aftersales> selectAdminListJoinOrder(
			@Param("filter") Map<String, Object> filter, @Param("offset") long offset, @Param("limit") int limit);

	long countFrontH5ListJoinOrder(@Param("filter") Map<String, Object> filter);

	List<Aftersales> selectFrontH5ListJoinOrder(
			@Param("filter") Map<String, Object> filter, @Param("offset") long offset, @Param("limit") int limit);

	List<Map<String, Object>> selectItemNamesByAftersalesBns(
			@Param("companyId") long companyId, @Param("bns") List<Long> bns);

	List<Long> selectOrderIdsByCompanyAndOrderClass(
			@Param("companyId") long companyId, @Param("orderClass") String orderClass);

	List<Long> selectOrderIdsByCompanyAndReceiverMobile(
			@Param("companyId") long companyId,
			@Param("plain") String plain,
			@Param("cipher") String cipher);

	List<Long> selectItemIdsByCompanyAndItemNameContains(
			@Param("companyId") long companyId, @Param("likePattern") String likePattern);

	List<Map<String, Object>> selectNormalOrderHeadersByOrderIds(
			@Param("companyId") long companyId, @Param("orderIds") List<Long> orderIds);

	List<Map<String, Object>> selectNormalOrderItemsByCompanyAndOrderIds(
			@Param("companyId") long companyId, @Param("orderIds") List<Long> orderIds);

	List<Map<String, Object>> selectSubOrderPrescriptionBySubIds(
			@Param("companyId") long companyId, @Param("subIds") List<Long> subIds);

	List<Map<String, Object>> selectShoppingGuideWorkUseridBySalespersonIds(
			@Param("companyId") long companyId, @Param("salespersonIds") List<Long> salespersonIds);

	List<Map<String, Object>> selectFinancialExportPage(
			@Param("filter") Map<String, Object> filter,
			@Param("offset") long offset,
			@Param("limit") int limit);

	/**
	 * Aftersales list CSV export rows: one row per {@code aftersales_detail}, joined to {@code aftersales},
	 * filtered with the same admin-list predicates as {@link #selectAdminList}.
	 */
	List<Map<String, Object>> selectRecordListExportPage(
			@Param("filter") Map<String, Object> filter,
			@Param("offset") long offset,
			@Param("limit") int limit);

	/**
	 * Loads {@code trade} rows in SUCCESS state for the given company, optionally restricted to the
	 * supplied order ids. Result maps contain {@code orderId} and {@code tradeNo} for displaying the
	 * trade number (e.g. order sequence column) in refund export output.
	 */
	List<Map<String, Object>> selectTradeIndexRowsForRefundExport(
			@Param("companyId") String companyId, @Param("orderIds") List<Long> orderIds);
}

