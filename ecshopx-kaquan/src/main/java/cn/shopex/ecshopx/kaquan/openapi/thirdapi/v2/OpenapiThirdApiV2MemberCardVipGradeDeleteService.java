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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OpenapiThirdApiV2MemberCardVipGradeDeleteService {

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV2MemberCardVipGradeDeleteService(
			VipGradeMapper vipGradeMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void executeOpenapiDelete(long companyId, String vipGradeIdRaw) {
		validateParams(vipGradeIdRaw);

		Long vipGradeId = tryParseLong(vipGradeIdRaw.trim());
		VipGrade existing = vipGradeMapper.selectOne(
				new LambdaQueryWrapper<VipGrade>()
						.eq(VipGrade::getCompanyId, (int) companyId)
						.eq(VipGrade::getVipGradeId, vipGradeId != null ? vipGradeId : -1L));
		if (existing == null) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_VIP_GRADE_NOT_FOUND, "会员付费等级找不到");
		}

		assertNotDefaultGrade(existing);
		assertNoAssociatedMembers(companyId, vipGradeIdRaw);

		transactionTemplate.executeWithoutResult(status -> vipGradeMapper.delete(
				new LambdaQueryWrapper<VipGrade>()
						.eq(VipGrade::getCompanyId, (int) companyId)
						.eq(VipGrade::getVipGradeId, existing.getVipGradeId())));
	}

	private void validateParams(String vipGradeIdRaw) {
		if (vipGradeIdRaw == null || vipGradeIdRaw.isEmpty()) {
			throw paramError();
		}
	}

	private void assertNotDefaultGrade(VipGrade existing) {
		if (Boolean.TRUE.equals(existing.getIsDefault())) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_VIP_GRADE_DELETE_ERROR,
					"会员付费等级删除错误");
		}
	}

	private void assertNoAssociatedMembers(long companyId, String vipGradeIdRaw) {
		long vipGradeIdForQuery = parseVipGradeIdForRelCheck(vipGradeIdRaw);
		Long count = vipGradeRelUserMapper.selectCount(
				new LambdaQueryWrapper<VipGradeRelUser>()
						.eq(VipGradeRelUser::getCompanyId, (int) companyId)
						.eq(VipGradeRelUser::getVipGradeId, vipGradeIdForQuery));
		if (count != null && count > 0) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_VIP_GRADE_DELETE_ERROR,
					"会员付费等级无法删除，该会员付费等级下扔存在关联的会员");
		}
	}

	private static long parseVipGradeIdForRelCheck(String vipGradeIdRaw) {
		try {
			return Long.parseLong(vipGradeIdRaw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long tryParseLong(String raw) {
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			try {
				double d = Double.parseDouble(raw);
				if (d == Math.floor(d)) {
					return (long) d;
				}
			} catch (NumberFormatException ignored) {
				// fall through
			}
			return null;
		}
	}

	private static OpenapiMemberV2FailException paramError() {
		return new OpenapiMemberV2FailException(
				OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误");
	}
}
