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

import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.repository.dto.RelItemTagNameRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ItemsRelTagsMapper extends BaseMapper<ItemsRelTags> {

	@Select("""
			<script>
			SELECT rel.item_id AS relItemId, tag.tag_name AS tagName
			FROM items_rel_tags rel
			INNER JOIN items_tags tag ON rel.tag_id = tag.tag_id AND rel.company_id = tag.company_id
			WHERE rel.company_id = #{companyId}
			AND rel.item_id IN
			<foreach collection="relItemIds" item="id" open="(" separator="," close=")">#{id}</foreach>
			</script>
			""")
	List<RelItemTagNameRow> selectRelItemIdTagNames(@Param("companyId") long companyId,
			@Param("relItemIds") Collection<Long> relItemIds);
}
