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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.common.cron.port.MemberTotalConsumptionMutatePort;
import cn.shopex.ecshopx.common.kaquan.port.MemberCardGradeListForCronPort;
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.stats.MemberTotalConsumptionReadService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 非积分支付场景下，订单完成后更新会员消费相关标记；与定时任务侧一致地累加累计消费、判定消费升级并经 Bus 投递
 * {@code FirePromotionsActivity}（{@code member_upgrade}）。
 */
@Service
public class MemberConsumptionOnNormalOrderFinishService {

	private final MembersInfoMapper membersInfoMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final MembersMapper membersMapper;
	private final MemberTotalConsumptionMutatePort memberTotalConsumptionMutatePort;
	private final MemberTotalConsumptionReadService memberTotalConsumptionReadService;
	private final MemberCardGradeListForCronPort memberCardGradeListForCronPort;
	private final DmCrmSettingAdminService dmCrmSettingAdminService;
	private final FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;
	private final boolean oemShuyun;

	public MemberConsumptionOnNormalOrderFinishService(
			MembersInfoMapper membersInfoMapper,
			NormalOrdersMapper normalOrdersMapper,
			MembersMapper membersMapper,
			MemberTotalConsumptionMutatePort memberTotalConsumptionMutatePort,
			MemberTotalConsumptionReadService memberTotalConsumptionReadService,
			MemberCardGradeListForCronPort memberCardGradeListForCronPort,
			DmCrmSettingAdminService dmCrmSettingAdminService,
			FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.membersInfoMapper = membersInfoMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.membersMapper = membersMapper;
		this.memberTotalConsumptionMutatePort = memberTotalConsumptionMutatePort;
		this.memberTotalConsumptionReadService = memberTotalConsumptionReadService;
		this.memberCardGradeListForCronPort = memberCardGradeListForCronPort;
		this.dmCrmSettingAdminService = dmCrmSettingAdminService;
		this.firePromotionsActivityDispatchPublisher = firePromotionsActivityDispatchPublisher;
		this.oemShuyun = oemShuyun;
	}

	public void updateMemberConsumptionIfNotPointPay(long companyId, NormalOrders order) {
		if (companyId <= 0L || order == null || order.getOrderId() == null || order.getOrderId() <= 0L) {
			return;
		}
		if (isPointPay(order)) {
			return;
		}
		long userId = order.getUserId() == null ? 0L : order.getUserId();
		if (userId > 0L) {
			MembersInfo info =
					membersInfoMapper.selectOne(
							new LambdaQueryWrapper<MembersInfo>()
									.eq(MembersInfo::getCompanyId, companyId)
									.eq(MembersInfo::getUserId, userId)
									.last("LIMIT 1"));
			if (info != null) {
				long nowSec = System.currentTimeMillis() / 1000L;
				LambdaUpdateWrapper<MembersInfo> uw = new LambdaUpdateWrapper<>();
				uw.eq(MembersInfo::getCompanyId, companyId)
						.eq(MembersInfo::getUserId, userId)
						.set(MembersInfo::getHaveConsume, true)
						.set(MembersInfo::getUpdated, nowSec);
				membersInfoMapper.update(null, uw);
			}
		}
		LambdaUpdateWrapper<NormalOrders> ow = new LambdaUpdateWrapper<>();
		ow.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, order.getOrderId())
				.set(NormalOrders::getIsConsumption, 1);
		normalOrdersMapper.update(null, ow);

		if (userId <= 0L) {
			return;
		}
		if (oemShuyun || dmCrmOpen(companyId)) {
			return;
		}
		BigDecimal payFen = orderPayFenFromOrder(order);
		if (payFen.compareTo(BigDecimal.ZERO) <= 0) {
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
			LambdaUpdateWrapper<Members> muw = new LambdaUpdateWrapper<>();
			muw.eq(Members::getCompanyId, companyId)
					.eq(Members::getUserId, userId)
					.set(Members::getGradeId, nextGradeId);
			membersMapper.update(null, muw);
			Map<String, Object> activityMember = new LinkedHashMap<>();
			activityMember.put("grade_id", nextGradeId);
			activityMember.put("user_id", userId);
			activityMember.put("mobile", mobile);
			activityMember.put("grade_name", gradeName);
			scheduleMemberUpgradeJobAfterCommit(companyId, activityMember);
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

	private static BigDecimal orderPayFenFromOrder(NormalOrders order) {
		String tf = order.getTotalFee();
		if (!StringUtils.hasText(tf)) {
			return BigDecimal.ZERO;
		}
		try {
			BigDecimal fen = new BigDecimal(tf.trim());
			return fen.compareTo(BigDecimal.ZERO) > 0 ? fen : BigDecimal.ZERO;
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
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

	private static boolean isPointPay(NormalOrders order) {
		String pt = order.getPayType();
		return StringUtils.hasText(pt) && "point".equalsIgnoreCase(pt.trim());
	}
}
