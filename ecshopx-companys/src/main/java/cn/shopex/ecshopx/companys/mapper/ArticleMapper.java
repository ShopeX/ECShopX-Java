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

import cn.shopex.ecshopx.companys.domain.Article;
import cn.shopex.ecshopx.companys.mapper.dto.ArticleProvinceGroupRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

	@Select(
			"""
			SELECT a.province AS province, a.regions_id AS regionsId
			FROM companys_article a
			INNER JOIN (
			    SELECT province, MIN(article_id) AS pick_article_id
			    FROM companys_article
			    WHERE company_id = #{companyId}
			    GROUP BY province
			) t ON a.company_id = #{companyId}
			   AND a.province = t.province
			   AND a.article_id = t.pick_article_id
			""")
	List<ArticleProvinceGroupRow> selectProvinceRegionsGroupByProvince(@Param("companyId") long companyId);
}
