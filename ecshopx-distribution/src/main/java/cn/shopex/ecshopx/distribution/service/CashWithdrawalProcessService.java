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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.RebateCashWithdrawalMerchantPaymentPort;
import cn.shopex.ecshopx.distribution.domain.CashWithdrawal;
import cn.shopex.ecshopx.distribution.mapper.CashWithdrawalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class CashWithdrawalProcessService {

	private static final String PAYMENT_DESC_COMMISSION_WITHDRAWAL = "佣金提现";

	private final CashWithdrawalMapper cashWithdrawalMapper;
	private final RebateCashWithdrawalMerchantPaymentPort rebateCashWithdrawalMerchantPaymentPort;
	private final DistributeCountWriteService distributeCountWriteService;
	private final TransactionTemplate transactionTemplate;

	public CashWithdrawalProcessService(
			CashWithdrawalMapper cashWithdrawalMapper,
			RebateCashWithdrawalMerchantPaymentPort rebateCashWithdrawalMerchantPaymentPort,
			DistributeCountWriteService distributeCountWriteService,
			PlatformTransactionManager transactionManager) {
		this.cashWithdrawalMapper = cashWithdrawalMapper;
		this.rebateCashWithdrawalMerchantPaymentPort = rebateCashWithdrawalMerchantPaymentPort;
		this.distributeCountWriteService = distributeCountWriteService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public boolean process(
			Map<String, Object> user,
			long cashWithdrawalId,
			String processType,
			String remarks,
			String clientIp) {
		Object companyObj = user.get("company_id");
		long companyId = toLong(companyObj);

		if (Objects.equals(processType, "argee")) {
			processCashWithdrawal(companyId, cashWithdrawalId, clientIp);
			return true;
		}
		if (Objects.equals(processType, "reject")) {
			rejectCashWithdrawal(companyId, cashWithdrawalId, processType, remarks);
			return true;
		}
		if (Objects.equals(processType, "success")) {
			successCashWithdrawal(companyId, cashWithdrawalId, processType, remarks);
			return true;
		}
		throw new ResourceException("参数错误");
	}

	private void processCashWithdrawal(long companyId, long cashWithdrawalId, String clientIp) {
		CashWithdrawal info = requireApplyWithdrawal(companyId, cashWithdrawalId);

		LambdaUpdateWrapper<CashWithdrawal> toProcess = new LambdaUpdateWrapper<>();
		toProcess.eq(CashWithdrawal::getCompanyId, companyId).eq(CashWithdrawal::getId, cashWithdrawalId).set(CashWithdrawal::getStatus, "process");
		cashWithdrawalMapper.update(null, toProcess);

		Map<String, Object> paymentData = new LinkedHashMap<>();
		paymentData.put("rel_scene_id", String.valueOf(cashWithdrawalId));
		paymentData.put("rel_scene_name", "rebate_cash_withdrawal");
		paymentData.put("re_user_name", info.getDistributorName());
		paymentData.put("mobile", info.getDistributorMobile());
		paymentData.put("amount", info.getMoney() != null ? info.getMoney().longValue() : 0L);
		paymentData.put("user_id", info.getUserId());
		paymentData.put("open_id", info.getOpenId());
		paymentData.put("payment_desc", PAYMENT_DESC_COMMISSION_WITHDRAWAL);
		String ip = StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1";
		paymentData.put("spbill_create_ip", ip);

		Map<String, Object> data =
				rebateCashWithdrawalMerchantPaymentPort.merchantPayment(companyId, info.getWxaAppid(), paymentData);
		String st = channelStatus(data != null ? data.get("status") : null);

		if ("SUCCESS".equals(st)) {
			try {
				transactionTemplate.executeWithoutResult(
						txStatus -> {
							LambdaUpdateWrapper<CashWithdrawal> uw = new LambdaUpdateWrapper<>();
							uw.eq(CashWithdrawal::getCompanyId, companyId)
									.eq(CashWithdrawal::getId, cashWithdrawalId)
									.set(CashWithdrawal::getStatus, "success");
							cashWithdrawalMapper.update(null, uw);
							int money = info.getMoney() != null ? info.getMoney() : 0;
							distributeCountWriteService.agreeCashWithdrawal(
									companyId, info.getDistributorId(), money);
						});
			} catch (RuntimeException e) {
				throw new ResourceException("付款成功，服务器异常，请通过异常处理重试");
			}
			return;
		}
		if ("PROCESS".equals(st)) {
			LambdaUpdateWrapper<CashWithdrawal> uw = new LambdaUpdateWrapper<>();
			uw.eq(CashWithdrawal::getCompanyId, companyId).eq(CashWithdrawal::getId, cashWithdrawalId).set(CashWithdrawal::getStatus, "process");
			cashWithdrawalMapper.update(null, uw);
			return;
		}

		LambdaUpdateWrapper<CashWithdrawal> revert = new LambdaUpdateWrapper<>();
		revert.eq(CashWithdrawal::getCompanyId, companyId).eq(CashWithdrawal::getId, cashWithdrawalId).set(CashWithdrawal::getStatus, "apply");
		cashWithdrawalMapper.update(null, revert);

		String msg = errorDesc(data != null ? data.get("error_desc") : null);
		if (!StringUtils.hasText(msg)) {
			msg = "请检查微信支付相关配置是否完成";
		}
		throw new ResourceException(msg);
	}

	private void rejectCashWithdrawal(long companyId, long cashWithdrawalId, String processType, String remarks) {
		CashWithdrawal info = requireApplyWithdrawal(companyId, cashWithdrawalId);
		String persistStatus = Objects.equals(processType, "reject") ? "reject" : "cancel";

		try {
			transactionTemplate.executeWithoutResult(txStatus -> {
				CashWithdrawal patch = new CashWithdrawal();
				patch.setStatus(persistStatus);
				if (StringUtils.hasText(remarks)) {
					patch.setRemarks(remarks.trim());
				}
				LambdaUpdateWrapper<CashWithdrawal> uw = new LambdaUpdateWrapper<>();
				uw.eq(CashWithdrawal::getCompanyId, companyId).eq(CashWithdrawal::getId, cashWithdrawalId);
				cashWithdrawalMapper.update(patch, uw);

				int money = info.getMoney() != null ? info.getMoney() : 0;
				distributeCountWriteService.rejectCashWithdrawal(companyId, info.getDistributorId(), money);
			});
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException("系统错误，请稍后再试");
		}
	}

	private void successCashWithdrawal(long companyId, long cashWithdrawalId, String processType, String remarks) {
		CashWithdrawal info = requireApplyWithdrawal(companyId, cashWithdrawalId);

		try {
			transactionTemplate.executeWithoutResult(txStatus -> {
				CashWithdrawal patch = new CashWithdrawal();
				patch.setStatus("success");
				if (StringUtils.hasText(remarks)) {
					patch.setRemarks(remarks.trim());
				}
				LambdaUpdateWrapper<CashWithdrawal> uw = new LambdaUpdateWrapper<>();
				uw.eq(CashWithdrawal::getCompanyId, companyId).eq(CashWithdrawal::getId, cashWithdrawalId);
				cashWithdrawalMapper.update(patch, uw);

				int money = info.getMoney() != null ? info.getMoney() : 0;
				distributeCountWriteService.rejectCashWithdrawal(companyId, info.getDistributorId(), money);
			});
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException("系统错误，请稍后再试");
		}
	}

	private CashWithdrawal requireApplyWithdrawal(long companyId, long cashWithdrawalId) {
		LambdaQueryWrapper<CashWithdrawal> q = new LambdaQueryWrapper<>();
		q.eq(CashWithdrawal::getCompanyId, companyId).eq(CashWithdrawal::getId, cashWithdrawalId);
		CashWithdrawal info = cashWithdrawalMapper.selectOne(q);
		if (info == null) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}
		if (!"apply".equals(info.getStatus())) {
			throw new ResourceException("当前佣金提现正在处理或已完成");
		}
		return info;
	}

	private static String channelStatus(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString().trim().toUpperCase();
	}

	private static String errorDesc(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString().trim();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
