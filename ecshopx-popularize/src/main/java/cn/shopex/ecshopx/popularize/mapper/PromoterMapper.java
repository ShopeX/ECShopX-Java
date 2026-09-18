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

package cn.shopex.ecshopx.popularize.mapper;

import cn.shopex.ecshopx.popularize.domain.Promoter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PromoterMapper extends BaseMapper<Promoter> {

	@Select("<script>"
			+ "SELECT pid AS pid, COUNT(1) AS cnt FROM popularize_promoter WHERE company_id = #{companyId} AND pid IN "
			+ "<foreach collection='pids' item='p' open='(' separator=',' close=')'>#{p}</foreach> "
			+ "GROUP BY pid"
			+ "</script>")
	List<Map<String, Object>> countDirectChildrenByPidList(@Param("companyId") long companyId,
			@Param("pids") List<Long> pids);

	List<Long> selectPromoterDownstreamUserIdsForAdminMemberList(
			@Param("companyId") long companyId, @Param("mobileEnc") String mobileEnc);

	@Select(
			"SELECT COUNT(1) FROM popularize_promoter WHERE company_id = #{companyId} AND pid = #{parentMemberUserId}")
	long countDirectChildrenByParentMemberUserId(
			@Param("companyId") long companyId, @Param("parentMemberUserId") long parentMemberUserId);

	@Select(
			"SELECT COALESCE(SUM(CAST(IFNULL(o.total_fee, '0') AS UNSIGNED)), 0) FROM popularize_promoter c "
					+ "INNER JOIN orders_normal_orders o ON o.company_id = c.company_id AND o.user_id = c.user_id "
					+ "WHERE c.company_id = #{companyId} AND c.pid = #{parentMemberUserId}")
	long sumChildOrdersTotalFeeMinorForParentMember(
			@Param("companyId") long companyId, @Param("parentMemberUserId") long parentMemberUserId);
}
