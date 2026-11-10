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

package cn.shopex.ecshopx.goods.mapper;

import cn.shopex.ecshopx.goods.search.index.SearchIndexStoreItemKey;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 商品搜索索引物化：店品键分页。 */
@Mapper
public interface SearchIndexSourceMapper {

	@Select(
			"""
			<script>
			SELECT distributor_id AS distributorId, default_item_id AS defaultItemId FROM (
			  SELECT distributor_id,
			    CASE WHEN default_item_id IS NOT NULL AND default_item_id &gt; 0 THEN default_item_id ELSE item_id END AS default_item_id
			  FROM distribution_distributor_items
			  WHERE company_id = #{companyId}
			  <if test="distributorId != null">AND distributor_id = #{distributorId}</if>
			  UNION ALL
			  SELECT 0 AS distributor_id, item_id AS default_item_id
			  FROM items
			  WHERE company_id = #{companyId}
			    AND (default_item_id IS NULL OR default_item_id = 0 OR default_item_id = item_id)
			  <if test="distributorId != null and distributorId &gt; 0">AND 1 = 0</if>
			) keys
			WHERE (default_item_id &gt; #{afterDefaultItemId})
			   OR (default_item_id = #{afterDefaultItemId} AND distributor_id &gt; #{afterDistributorId})
			GROUP BY distributor_id, default_item_id
			ORDER BY default_item_id, distributor_id
			LIMIT #{limit}
			</script>
			""")
	List<SearchIndexStoreItemKey> selectStoreItemKeys(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("afterDefaultItemId") long afterDefaultItemId,
			@Param("afterDistributorId") long afterDistributorId,
			@Param("limit") int limit);
}
