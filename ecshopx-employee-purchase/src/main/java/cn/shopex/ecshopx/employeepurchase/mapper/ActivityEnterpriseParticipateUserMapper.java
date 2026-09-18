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

import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseParticipateUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActivityEnterpriseParticipateUserMapper extends BaseMapper<ActivityEnterpriseParticipateUser> {

	@Insert(
			"""
			INSERT IGNORE INTO employee_purchase_activity_enterprise_participate_user
			(company_id, activity_id, enterprise_id, user_id, created)
			VALUES (#{companyId}, #{activityId}, #{enterpriseId}, #{userId}, #{created})
			""")
	int insertIgnore(
			@Param("companyId") long companyId,
			@Param("activityId") long activityId,
			@Param("enterpriseId") long enterpriseId,
			@Param("userId") long userId,
			@Param("created") int created);
}
