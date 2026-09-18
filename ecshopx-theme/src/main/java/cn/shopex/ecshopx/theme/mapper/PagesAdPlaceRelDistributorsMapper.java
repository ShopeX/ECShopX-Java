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

import cn.shopex.ecshopx.theme.domain.PagesAdPlaceRelDistributors;
import cn.shopex.ecshopx.theme.mapper.dto.PagesAdPlaceDistributorJoinRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PagesAdPlaceRelDistributorsMapper extends BaseMapper<PagesAdPlaceRelDistributors> {

	List<PagesAdPlaceDistributorJoinRow> selectRelDistributorsByAdPlaceIds(@Param("ids") List<Long> ids);
}
