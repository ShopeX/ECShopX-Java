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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WxappKaquanUserDiscountCountMapper {

	@Select(
			"SELECT COUNT(1) FROM kaquan_user_discount WHERE company_id = #{companyId} AND user_id = #{userId} "
					+ "AND status = 1 AND end_date >= #{nowEpoch}")
	Long countValidForWxappOrderStats(
			@Param("companyId") long companyId, @Param("userId") long userId, @Param("nowEpoch") int nowEpoch);
}
