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

package cn.shopex.ecshopx.kaquan.integration.members;

import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchVipDelayPort;
import cn.shopex.ecshopx.common.members.admin.VipGradeRow;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeBatchActiveDelayService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeOrderReceiveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service("adminMemberBatchVipDelayPortImpl")
public class AdminMemberBatchVipDelayPortImpl implements AdminMemberBatchVipDelayPort {

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService;
	private final VipGradeOrderReceiveService vipGradeOrderReceiveService;
	private final TransactionTemplate transactionTemplate;

	public AdminMemberBatchVipDelayPortImpl(
			VipGradeMapper vipGradeMapper,
			VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService,
			VipGradeOrderReceiveService vipGradeOrderReceiveService,
			PlatformTransactionManager platformTransactionManager) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeBatchActiveDelayService = vipGradeBatchActiveDelayService;
		this.vipGradeOrderReceiveService = vipGradeOrderReceiveService;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	@Override
	public VipGradeRow loadVipGrade(long companyId, long vipGradeId) {
		VipGrade g =
				vipGradeMapper.selectOne(
						new LambdaQueryWrapper<VipGrade>()
								.eq(VipGrade::getCompanyId, (int) companyId)
								.eq(VipGrade::getVipGradeId, vipGradeId));
		if (g == null) {
			return null;
		}
		String lv = g.getLvType() == null ? "vip" : g.getLvType();
		return new VipGradeRow(vipGradeId, lv);
	}

	@Override
	public void processExpiredMemberExtension(long companyId, long vipGradeId, int addDay) {
		vipGradeBatchActiveDelayService.processBatchActiveDelay(companyId, vipGradeId, addDay, "expired", null);
	}

	@Override
	public void applyVipDelayForUserChunk(
			long companyId, long vipGradeId, int addDay, Map<Long, String> userIdToMobilePlain) {
		if (userIdToMobilePlain == null || userIdToMobilePlain.isEmpty()) {
			return;
		}
		transactionTemplate.execute(
				status -> {
					for (Map.Entry<Long, String> e : userIdToMobilePlain.entrySet()) {
						vipGradeOrderReceiveService.receiveAdminCustomDelay(
								companyId, e.getKey(), e.getValue() == null ? "" : e.getValue(), vipGradeId, addDay);
					}
					return null;
				});
	}
}
