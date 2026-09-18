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

import java.util.LinkedHashMap;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeePurchaseUserActivitiesQueryMapper {

	long countUserActivities(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("now") int now,
			@Param("enterpriseId") long enterpriseId,
			@Param("activityName") String activityName,
			@Param("activityId") Long activityId);

	List<LinkedHashMap<String, Object>> selectUserActivities(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("now") int now,
			@Param("enterpriseId") long enterpriseId,
			@Param("activityName") String activityName,
			@Param("activityId") Long activityId,
			@Param("offset") long offset,
			@Param("pageSize") int pageSize);
}
