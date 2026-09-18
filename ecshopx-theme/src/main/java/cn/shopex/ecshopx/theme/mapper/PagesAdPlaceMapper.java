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

package cn.shopex.ecshopx.theme.mapper;

import cn.shopex.ecshopx.theme.domain.PagesAdPlace;
import cn.shopex.ecshopx.theme.service.dto.PagesAdPlaceListCriteria;
import cn.shopex.ecshopx.theme.service.dto.PagesAdPlaceWxappListRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PagesAdPlaceMapper extends BaseMapper<PagesAdPlace> {

	long countByListCriteria(@Param("c") PagesAdPlaceListCriteria c);

	List<PagesAdPlace> selectPageByListCriteria(
			@Param("c") PagesAdPlaceListCriteria c, @Param("offset") long offset, @Param("limit") int limit);

	List<PagesAdPlace> selectAllByListCriteria(@Param("c") PagesAdPlaceListCriteria c);

	List<PagesAdPlaceWxappListRow> selectWxappAdListRowsByCriteria(@Param("c") PagesAdPlaceListCriteria c);

	PagesAdPlace selectOneForAdminGetInfo(
			@Param("companyId") long companyId,
			@Param("id") long id,
			@Param("sourceId") Long sourceId);
}
