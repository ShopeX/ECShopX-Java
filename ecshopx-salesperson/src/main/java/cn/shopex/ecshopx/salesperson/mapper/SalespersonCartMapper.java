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

package cn.shopex.ecshopx.salesperson.mapper;

import cn.shopex.ecshopx.salesperson.domain.SalespersonCart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SalespersonCartMapper extends BaseMapper<SalespersonCart> {

	@Select(
			"SELECT COUNT(*) AS cart_count, SUM(num) AS item_count "
					+ "FROM companys_saleperson_cart "
					+ "WHERE company_id = #{companyId} AND salesperson_id = #{salespersonId}")
	Map<String, Object> selectCartCountAggregate(
			@Param("companyId") long companyId, @Param("salespersonId") long salespersonId);
}
