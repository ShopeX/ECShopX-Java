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

import cn.shopex.ecshopx.companys.dto.SupplierOperatorNameRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SupplierOperatorReadMapper {

	List<Long> selectOperatorIdsByCompanyIdAndSupplierNameLike(
			@Param("companyId") long companyId, @Param("supplierName") String supplierName);

	List<SupplierOperatorNameRow> selectSupplierNameRowsByCompanyIdAndOperatorIds(
			@Param("companyId") long companyId, @Param("operatorIds") List<Long> operatorIds);

	Integer selectIsCheckByCompanyIdAndOperatorId(
			@Param("companyId") long companyId, @Param("operatorId") long operatorId);
}
