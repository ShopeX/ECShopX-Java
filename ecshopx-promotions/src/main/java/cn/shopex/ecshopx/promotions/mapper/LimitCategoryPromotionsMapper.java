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

import cn.shopex.ecshopx.promotions.domain.LimitCategoryPromotions;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LimitCategoryPromotionsMapper extends BaseMapper<LimitCategoryPromotions> {

	@Insert(
			"INSERT INTO promotions_limit_category (limit_id, category_id, company_id, category_level) VALUES (#{limitId},#{categoryId},#{companyId},#{categoryLevel})")
	int insertCategoryRow(
			@Param("limitId") long limitId,
			@Param("categoryId") long categoryId,
			@Param("companyId") long companyId,
			@Param("categoryLevel") int categoryLevel);
}
