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

import cn.shopex.ecshopx.companys.dto.SelfDeliveryStaffAccountListRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {@code self_delivery_staff} 写读（与 distribution 模块同库），避免 companys→distribution 的 Maven 环依赖。
 */
@Mapper
public interface EmployeeSelfDeliveryStaffMapper {

	long countByStaffNoAndCompanyId(@Param("staffNo") String staffNo, @Param("companyId") long companyId);

	long countByStaffNoAndCompanyIdExcludingOperatorId(
			@Param("staffNo") String staffNo,
			@Param("companyId") long companyId,
			@Param("excludeOperatorId") long excludeOperatorId);

	int insertStaffRow(
			@Param("companyId") long companyId,
			@Param("operatorId") long operatorId,
			@Param("distributorId") int distributorId,
			@Param("shopId") int shopId,
			@Param("staffAttribute") String staffAttribute,
			@Param("staffNo") String staffNo,
			@Param("staffType") String staffType,
			@Param("paymentMethod") String paymentMethod,
			@Param("paymentFee") int paymentFee,
			@Param("created") long created,
			@Param("updated") long updated);

	int deleteStaffRowByCompanyIdAndOperatorId(
			@Param("companyId") long companyId, @Param("operatorId") long operatorId);

	int updateStaffRowByOperatorId(
			@Param("companyId") long companyId,
			@Param("operatorId") long operatorId,
			@Param("distributorId") int distributorId,
			@Param("shopId") int shopId,
			@Param("staffAttribute") String staffAttribute,
			@Param("staffNo") String staffNo,
			@Param("staffType") String staffType,
			@Param("paymentMethod") String paymentMethod,
			@Param("paymentFee") int paymentFee,
			@Param("updated") long updated);

	List<Long> selectOperatorIdsByCompanyIdAndPaymentMethod(
			@Param("companyId") long companyId, @Param("paymentMethod") String paymentMethod);

	List<Long> selectOperatorIdsByCompanyIdAndStaffType(
			@Param("companyId") long companyId, @Param("staffType") String staffType);

	List<SelfDeliveryStaffAccountListRow> selectAccountListRowsByCompanyIdAndOperatorIds(
			@Param("companyId") long companyId, @Param("operatorIds") List<Long> operatorIds);
}
