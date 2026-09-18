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

import cn.shopex.ecshopx.orders.domain.CompanyRelLogistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompanyRelLogisticsMapper extends BaseMapper<CompanyRelLogistics> {

	@Select(
			"SELECT COUNT(*) FROM company_rel_logistics WHERE company_id = #{companyId} "
					+ "AND distributor_id = #{distributorId} AND supplier_id = #{supplierId}")
	long countTradeLogisticsList(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("supplierId") long supplierId);

	@Select(
			"SELECT kuaidi_code AS `value`, corp_name AS name FROM company_rel_logistics WHERE company_id = #{companyId} "
					+ "AND distributor_id = #{distributorId} AND supplier_id = #{supplierId} "
					+ "ORDER BY id DESC LIMIT 100")
	List<Map<String, Object>> selectTradeLogisticsListAsKuaidi100(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("supplierId") long supplierId);

	@Select(
			"SELECT corp_code AS `value`, corp_name AS name FROM company_rel_logistics WHERE company_id = #{companyId} "
					+ "AND distributor_id = #{distributorId} AND supplier_id = #{supplierId} "
					+ "ORDER BY id DESC LIMIT 100")
	List<Map<String, Object>> selectTradeLogisticsListAsCorpCode(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("supplierId") long supplierId);
}
