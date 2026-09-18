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

import cn.shopex.ecshopx.promotions.domain.PaymentOverEndTimeRow;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromotionGroupsTeamMemberMapper extends BaseMapper<PromotionGroupsTeamMember> {

	long countTeamMemberOrderList(
			@Param("companyId") long companyId,
			@Param("teamId") String teamId,
			@Param("startTime") Long startTime,
			@Param("endTime") Long endTime,
			@Param("orderId") String orderId);

	List<LinkedHashMap<String, Object>> selectTeamMemberOrderListPage(
			@Param("companyId") long companyId,
			@Param("teamId") String teamId,
			@Param("startTime") Long startTime,
			@Param("endTime") Long endTime,
			@Param("orderId") String orderId,
			@Param("offset") int offset,
			@Param("limit") int limit);

	/**
	 * 拼团截止时间之后，关联服务订单已支付（或已完成待确认）的团员记录数，用于已结束但团状态未刷新时的进度判断。
	 */
	long countPaidServiceOrdersAfterTeamEnd(
			@Param("companyId") long companyId,
			@Param("teamId") String teamId,
			@Param("teamEndTimeSeconds") long teamEndTimeSeconds);

	List<PaymentOverEndTimeRow> selectPaymentOverEndTimeList(@Param("teamIds") List<String> teamIds);

	List<PromotionGroupsTeamMember> selectGroupTeamSuccessMembers(@Param("teamId") String teamId);
}
