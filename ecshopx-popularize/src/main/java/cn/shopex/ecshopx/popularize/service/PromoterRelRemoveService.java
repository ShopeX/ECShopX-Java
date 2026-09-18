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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromoterRelRemoveService {

	private final PromoterMapper promoterMapper;
	private final MemberAccountService memberAccountService;
	private final PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService;

	public PromoterRelRemoveService(
			PromoterMapper promoterMapper,
			MemberAccountService memberAccountService,
			PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService) {
		this.promoterMapper = promoterMapper;
		this.memberAccountService = memberAccountService;
		this.promoterGradeUpgradeTriggerService = promoterGradeUpgradeTriggerService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void relRemove(long companyId, long userId, long newUserId) {
		if (userId == newUserId) {
			throw new ResourceException("自己不能调到自己");
		}
		Promoter userInfo = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getUserId, userId)
				.last("LIMIT 1"));
		if (userInfo == null) {
			throw new ResourceException("当前不是推广员-");
		}
		if (!Objects.equals(userInfo.getCompanyId(), companyId)) {
			throw new ResourceException("无效的推广员");
		}
		if (newUserId > 0L) {
			Promoter newParent = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
					.eq(Promoter::getUserId, newUserId)
					.last("LIMIT 1"));
			if (newParent != null) {
				Long walkPid = newParent.getPid();
				int depth = 100;
				while (walkPid != null && walkPid > 0L && depth-- > 0) {
					Promoter anc = promoterMapper.selectById(walkPid);
					if (anc == null) {
						break;
					}
					if (Objects.equals(anc.getUserId(), userId)) {
						throw new ResourceException("不能移动到下级");
					}
					walkPid = anc.getPid();
				}
			}
			Promoter pdata = newParent;
			if (pdata == null) {
				throw new ResourceException("无效的上级！");
			}
			if (!Objects.equals(pdata.getCompanyId(), companyId)) {
				throw new ResourceException("无效的上级。");
			}
			Integer d = userInfo.getDisabled();
			if (d != null && d != 0) {
				throw new ResourceException("无效的上级-");
			}
			Long pidCol = pdata.getId();
			String pname = pdata.getPromoterName() == null ? "" : pdata.getPromoterName();
			String pmobile = memberAccountService.findMobileStored(pdata.getCompanyId(), newUserId);
			LambdaUpdateWrapper<Promoter> uw = new LambdaUpdateWrapper<Promoter>()
					.eq(Promoter::getUserId, userId)
					.set(Promoter::getPid, pidCol)
					.set(Promoter::getPmobile, pmobile)
					.set(Promoter::getPname, pname);
			int rows = promoterMapper.update(null, uw);
			if (rows <= 0) {
				throw new ResourceException("更新失败");
			}
			Long oldPid = userInfo.getPid();
			promoterGradeUpgradeTriggerService.upgradeGrade(companyId, newUserId);
			promoterGradeUpgradeTriggerService.upgradeGrade(companyId, userId);
			if (oldPid != null && oldPid > 0L) {
				promoterGradeUpgradeTriggerService.upgradeGrade(companyId, oldPid);
			}
			return;
		}
		Long oldPid = userInfo.getPid();
		// 调到顶级：持久层将空父级 id 与 0 都视为「无上级」；重复剥离保持幂等，不因「已是顶级」拒绝。
		LambdaUpdateWrapper<Promoter> uw = new LambdaUpdateWrapper<Promoter>()
				.eq(Promoter::getUserId, userId)
				.set(Promoter::getPid, null)
				.set(Promoter::getPmobile, null)
				.set(Promoter::getPname, null);
		int rows = promoterMapper.update(null, uw);
		if (rows <= 0) {
			throw new ResourceException("更新失败");
		}
		promoterGradeUpgradeTriggerService.upgradeGrade(companyId, userId);
		if (oldPid != null && oldPid > 0L) {
			promoterGradeUpgradeTriggerService.upgradeGrade(companyId, oldPid);
		}
	}
}
