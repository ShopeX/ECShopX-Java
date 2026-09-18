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

package cn.shopex.ecshopx.pointsmall.mapper;

import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsmallItemsMapper extends BaseMapper<PointsmallItems> {

	@Select("<script>SELECT default_item_id AS did, COALESCE(SUM(store), 0) AS total FROM pointsmall_items WHERE company_id = #{companyId} "
			+ "AND default_item_id IN <foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach> "
			+ "GROUP BY default_item_id</script>")
	List<Map<String, Object>> sumStoreByDefaultItemIds(@Param("companyId") long companyId, @Param("ids") List<Long> ids);

	@Select("SELECT item_id, price, store, cost_price, item_bn, barcode, market_price, point, pay_class, item_unit, volume, "
			+ "approve_status, is_default, weight FROM pointsmall_items WHERE company_id = #{companyId} AND default_item_id = #{defaultItemId} "
			+ "ORDER BY item_id ASC")
	List<PointsmallItems> listSkusByCompanyAndDefaultItemId(@Param("companyId") long companyId, @Param("defaultItemId") long defaultItemId);
}
