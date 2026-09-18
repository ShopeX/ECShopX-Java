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

import cn.shopex.ecshopx.goods.domain.Items;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ItemsMapper extends BaseMapper<Items> {

	@Select("<script>"
			+ "SELECT COUNT(1) FROM items i INNER JOIN items_rel_attributes r ON i.item_id = r.item_id "
			+ "WHERE i.company_id = #{companyId} AND i.item_category = #{itemCategory} AND r.attribute_id IN "
			+ "<foreach collection='attributeIds' item='id' open='(' separator=',' close=')'>"
			+ "#{id}"
			+ "</foreach>"
			+ "</script>")
	long countItemsLinkedToAttributesInCategory(@Param("companyId") long companyId, @Param("itemCategory") String itemCategory,
			@Param("attributeIds") List<Long> attributeIds);

	@Select("SELECT data_source FROM items WHERE item_id = #{itemId} AND company_id = #{companyId} LIMIT 1")
	String selectDataSourceRaw(@Param("itemId") long itemId, @Param("companyId") long companyId);
}
