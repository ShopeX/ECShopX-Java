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

package cn.shopex.ecshopx.popularize.mapper;

import cn.shopex.ecshopx.popularize.domain.PromoterBrokerageStatistics;
import cn.shopex.ecshopx.popularize.dto.PromoterBrokerageCompanySumRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PromoterBrokerageStatisticsMapper extends BaseMapper<PromoterBrokerageStatistics> {

	@Select(
			"""
			SELECT
			  COALESCE(SUM(item_total_price), 0) AS item_total_price,
			  COALESCE(SUM(rebate_total), 0) AS rebate_total,
			  COALESCE(SUM(no_close_rebate), 0) AS no_close_rebate,
			  COALESCE(SUM(cash_withdrawal_rebate), 0) AS cash_withdrawal_rebate,
			  COALESCE(SUM(freeze_cash_withdrawal_rebate), 0) AS freeze_cash_withdrawal_rebate,
			  COALESCE(SUM(recharge_rebate), 0) AS recharge_rebate,
			  COALESCE(SUM(payed_rebate), 0) AS payed_rebate,
			  COALESCE(SUM(point_total), 0) AS point_total
			FROM popularize_brokerage_statistics
			WHERE company_id = #{companyId}
			""")
	PromoterBrokerageCompanySumRow selectCompanySum(@Param("companyId") Long companyId);
}
