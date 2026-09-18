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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseParticipateUserMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityDataService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class EmployeePurchasePassphraseOrderService {

	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	private final ActivityPassphraseService activityPassphraseService;
	private final EmployeePassphraseService employeePassphraseService;
	private final PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService;
	private final ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper;
	private final OrdersRelActivityMapper ordersRelActivityMapper;

	public EmployeePurchasePassphraseOrderService(
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService,
			ActivityPassphraseService activityPassphraseService,
			EmployeePassphraseService employeePassphraseService,
			PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService,
			ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper,
			OrdersRelActivityMapper ordersRelActivityMapper) {
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
		this.activityPassphraseService = activityPassphraseService;
		this.employeePassphraseService = employeePassphraseService;
		this.passphraseParticipateQuotaRedisService = passphraseParticipateQuotaRedisService;
		this.activityEnterpriseParticipateUserMapper = activityEnterpriseParticipateUserMapper;
		this.ordersRelActivityMapper = ordersRelActivityMapper;
	}

	public void checkBeforeOrder(NormalOrderCreateParams p, boolean isCheck) {
		Map<String, Object> pr = p.getParams();
		if (!"normal_employee_purchase".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		long enterpriseId = longVal(pr.get("enterprise_id"), 0L);
		long activityId = longVal(pr.get("activity_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		if (companyId <= 0L || enterpriseId <= 0L || activityId <= 0L || userId <= 0L) {
			return;
		}

		Activities activity =
				employeePurchaseActivityDataService.requireActivityWithEnterpriseOrThrow(
						companyId, activityId, enterpriseId);
		assertActivityStatusForOrder(activity);

		if (!activityPassphraseService.isPassphraseEnabled(activity)) {
			return;
		}

		String memberMobile = stringVal(pr.get("mobile"));
		employeePassphraseService.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, companyId, activityId, enterpriseId, userId, memberMobile);

		if (isCheck) {
			employeePassphraseService.assertQuotaAvailableForOrder(companyId, activityId, enterpriseId, userId);
			assertPassphraseLimitFee(p, activity, companyId, enterpriseId, activityId, userId);
		}
	}

	public void persistRelAndScheduleQuotaConsume(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if (!"normal_employee_purchase".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		long enterpriseId = longVal(pr.get("enterprise_id"), 0L);
		long activityId = longVal(pr.get("activity_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		long orderId = longVal(od.get("order_id"), 0L);
		if (companyId <= 0L || enterpriseId <= 0L || activityId <= 0L || userId <= 0L || orderId <= 0L) {
			return;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		Activities activity =
				employeePurchaseActivityDataService.requireActivityWithEnterpriseOrThrow(
						companyId, activityId, enterpriseId);
		OrdersRelActivity rel = new OrdersRelActivity();
		rel.setOrderId(orderId);
		rel.setCompanyId(companyId);
		rel.setEnterpriseId(enterpriseId);
		rel.setActivityId(activityId);
		rel.setUserId(userId);
		rel.setIfShareStore(Boolean.TRUE.equals(activity.getIfShareStore()));
		rel.setCloseModifyTime(now);
		rel.setParticipateQuotaOrderConsumed(false);
		rel.setPurchaseMode(activity.getPurchaseMode());
		if (PurchaseModeSupport.isPrepaidPoint(activity)) {
			int prepaidPayable = intVal(od.get("prepaid_payable_fee"), 0);
			if (prepaidPayable <= 0) {
				long totalFee = longVal(od.get("total_fee"), 0L);
				if (totalFee <= 0L) {
					totalFee = longVal(od.get("item_fee"), 0L) + longVal(od.get("freight_fee"), 0L);
				}
				prepaidPayable = (int) Math.min(Math.max(totalFee, 0L), Integer.MAX_VALUE);
			}
			rel.setPrepaidPayableFee(prepaidPayable);
			rel.setRestoredPrepaidFee(0);
		} else {
			rel.setPrepaidPayableFee(0);
			rel.setRestoredPrepaidFee(0);
		}
		ordersRelActivityMapper.insert(rel);

		if (!activityPassphraseService.isPassphraseEnabled(activity)) {
			return;
		}
		if (employeePassphraseService.isParticipateUser(companyId, activityId, enterpriseId, userId)) {
			return;
		}

		scheduleConsumeQuotaAfterCommit(companyId, activityId, enterpriseId, userId, orderId);
	}

	public void releaseQuotaOnCancel(OrdersRelActivity rel) {
		if (rel == null || !Boolean.TRUE.equals(rel.getParticipateQuotaOrderConsumed())) {
			return;
		}
		if (rel.getCompanyId() == null || rel.getActivityId() == null || rel.getEnterpriseId() == null) {
			return;
		}
		passphraseParticipateQuotaRedisService.releaseOneSlot(
				rel.getCompanyId(), rel.getActivityId(), rel.getEnterpriseId());
		rel.setParticipateQuotaOrderConsumed(false);
		ordersRelActivityMapper.updateById(rel);
	}

	private void scheduleConsumeQuotaAfterCommit(
			long companyId, long activityId, long enterpriseId, long userId, long orderId) {
		Runnable task = () -> consumeQuotaAfterOrderCommit(companyId, activityId, enterpriseId, userId, orderId);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			task.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						task.run();
					}
				});
	}

	private void consumeQuotaAfterOrderCommit(
			long companyId, long activityId, long enterpriseId, long userId, long orderId) {
		if (employeePassphraseService.isParticipateUser(companyId, activityId, enterpriseId, userId)) {
			return;
		}
		ActivityPassphraseEnterprise cfg =
				activityPassphraseService.requireEnterpriseConfig(companyId, activityId, enterpriseId);
		if (cfg == null) {
			return;
		}
		PassphraseParticipateQuotaRedisService.TryConsumeResult result =
				passphraseParticipateQuotaRedisService.tryConsumeSlot(companyId, activityId, enterpriseId);
		if (result == PassphraseParticipateQuotaRedisService.TryConsumeResult.NOT_CONFIGURED) {
			passphraseParticipateQuotaRedisService.syncRemainingQuota(
					companyId, activityId, enterpriseId, cfg.getParticipateQuota());
			result = passphraseParticipateQuotaRedisService.tryConsumeSlot(companyId, activityId, enterpriseId);
		}
		if (result != PassphraseParticipateQuotaRedisService.TryConsumeResult.SUCCESS) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		activityEnterpriseParticipateUserMapper.insertIgnore(
				companyId, activityId, enterpriseId, userId, now);
		OrdersRelActivity rel = ordersRelActivityMapper.selectById(orderId);
		if (rel != null) {
			rel.setParticipateQuotaOrderConsumed(true);
			ordersRelActivityMapper.updateById(rel);
		}
	}

	private void assertPassphraseLimitFee(
			NormalOrderCreateParams p,
			Activities activity,
			long companyId,
			long enterpriseId,
			long activityId,
			long userId) {
		ActivityPassphraseEnterprise cfg =
				activityPassphraseService.requireEnterpriseConfig(companyId, activityId, enterpriseId);
		if (cfg == null || cfg.getPassphraseLimitfee() == null) {
			return;
		}
		Map<String, Object> aggregate =
				employeePurchaseActivityDataService.getPassphraseAggregateFee(
						activity, companyId, enterpriseId, activityId, userId, cfg.getPassphraseLimitfee());
		long leftFee = longVal(aggregate.get("left_fee"), 0L);
		long itemFee = longVal(p.getOrderData().get("item_fee"), 0L);
		if (leftFee < itemFee) {
			throw new ResourceException("超过个人口令通道额度");
		}
	}

	private static void assertActivityStatusForOrder(Activities activity) {
		String status = activity.getStatus() == null ? "" : activity.getStatus();
		if ("cancel".equals(status)) {
			throw new ResourceException("活动已取消");
		}
		if ("pending".equals(status)) {
			throw new ResourceException("活动已暂停");
		}
		if ("over".equals(status)) {
			throw new ResourceException("活动已暂停");
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
