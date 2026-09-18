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

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NostoresZitiDistributorQueryMapper {

	@Select(
			"""
			SELECT distributor_id
			FROM distribution_distributor
			WHERE company_id = #{companyId}
			AND (is_valid = 'true' OR is_valid = 1 OR LOWER(TRIM(CAST(is_valid AS CHAR))) IN ('true','1'))
			AND (is_ziti = 1 OR LOWER(TRIM(CAST(is_ziti AS CHAR))) IN ('true','1'))
			ORDER BY distributor_id
			""")
	List<Long> listZitiDistributorIds(@Param("companyId") long companyId);
}
