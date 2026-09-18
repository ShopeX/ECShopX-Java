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

package cn.shopex.ecshopx.employeepurchase.mapper;

import cn.shopex.ecshopx.employeepurchase.domain.Cart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

	@Select(
			"""
			SELECT COUNT(*) AS cart_count, COALESCE(SUM(num), 0) AS item_count
			FROM employee_purchase_cart
			WHERE company_id = #{companyId}
			  AND user_id = #{userId}
			  AND enterprise_id = #{enterpriseId}
			  AND activity_id = #{activityId}
			""")
	Map<String, Object> selectCartCountSummary(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("enterpriseId") long enterpriseId,
			@Param("activityId") long activityId);
}
