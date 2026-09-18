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

import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.dto.LimitItemTagCheckRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LimitItemPromotionsMapper extends BaseMapper<LimitItemPromotions> {

	List<LimitItemTagCheckRow> selectActiveRowsForTagActivityCheck(@Param("companyId") long companyId, @Param("now") int now,
			@Param("tagIds") List<Long> tagIds, @Param("requestItemIds") List<Long> requestItemIds,
			@Param("brandIds") List<Long> brandIds, @Param("categoryIds") List<Long> categoryIds);

	List<LimitItemPromotions> selectWxappActiveLimitItemsForDetail(
			@Param("companyId") long companyId,
			@Param("itemIds") List<Long> itemIds,
			@Param("distributorId") long distributorId,
			@Param("now") int now);

	/**
	 * 与 {@link #selectWxappActiveLimitItemsForDetail} 相同时间窗与门店过滤，按 {@code item_type} + {@code item_id IN} 查询（类目 / 标签 / 品牌限购行）。
	 */
	List<LimitItemPromotions> selectWxappActiveLimitItemsForDetailByType(
			@Param("companyId") long companyId,
			@Param("itemIds") List<Long> itemIds,
			@Param("itemType") String itemType,
			@Param("distributorId") long distributorId,
			@Param("now") int now);
}
