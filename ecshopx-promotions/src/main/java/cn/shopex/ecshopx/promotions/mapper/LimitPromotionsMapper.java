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

package cn.shopex.ecshopx.promotions.mapper;

import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.dto.LimitPromotionNameRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LimitPromotionsMapper extends BaseMapper<LimitPromotions> {

	List<LimitPromotionNameRow> selectLimitNamesByLimitIds(@Param("ids") List<Long> limitIds);

	List<LimitPromotions> selectOverlappingLimits(
			@Param("companyId") long companyId,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd,
			@Param("excludeLimitId") Long excludeLimitId);

	List<Long> selectLimitIdsCategorySql1(
			@Param("companyId") long companyId,
			@Param("categoryIds") List<Long> categoryIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsCategorySql2(
			@Param("companyId") long companyId,
			@Param("categoryIds") List<Long> categoryIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsCategorySql3(
			@Param("companyId") long companyId,
			@Param("categoryIds") List<Long> categoryIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsCategorySql4(
			@Param("companyId") long companyId,
			@Param("categoryIds") List<Long> categoryIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsTagsSql1(
			@Param("companyId") long companyId,
			@Param("tagIds") List<Long> tagIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsTagsSql2(
			@Param("companyId") long companyId,
			@Param("tagIds") List<Long> tagIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsTagsSql3(
			@Param("companyId") long companyId,
			@Param("tagIds") List<Long> tagIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsTagsSql4(
			@Param("companyId") long companyId,
			@Param("tagIds") List<Long> tagIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsBrandSql1(
			@Param("companyId") long companyId,
			@Param("brandIds") List<Long> brandIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsBrandSql2(
			@Param("companyId") long companyId,
			@Param("brandIds") List<Long> brandIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsBrandSql3(
			@Param("companyId") long companyId,
			@Param("brandIds") List<Long> brandIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	List<Long> selectLimitIdsBrandSql4(
			@Param("companyId") long companyId,
			@Param("brandIds") List<Long> brandIds,
			@Param("overlapStart") long overlapStart,
			@Param("windowEnd") long windowEnd);

	long countAdminLimitList(
			@Param("companyId") long companyId,
			@Param("status") String status,
			@Param("sourceType") String sourceType,
			@Param("sourceIdFromQuery") long sourceIdFromQuery,
			@Param("now") long now);

	List<LimitPromotions> selectAdminLimitListPage(
			@Param("companyId") long companyId,
			@Param("status") String status,
			@Param("sourceType") String sourceType,
			@Param("sourceIdFromQuery") long sourceIdFromQuery,
			@Param("now") long now,
			@Param("offset") long offset,
			@Param("limit") int limit);
}
