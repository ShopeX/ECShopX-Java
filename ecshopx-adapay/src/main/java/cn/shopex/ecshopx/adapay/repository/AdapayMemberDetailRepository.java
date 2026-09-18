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

package cn.shopex.ecshopx.adapay.repository;

import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMemberUpdateLog;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberDetailMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AdapayMemberDetailRepository {

	private final AdapayMemberDetailMapper adapayMemberDetailMapper;

	public AdapayMemberDetailRepository(AdapayMemberDetailMapper adapayMemberDetailMapper) {
		this.adapayMemberDetailMapper = adapayMemberDetailMapper;
	}

	public Optional<AdapayMember> selectMemberByOperatorTypeAndOperatorIdAndCompany(
			String operatorType, long operatorId, long companyId) {
		AdapayMember row =
				adapayMemberDetailMapper.selectMemberByOperatorTypeAndOperatorIdAndCompany(
						operatorType, operatorId, companyId);
		return Optional.ofNullable(row);
	}

	public Optional<AdapaySettleAccount> selectSettleAccountByMemberIdAndCompany(long memberId, long companyId) {
		AdapaySettleAccount row =
				adapayMemberDetailMapper.selectSettleAccountByMemberIdAndCompany(memberId, companyId);
		return Optional.ofNullable(row);
	}

	public Optional<AdapayCorpMember> selectCorpMemberByMemberIdAndCompany(long memberId, long companyId) {
		AdapayCorpMember row = adapayMemberDetailMapper.selectCorpMemberByMemberIdAndCompany(memberId, companyId);
		return Optional.ofNullable(row);
	}

	public Optional<String> selectConcatAreaNamesByProvAndAreaCode(String provCode, String areaCode) {
		if (provCode == null || areaCode == null) {
			return Optional.empty();
		}
		String s = adapayMemberDetailMapper.selectConcatAreaNamesByProvAndAreaCode(provCode, areaCode);
		return Optional.ofNullable(s);
	}

	public Optional<AdapayMemberUpdateLog> selectLatestUpdateLogByMemberIdForWaitDataTranf(long memberId) {
		AdapayMemberUpdateLog row =
				adapayMemberDetailMapper.selectLatestUpdateLogByMemberIdForWaitDataTranf(memberId);
		return Optional.ofNullable(row);
	}
}
