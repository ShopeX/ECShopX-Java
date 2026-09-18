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

package cn.shopex.ecshopx.companys.mapper;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.service.employee.AccountManagementOperatorsFilter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperatorsMapper extends BaseMapper<Operators> {

	long countSelfDeliveryStaffForDeliveryStaffData(
			@Param("companyId") long companyId,
			@Param("username") String username,
			@Param("merchantId") Long merchantId,
			@Param("mobileEncrypted") String mobileEncrypted,
			@Param("distributorIds") List<Long> distributorIds);

	List<Operators> pageSelfDeliveryStaffForDeliveryStaffData(
			@Param("companyId") long companyId,
			@Param("username") String username,
			@Param("merchantId") Long merchantId,
			@Param("mobileEncrypted") String mobileEncrypted,
			@Param("distributorIds") List<Long> distributorIds,
			@Param("offset") int offset,
			@Param("limit") int limit);

	Map<String, Object> selectInfo(
			@Param("loginName") String loginName,
			@Param("mobile") String mobile,
			@Param("operatorType") String operatorType,
			@Param("companyId") Long companyId,
			@Param("passportUid") String passportUid,
			@Param("operatorId") Long operatorId);

	Map<String, Object> getOperatorByMobile(
			@Param("mobile") String mobile, @Param("operatorType") String operatorType);

	long countDistributorMainConflict(
			@Param("companyId") long companyId, @Param("distributorId") long distributorId);

	long countDistributorMainConflictExcludingOperatorId(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("excludeOperatorId") long excludeOperatorId);

	long countExistingByMobileAndOperatorType(
			@Param("mobile") String mobile, @Param("operatorType") String operatorType);

	long countExistingByLoginNameAndOperatorType(
			@Param("loginName") String loginName, @Param("operatorType") String operatorType);

	long countExistingByMobileAndOperatorTypeExcluding(
			@Param("excludeOperatorId") long excludeOperatorId,
			@Param("mobile") String mobile,
			@Param("operatorType") String operatorType);

	long countExistingByLoginNameAndOperatorTypeExcluding(
			@Param("excludeOperatorId") long excludeOperatorId,
			@Param("loginName") String loginName,
			@Param("operatorType") String operatorType);

	long countAccountManagementList(@Param("filter") AccountManagementOperatorsFilter filter);

	List<Operators> pageAccountManagementList(
			@Param("filter") AccountManagementOperatorsFilter filter,
			@Param("offset") int offset,
			@Param("limit") int limit,
			@Param("orderBy") List<AccountManagementOperatorsFilter.OrderBy> orderBy);
}
