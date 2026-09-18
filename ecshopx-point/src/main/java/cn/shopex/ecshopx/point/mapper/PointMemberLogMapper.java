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

package cn.shopex.ecshopx.point.mapper;

import cn.shopex.ecshopx.point.domain.PointMemberLog;
import cn.shopex.ecshopx.point.domain.PointMemberPointCountSummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointMemberLogMapper extends BaseMapper<PointMemberLog> {

	@Select(
			"""
			SELECT
			  (COALESCE(SUM(income), 0) - COALESCE(SUM(outcome), 0)) AS can_use,
			  COALESCE(SUM(income), 0) AS total,
			  COALESCE(SUM(outcome), 0) AS used
			FROM point_member_log
			WHERE company_id = #{companyId}
			""")
	PointMemberPointCountSummary selectPointCountSummaryByCompanyId(@Param("companyId") long companyId);

	/**
	 * 本月内 journal_type=7 正入账汇总（与 PHP check_point_income 一致：自然月、income&gt;0）。
	 */
	@Select(
			"""
			SELECT COALESCE(SUM(income), 0)
			FROM point_member_log
			WHERE user_id = #{userId}
			  AND company_id = #{companyId}
			  AND journal_type = 7
			  AND income > 0
			  AND created >= #{startInclusive}
			  AND created <= #{endInclusive}
			""")
	Integer selectSumIncomeOrderBonusInRange(
			@Param("userId") long userId,
			@Param("companyId") long companyId,
			@Param("startInclusive") int startInclusive,
			@Param("endInclusive") int endInclusive);
}
