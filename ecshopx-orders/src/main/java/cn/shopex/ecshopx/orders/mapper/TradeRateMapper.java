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

import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.service.admin.dto.TradeRateAdminListQuery;
import cn.shopex.ecshopx.orders.service.admin.dto.TradeRateListRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TradeRateMapper extends BaseMapper<TradeRate> {

	BigDecimal selectAvgStarByCompanyAndDistributor(
			@Param("companyId") long companyId, @Param("distributorId") long distributorId);

	List<Map<String, Object>> selectAvgStarBatchByCompanyAndDistributorIds(
			@Param("companyId") long companyId, @Param("distributorIds") List<Long> distributorIds);

	long countTradeRateListAdminSimple(
			@Param("companyId") long companyId, @Param("q") TradeRateAdminListQuery q);

	long countTradeRateListAdminJoin(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("q") TradeRateAdminListQuery q);

	List<TradeRateListRow> selectTradeRateListAdminPageSimple(
			@Param("companyId") long companyId,
			@Param("q") TradeRateAdminListQuery q,
			@Param("offset") long offset,
			@Param("limit") long limit);

	List<TradeRateListRow> selectTradeRateListAdminPageJoin(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("q") TradeRateAdminListQuery q,
			@Param("offset") long offset,
			@Param("limit") long limit);

	String selectGoodsItemNameByItemIdAndCompanyId(
			@Param("itemId") long itemId, @Param("companyId") long companyId);

	String selectPointsmallItemNameByItemIdAndCompanyId(
			@Param("itemId") long itemId, @Param("companyId") long companyId);

	Map<String, Object> selectItemsRowMapByItemIdAndCompanyId(
			@Param("itemId") long itemId, @Param("companyId") long companyId);

	Map<String, Object> selectPointsmallItemsRowMapByItemIdAndCompanyId(
			@Param("itemId") long itemId, @Param("companyId") long companyId);
}
