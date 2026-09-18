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

import cn.shopex.ecshopx.point.domain.PointMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointMemberMapper extends BaseMapper<PointMember> {

	@Insert(
			"INSERT INTO point_member (user_id, company_id, point) VALUES (#{userId}, #{companyId}, #{delta}) "
					+ "ON DUPLICATE KEY UPDATE point = point + #{delta}")
	void addPointDelta(@Param("userId") long userId, @Param("companyId") long companyId, @Param("delta") int delta);

	@Update(
			"UPDATE point_member SET point = point - #{delta} WHERE user_id = #{userId} AND company_id = #{companyId} "
					+ "AND point >= #{delta}")
	int subtractPointIfEnough(@Param("userId") long userId, @Param("companyId") long companyId, @Param("delta") int delta);
}
