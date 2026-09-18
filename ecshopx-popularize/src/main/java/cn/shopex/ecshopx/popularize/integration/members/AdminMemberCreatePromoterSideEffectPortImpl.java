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

package cn.shopex.ecshopx.popularize.integration.members;

import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePromoterSideEffectPort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.PromoterGradeUpgradeTriggerService;
import cn.shopex.ecshopx.popularize.service.PromoterUserChangeEligibilityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service("adminMemberCreatePromoterSideEffectPortImpl")
public class AdminMemberCreatePromoterSideEffectPortImpl implements AdminMemberCreatePromoterSideEffectPort {

	private final PromoterMapper promoterMapper;
	private final MemberAccountService memberAccountService;
	private final PromoterUserChangeEligibilityService eligibilityService;
	private final PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService;

	public AdminMemberCreatePromoterSideEffectPortImpl(
			PromoterMapper promoterMapper,
			MemberAccountService memberAccountService,
			PromoterUserChangeEligibilityService eligibilityService,
			PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService) {
		this.promoterMapper = promoterMapper;
		this.memberAccountService = memberAccountService;
		this.eligibilityService = eligibilityService;
		this.promoterGradeUpgradeTriggerService = promoterGradeUpgradeTriggerService;
	}

	@Override
	public void createForNewMember(long companyId, long userId, String mobilePlain, long gradeId) {
		Promoter existing = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getCompanyId, companyId)
				.eq(Promoter::getUserId, userId)
				.last("LIMIT 1"));
		if (existing != null) {
			return;
		}
		boolean isPromoter = eligibilityService.userIsChangePromoter(companyId, userId, false);
		Long inviterUserId = memberAccountService.findInviterUserId(companyId, userId);
		Promoter insert = new Promoter();
		insert.setUserId(userId);
		insert.setCompanyId(companyId);
		insert.setIdentityId(0L);
		insert.setIsSubordinates(0);
		insert.setGradeLevel(1);
		insert.setIsPromoter(isPromoter ? 1 : 0);
		insert.setDisabled(0);
		insert.setShopStatus(0);
		insert.setIsBuy(0);
		insert.setPromoterName("");
		insert.setRegionsId(null);
		insert.setAddress("");
		int now = (int) (System.currentTimeMillis() / 1000L);
		insert.setCreated(now);
		insert.setUpdated(now);
		if (inviterUserId != null && inviterUserId > 0L) {
			Promoter inviterRow = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
					.eq(Promoter::getCompanyId, companyId)
					.eq(Promoter::getUserId, inviterUserId)
					.last("LIMIT 1"));
			if (inviterRow != null && Objects.equals(inviterRow.getIsPromoter(), 1)) {
				insert.setPid(inviterRow.getId());
				String pm = memberAccountService.findMobileStored(companyId, inviterUserId);
				insert.setPmobile(pm != null ? pm : "");
				insert.setPname(inviterRow.getPromoterName() != null ? inviterRow.getPromoterName() : "");
			}
		}
		promoterMapper.insert(insert);
		if (insert.getPid() != null && inviterUserId != null && inviterUserId > 0L) {
			promoterGradeUpgradeTriggerService.upgradeGrade(companyId, inviterUserId);
		}
		if (isPromoter) {
			promoterGradeUpgradeTriggerService.upgradeGrade(companyId, userId);
		}
	}
}
