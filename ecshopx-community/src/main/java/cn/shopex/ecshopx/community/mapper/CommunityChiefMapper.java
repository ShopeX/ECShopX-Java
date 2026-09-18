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

package cn.shopex.ecshopx.community.mapper;

import cn.shopex.ecshopx.community.domain.CommunityChief;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityChiefMapper extends BaseMapper<CommunityChief> {

	long countChiefListByDistributorJoin(
			@Param("distributorIdFilter") Long distributorIdFilter,
			@Param("companyId") long companyId,
			@Param("chiefNameOrNull") String chiefNameOrNull,
			@Param("chiefMobileOrNull") String chiefMobileOrNull);

	List<Map<String, Object>> selectChiefListByDistributorJoin(
			@Param("distributorIdFilter") Long distributorIdFilter,
			@Param("companyId") long companyId,
			@Param("chiefNameOrNull") String chiefNameOrNull,
			@Param("chiefMobileOrNull") String chiefMobileOrNull,
			@Param("offset") int offset,
			@Param("limit") int limit);

	/**
	 * 团长详情：固定 company + chief_id + distributor_id，最多一行（无行时 {@code null}）。
	 */
	Map<String, Object> selectChiefDetailByDistributorJoin(
			@Param("distributorId") long distributorId,
			@Param("companyId") long companyId,
			@Param("chiefId") String chiefId);
}
