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

import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeEmailVcodeEnterpriseRow;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeFrontCheckListRow;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeFrontEmailEnterpriseRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeFrontCheckQueryMapper {

	List<Long> selectEnterpriseIdsByActivity(@Param("companyId") long companyId, @Param("activityId") long activityId);

	EmployeeFrontEmailEnterpriseRow selectFirstEnterpriseByEmailSuffix(
			@Param("companyId") long companyId,
			@Param("suffix") String suffix,
			@Param("enterpriseIds") List<Long> enterpriseIds);

	EmployeeEmailVcodeEnterpriseRow selectFirstEnterpriseForEmailVcode(
			@Param("companyId") long companyId,
			@Param("suffix") String suffix,
			@Param("distributorId") Integer distributorId,
			@Param("enterpriseId") Long enterpriseId);

	long countEmployeesWithRel(
			@Param("companyId") long companyId,
			@Param("authType") String authType,
			@Param("enterpriseIds") List<Long> enterpriseIds,
			@Param("singleEnterpriseId") Long singleEnterpriseId,
			@Param("distributorId") Integer distributorId,
			@Param("mobileEncrypted") String mobileEncrypted,
			@Param("account") String account,
			@Param("authCode") String authCode,
			@Param("requireUnboundUser") boolean requireUnboundUser);

	List<EmployeeFrontCheckListRow> selectEmployeesWithRel(
			@Param("companyId") long companyId,
			@Param("authType") String authType,
			@Param("enterpriseIds") List<Long> enterpriseIds,
			@Param("singleEnterpriseId") Long singleEnterpriseId,
			@Param("distributorId") Integer distributorId,
			@Param("mobileEncrypted") String mobileEncrypted,
			@Param("account") String account,
			@Param("authCode") String authCode,
			@Param("requireUnboundUser") boolean requireUnboundUser);
}
