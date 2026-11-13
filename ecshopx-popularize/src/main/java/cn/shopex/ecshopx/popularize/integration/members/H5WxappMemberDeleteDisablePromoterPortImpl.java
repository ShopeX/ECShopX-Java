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

import cn.shopex.ecshopx.common.members.h5.H5WxappMemberDeleteDisablePromoterPort;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.PromoterGradeUpgradeTriggerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("h5WxappMemberDeleteDisablePromoterPortImpl")
@RequiredArgsConstructor
public class H5WxappMemberDeleteDisablePromoterPortImpl implements H5WxappMemberDeleteDisablePromoterPort {

	private final PromoterMapper promoterMapper;
	private final PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService;

	@Override
	public void disablePromoter(long companyId, long userId) {
		Promoter row =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.last("LIMIT 1"));
		if (row == null) {
			return;
		}
		Long oldParentRecordId = row.getPid();
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Promoter> u =
				new LambdaUpdateWrapper<Promoter>()
						.eq(Promoter::getCompanyId, companyId)
						.eq(Promoter::getUserId, userId)
						.set(Promoter::getDisabled, 1)
						.set(Promoter::getPid, null)
						.set(Promoter::getPmobile, null)
						.set(Promoter::getPname, null)
						.set(Promoter::getUpdated, nowSec);
		promoterMapper.update(null, u);
		promoterGradeUpgradeTriggerService.upgradeGrade(companyId, userId);
		if (oldParentRecordId != null && oldParentRecordId > 0L) {
			Promoter parent = promoterMapper.selectById(oldParentRecordId);
			if (parent != null && parent.getUserId() != null && parent.getUserId() > 0L) {
				promoterGradeUpgradeTriggerService.upgradeGrade(companyId, parent.getUserId());
			}
		}
	}
}
