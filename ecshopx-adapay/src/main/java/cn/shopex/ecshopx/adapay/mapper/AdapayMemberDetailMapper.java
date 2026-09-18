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

package cn.shopex.ecshopx.adapay.mapper;

import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMemberUpdateLog;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdapayMemberDetailMapper {

	AdapayMember selectMemberByOperatorTypeAndOperatorIdAndCompany(
			@Param("operatorType") String operatorType,
			@Param("operatorId") long operatorId,
			@Param("companyId") long companyId);

	AdapaySettleAccount selectSettleAccountByMemberIdAndCompany(
			@Param("memberId") long memberId, @Param("companyId") long companyId);

	AdapayCorpMember selectCorpMemberByMemberIdAndCompany(
			@Param("memberId") long memberId, @Param("companyId") long companyId);

	String selectConcatAreaNamesByProvAndAreaCode(
			@Param("provCode") String provCode, @Param("areaCode") String areaCode);

	AdapayMemberUpdateLog selectLatestUpdateLogByMemberIdForWaitDataTranf(@Param("memberId") long memberId);
}
