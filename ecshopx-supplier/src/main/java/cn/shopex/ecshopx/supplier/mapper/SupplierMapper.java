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

package cn.shopex.ecshopx.supplier.mapper;

import cn.shopex.ecshopx.supplier.domain.Supplier;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

	@Select(
			"""
			SELECT id, company_id, operator_id, add_time
			FROM supplier
			WHERE (is_check IS NULL OR is_check <> 1)
			ORDER BY id ASC
			LIMIT #{limit} OFFSET #{offset}
			""")
	List<Supplier> selectPageForStatementSchedule(@Param("offset") int offset, @Param("limit") int limit);
}
