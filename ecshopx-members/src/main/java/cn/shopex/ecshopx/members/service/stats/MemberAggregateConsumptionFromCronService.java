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

package cn.shopex.ecshopx.members.service.stats;

import cn.shopex.ecshopx.common.cron.port.MemberTotalConsumptionMutatePort;
import cn.shopex.ecshopx.common.kaquan.port.GradeCardPackageOnUpgradeTriggerPort;
import cn.shopex.ecshopx.common.kaquan.port.MemberCardGradeListForCronPort;
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 定时任务侧按用户汇总消费后更新会员累计与等级；分支与订单批处理链 3.5 / 4.x / 5.x 对齐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAggregateConsumptionFromCronService {

	private final MemberTotalConsumptionMutatePort memberTotalConsumptionMutatePort;
	private final MemberTotalConsumptionReadService memberTotalConsumptionReadService;
	private final MemberCardGradeListForCronPort memberCardGradeListForCronPort;
	private final DmCrmSettingAdminService dmCrmSettingAdminService;
	private final MembersMapper membersMapper;
	private final FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;
	private final GradeCardPackageOnUpgradeTriggerPort gradeCardPackageOnUpgradeTriggerPort;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public void applyAggregates(Map<Long, ConsumptionAggregate> aggregates) {
		if (aggregates == null || aggregates.isEmpty()) {
			return;
		}
		for (ConsumptionAggregate agg : aggregates.values()) {
			updateMemberConsumptionOne(agg.userId(), agg.companyId(), agg.payFen());
		}
	}

	private void updateMemberConsumptionOne(long userId, long companyId, BigDecimal payFen) {
		if (userId <= 0L || payFen == null || payFen.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		if (oemShuyun) {
			return;
		}
		if (dmCrmOpen(companyId)) {
			return;
		}
		Members member = loadMember(userId, companyId);
		if (member == null) {
			return;
		}
		memberTotalConsumptionMutatePort.addFenToTotalOrSetZero(userId, payFen);
		BigDecimal totalAfter = memberTotalConsumptionReadService.getTotalConsumption(userId);
		List<Map<String, Object>> gradeRows = memberCardGradeListForCronPort.listForConsumptionUpgradeScan(companyId);
		if (gradeRows == null || gradeRows.isEmpty()) {
			return;
		}
		long currentGradeId = member.getGradeId() == null ? 0L : member.getGradeId();
		for (int i = gradeRows.size() - 1; i >= 0; i--) {
			Map<String, Object> value = gradeRows.get(i);
			long nextGradeId = longValue(value.get("grade_id"));
			BigDecimal thresholdFen = promotionTotalConsumptionToFen(value.get("promotion_condition"));
			if (totalAfter.compareTo(thresholdFen) < 0 || currentGradeId >= nextGradeId) {
				continue;
			}
			String mobile = member.getMobile() == null ? "" : member.getMobile();
			String gradeName = str(value.get("grade_name"));
			LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
			uw.eq(Members::getCompanyId, companyId)
					.eq(Members::getUserId, userId)
					.set(Members::getGradeId, nextGradeId);
			membersMapper.update(null, uw);
			Map<String, Object> activityMember = new LinkedHashMap<>();
			activityMember.put("grade_id", nextGradeId);
			activityMember.put("user_id", userId);
			activityMember.put("mobile", mobile);
			activityMember.put("grade_name", gradeName);
			scheduleMemberUpgradeJobAfterCommit(companyId, activityMember);
			gradeCardPackageOnUpgradeTriggerPort.trigger(companyId, userId, nextGradeId, "grade", true);
			break;
		}
	}

	private void scheduleMemberUpgradeJobAfterCommit(long companyId, Map<String, Object> activityMember) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			firePromotionsActivityDispatchPublisher.publishMemberUpgradeAfterOrderConsumption(companyId, activityMember);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				firePromotionsActivityDispatchPublisher.publishMemberUpgradeAfterOrderConsumption(companyId, activityMember);
			}
		});
	}

	private Members loadMember(long userId, long companyId) {
		return membersMapper.selectOne(
				new LambdaQueryWrapper<Members>()
						.eq(Members::getUserId, userId)
						.eq(Members::getCompanyId, companyId)
						.last("LIMIT 1"));
	}

	private boolean dmCrmOpen(long companyId) {
		Object raw = dmCrmSettingAdminService.getSetting(companyId);
		if (!(raw instanceof Map<?, ?> m)) {
			return false;
		}
		Object o = m.get("is_open");
		if (o instanceof Boolean b) {
			return b.booleanValue();
		}
		return StringUtils.hasText(str(o)) && "true".equalsIgnoreCase(str(o).trim());
	}

	private static BigDecimal promotionTotalConsumptionToFen(Object promotionCondition) {
		if (!(promotionCondition instanceof Map<?, ?> pc)) {
			return BigDecimal.ZERO;
		}
		Object tc = pc.get("total_consumption");
		if (tc == null) {
			return BigDecimal.ZERO;
		}
		BigDecimal yuan;
		if (tc instanceof BigDecimal bd) {
			yuan = bd;
		} else if (tc instanceof Number n) {
			yuan = new BigDecimal(n.toString());
		} else {
			try {
				yuan = new BigDecimal(tc.toString().trim());
			} catch (Exception e) {
				return BigDecimal.ZERO;
			}
		}
		return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
	}

	private static long longValue(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	public record ConsumptionAggregate(long userId, long companyId, BigDecimal payFen) {}

	public static Map<Long, ConsumptionAggregate> emptyAggregates() {
		return new HashMap<>();
	}
}
