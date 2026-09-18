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
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OpenapiThirdApiV2MemberCardGradeDeleteService {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MembersMapper membersMapper;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV2MemberCardGradeDeleteService(
			MemberCardGradeMapper memberCardGradeMapper,
			MembersMapper membersMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.membersMapper = membersMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void executeOpenapiDelete(long companyId, String gradeIdRaw) {
		validateParams(gradeIdRaw);
		assertNoAssociatedMembers(companyId, gradeIdRaw);

		String companyIdStr = String.valueOf(companyId);
		MemberCardGrade existing = memberCardGradeMapper.selectOne(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr)
						.eq(MemberCardGrade::getGradeId, gradeIdRaw));

		if (existing == null) {
			throw new OpenapiLegacyZeroCodeFailException("$entity must be an object, NULL given.");
		}

		transactionTemplate.executeWithoutResult(status -> memberCardGradeMapper.delete(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr)
						.eq(MemberCardGrade::getGradeId, existing.getGradeId())));
	}

	private void validateParams(String gradeIdRaw) {
		if (gradeIdRaw == null || gradeIdRaw.isEmpty()) {
			throw paramError();
		}
	}

	private void assertNoAssociatedMembers(long companyId, String gradeIdRaw) {
		long gradeIdForQuery = parseGradeIdForMemberCheck(gradeIdRaw);
		Long count = membersMapper.selectCount(
				new LambdaQueryWrapper<Members>()
						.eq(Members::getCompanyId, companyId)
						.eq(Members::getGradeId, gradeIdForQuery));
		if (count != null && count > 0) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.MEMBER_GRADE_DELETE_ERROR,
					"会员等级无法删除，该会员等级下扔存在关联的会员");
		}
	}

	private static long parseGradeIdForMemberCheck(String gradeIdRaw) {
		try {
			return Long.parseLong(gradeIdRaw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static OpenapiMemberV2FailException paramError() {
		return new OpenapiMemberV2FailException(
				OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请求参数错误");
	}
}
