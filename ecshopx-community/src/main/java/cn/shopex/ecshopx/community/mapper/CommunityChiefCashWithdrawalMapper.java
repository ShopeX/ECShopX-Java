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

import cn.shopex.ecshopx.community.domain.CommunityChiefCashWithdrawal;
import cn.shopex.ecshopx.community.domain.dto.ChiefRebateAggRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityChiefCashWithdrawalMapper extends BaseMapper<CommunityChiefCashWithdrawal> {

	long countWithdrawalList(
			@Param("companyId") long companyId,
			@Param("distributorId") int distributorId,
			@Param("mobileEnc") String mobileEnc,
			@Param("status") String status);

	List<CommunityChiefCashWithdrawal> selectWithdrawalList(
			@Param("companyId") long companyId,
			@Param("distributorId") int distributorId,
			@Param("mobileEnc") String mobileEnc,
			@Param("status") String status,
			@Param("offset") int offset,
			@Param("limit") int limit);

	List<ChiefRebateAggRow> selectChiefRebateRebateTotalByChiefIds(
			@Param("companyId") long companyId, @Param("chiefIds") List<Long> chiefIds);

	List<ChiefRebateAggRow> selectChiefRebateCloseRebateByChiefIds(
			@Param("companyId") long companyId,
			@Param("chiefIds") List<Long> chiefIds,
			@Param("nowSeconds") long nowSeconds);

	List<ChiefRebateAggRow> selectChiefRebateApplyMoneyByChiefIds(
			@Param("companyId") long companyId, @Param("chiefIds") List<Long> chiefIds);

	List<ChiefRebateAggRow> selectChiefRebateSuccessMoneyByChiefIds(
			@Param("companyId") long companyId, @Param("chiefIds") List<Long> chiefIds);

	Map<String, Object> selectCashWithdrawalCountAggregates(
			@Param("companyId") long companyId, @Param("distributorId") int distributorId);
}
