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

import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityItemsMapper extends BaseMapper<ActivityItems> {

	@Update(
			"UPDATE employee_purchase_activity_items SET activity_store = activity_store - #{num} "
					+ "WHERE company_id = #{companyId} AND activity_id = #{activityId} AND item_id = #{itemId} "
					+ "AND activity_store >= #{num}")
	int minusActivityItemStore(
			@Param("companyId") long companyId,
			@Param("activityId") long activityId,
			@Param("itemId") long itemId,
			@Param("num") int num);

	@Update(
			"UPDATE employee_purchase_activity_items SET activity_store = activity_store + #{num} "
					+ "WHERE company_id = #{companyId} AND activity_id = #{activityId} AND item_id = #{itemId}")
	int addActivityItemStore(
			@Param("companyId") long companyId,
			@Param("activityId") long activityId,
			@Param("itemId") long itemId,
			@Param("num") int num);

	@Select(
			"SELECT ai.goods_id FROM employee_purchase_activity_items ai "
					+ "INNER JOIN items i ON ai.item_id = i.item_id "
					+ "WHERE ai.company_id = #{companyId} AND ai.activity_id = #{activityId} "
					+ "AND i.goods_bn = #{goodsBn} LIMIT 1")
	Long selectGoodsIdByActivityAndGoodsBn(
			@Param("companyId") long companyId,
			@Param("activityId") long activityId,
			@Param("goodsBn") String goodsBn);
}
