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

package cn.shopex.ecshopx.kujiale.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KujialeDesignerWorksItemsQueryMapper {

	List<Map<String, Object>> selectPageRows(
			@Param("companyId") long companyId,
			@Param("itemName") String itemName,
			@Param("itemBn") String itemBn,
			@Param("goodsBn") String goodsBn,
			@Param("designId") String designId,
			@Param("designName") String designName,
			@Param("approveStatus") String[] approveStatus,
			@Param("itemCategory") String[] itemCategory,
			@Param("limit") Integer limit,
			@Param("offset") Integer offset,
			@Param("applyPaging") boolean applyPaging);

	long countDistinct(
			@Param("companyId") long companyId,
			@Param("itemName") String itemName,
			@Param("itemBn") String itemBn,
			@Param("goodsBn") String goodsBn,
			@Param("designId") String designId,
			@Param("designName") String designName,
			@Param("approveStatus") String[] approveStatus,
			@Param("itemCategory") String[] itemCategory);

	List<Map<String, Object>> selectSalesCategories(@Param("itemIds") List<Long> itemIds);

	List<Map<String, Object>> selectItemTagRows(
			@Param("companyId") long companyId,
			@Param("itemIds") List<Long> itemIds);

	List<Map<String, Object>> selectDesignTagNames(@Param("designIds") List<String> designIds);
}
