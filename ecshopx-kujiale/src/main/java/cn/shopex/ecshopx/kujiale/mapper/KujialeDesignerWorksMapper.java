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

import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorks;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KujialeDesignerWorksMapper extends BaseMapper<KujialeDesignerWorks> {

	@Update(
			"UPDATE kujiale_designer_works SET like_count = like_count + 1 WHERE design_id = #{designId} AND plan_id = #{planId}")
	int incrementLikeCount(@Param("designId") String designId, @Param("planId") String planId);

	@Update(
			"UPDATE kujiale_designer_works SET like_count = like_count - 1 WHERE design_id = #{designId} AND plan_id = #{planId}")
	int decrementLikeCount(@Param("designId") String designId, @Param("planId") String planId);
}
