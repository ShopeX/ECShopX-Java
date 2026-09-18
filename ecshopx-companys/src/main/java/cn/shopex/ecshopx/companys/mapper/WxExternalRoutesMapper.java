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

import cn.shopex.ecshopx.companys.domain.WxExternalRoutes;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WxExternalRoutesMapper extends BaseMapper<WxExternalRoutes> {

	Long countConfigRoutesList(
			@Param("companyId") long companyId,
			@Param("appId") String appId,
			@Param("routeInfoNeedleEscaped") String routeInfoNeedleEscaped);

	List<LinkedHashMap<String, Object>> selectConfigRoutesList(
			@Param("companyId") long companyId,
			@Param("appId") String appId,
			@Param("routeInfoNeedleEscaped") String routeInfoNeedleEscaped,
			@Param("limit") Integer limit,
			@Param("offset") Integer offset,
			@Param("paged") boolean paged);
}
