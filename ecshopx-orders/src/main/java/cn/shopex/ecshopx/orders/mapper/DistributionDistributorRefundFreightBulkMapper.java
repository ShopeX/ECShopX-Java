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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DistributionDistributorRefundFreightBulkMapper {

	@Update(
			"UPDATE distribution_distributor SET is_refund_freight = #{value} "
					+ "WHERE company_id = #{companyId} AND distribution_type = #{distributionType}")
	int updateIsRefundFreightByCompanyAndDistributionType(
			@Param("companyId") long companyId,
			@Param("distributionType") int distributionType,
			@Param("value") int value);
}
