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

package cn.shopex.ecshopx.datacube.mapper;

import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDailyStatLineKey;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoodsDailyStatisticsQueryMapper {

	long countKernelJoinWindow(
			@Param("createTimeStart") long createTimeStart,
			@Param("createTimeEnd") long createTimeEnd,
			@Param("tradeStates") List<String> tradeStates);

	List<GoodsDailyStatLineKey> selectKernelJoinPage(
			@Param("createTimeStart") long createTimeStart,
			@Param("createTimeEnd") long createTimeEnd,
			@Param("tradeStates") List<String> tradeStates,
			@Param("offset") int offset,
			@Param("limit") int limit);

	long countEmployeePurchaseJoinWindow(
			@Param("createTimeStart") long createTimeStart,
			@Param("createTimeEnd") long createTimeEnd,
			@Param("tradeStates") List<String> tradeStates,
			@Param("actId") long actId);

	List<GoodsDailyStatLineKey> selectEmployeePurchaseJoinPage(
			@Param("createTimeStart") long createTimeStart,
			@Param("createTimeEnd") long createTimeEnd,
			@Param("tradeStates") List<String> tradeStates,
			@Param("actId") long actId,
			@Param("offset") int offset,
			@Param("limit") int limit);
}
