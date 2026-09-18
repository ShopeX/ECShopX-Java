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

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.ScheduleAutoDoneGroupTeamRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromotionGroupsTeamMapper extends BaseMapper<PromotionGroupsTeam> {

	long countWxappGroupOrderListByMember(
			@Param("companyId") long companyId,
			@Param("memberId") long memberId,
			@Param("teamStatus") Long teamStatus,
			@Param("groupGoodsType") String groupGoodsType);

	List<LinkedHashMap<String, Object>> selectWxappGroupOrderListByMemberPage(
			@Param("companyId") long companyId,
			@Param("memberId") long memberId,
			@Param("teamStatus") Long teamStatus,
			@Param("groupGoodsType") String groupGoodsType,
			@Param("offset") int offset,
			@Param("limit") int limit);

	long countScheduleAutoDoneGroup(
			@Param("nowSec") long nowSec, @Param("secondWindow") int secondWindow);

	List<ScheduleAutoDoneGroupTeamRow> listScheduleAutoDoneGroupPage(
			@Param("nowSec") long nowSec,
			@Param("secondWindow") int secondWindow,
			@Param("offset") int offset,
			@Param("pageSize") int pageSize);

	long countScheduleNoStoreAutoDoneGroup();

	List<ScheduleAutoDoneGroupTeamRow> listScheduleNoStoreAutoDoneGroupPage(
			@Param("offset") int offset, @Param("pageSize") int pageSize);

	List<LinkedHashMap<String, Object>> selectGroupsTeamByItemsPage(
			@Param("companyId") long companyId,
			@Param("actId") long actId,
			@Param("offset") int offset,
			@Param("limit") int limit);
}
