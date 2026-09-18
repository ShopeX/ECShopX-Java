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

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeePurchaseDistributorItemsQueryMapper {

	Integer selectDistributorSelf(@Param("companyId") long companyId, @Param("distributorId") long distributorId);

	Long countItemsForDistributorByItemCategories(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("distributorSelf") Integer distributorSelf,
			@Param("itemCategoryKeys") List<String> itemCategoryKeys);

	List<Long> selectItemIdsForDistributorByItemCategories(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("distributorSelf") Integer distributorSelf,
			@Param("itemCategoryKeys") List<String> itemCategoryKeys);

	Long countItemsForDistributorByItemIds(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("distributorSelf") Integer distributorSelf,
			@Param("itemIds") List<Long> itemIds);
}
