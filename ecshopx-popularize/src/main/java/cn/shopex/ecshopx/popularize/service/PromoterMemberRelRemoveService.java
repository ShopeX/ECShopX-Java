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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PromoterMemberRelRemoveService {

	private final PromoterMapper promoterMapper;
	private final MemberAccountService memberAccountService;
	private final PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService;

	public PromoterMemberRelRemoveService(
			PromoterMapper promoterMapper,
			MemberAccountService memberAccountService,
			PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService) {
		this.promoterMapper = promoterMapper;
		this.memberAccountService = memberAccountService;
		this.promoterGradeUpgradeTriggerService = promoterGradeUpgradeTriggerService;
	}

	public void updateMemberPromoterRemove(long companyId, long userId, long newUserId) {
		if (userId == newUserId) {
			throw new ResourceException("自己不能调到自己");
		}
		Promoter userInfo =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getUserId, userId)
								.last("LIMIT 1"));
		if (userInfo == null) {
			throw new ResourceException("当前不支持调整");
		}
		Long p = userInfo.getPid();
		if (p == null || p <= 0L) {
			throw new ResourceException("当前不支持调整");
		}
		Integer ip = userInfo.getIsPromoter();
		if (ip != null && ip != 0) {
			throw new ResourceException("当前已经是推广员");
		}
		if (newUserId <= 0L) {
			throw new BadRequestException("推广员ID错误");
		}
		Promoter pdata =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getUserId, newUserId)
								.last("LIMIT 1"));
		if (pdata == null || !Objects.equals(pdata.getCompanyId(), companyId)) {
			throw new ResourceException("无效的上级");
		}
		Integer nip = pdata.getIsPromoter();
		if (nip == null || nip == 0) {
			throw new ResourceException("无效的上级");
		}
		Integer dis = pdata.getDisabled();
		if (dis != null && dis != 0) {
			throw new ResourceException("无效的上级");
		}
		long parentPromoterRecordId = pdata.getId();
		String pmobile =
				memberAccountService.findMobileStored(pdata.getCompanyId(), newUserId);
		LambdaUpdateWrapper<Promoter> uw =
				new LambdaUpdateWrapper<Promoter>()
						.eq(Promoter::getUserId, userId)
						.set(Promoter::getPid, parentPromoterRecordId)
						.set(Promoter::getPmobile, pmobile);
		int rows = promoterMapper.update(null, uw);
		if (rows <= 0) {
			throw new ResourceException("更新失败");
		}
		promoterGradeUpgradeTriggerService.upgradeGrade(companyId, newUserId);
		promoterGradeUpgradeTriggerService.upgradeGrade(companyId, userId);
		Long oldPid = userInfo.getPid();
		promoterGradeUpgradeTriggerService.upgradeGrade(companyId, oldPid == null ? 0L : oldPid.longValue());
	}
}
