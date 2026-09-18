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

package cn.shopex.ecshopx.members.mapper.bind;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BindUserSalespersonRelShopReadMapper {

	@Select("SELECT COUNT(1) FROM shop_rel_salesperson WHERE salesperson_id = #{salespersonId} "
			+ "AND company_id = #{companyId} AND store_type = 'distributor' "
			+ "AND (#{shopId} IS NULL OR shop_id = #{shopId})")
	int countShopRelSalesperson(
			@Param("salespersonId") long salespersonId,
			@Param("companyId") long companyId,
			@Param("shopId") Long shopId);

	@Select("SELECT name FROM shop_salesperson WHERE salesperson_id = #{salespersonId} LIMIT 1")
	String selectSalespersonNameById(@Param("salespersonId") long salespersonId);
}
