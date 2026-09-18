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
import cn.shopex.ecshopx.common.port.RebateCashWithdrawalMerchantPaymentPort;
import cn.shopex.ecshopx.popularize.port.PopularizeHfpayRebateMerchantWithdrawalPort;
import cn.shopex.ecshopx.popularize.domain.PromoterCashWithdrawal;
import cn.shopex.ecshopx.popularize.mapper.PromoterCashWithdrawalMapper;
import cn.shopex.ecshopx.popularize.port.PopularizeCashWithdrawalBankcardPayPort;
import cn.shopex.ecshopx.popularize.util.PopularizeCashWithdrawalChannelStatus;
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
public class PopularizeCashWithdrawalProcessService {

	private static final String PAYMENT_DESC_COMMISSION_WITHDRAWAL = "佣金提现";

	private static final String REL_SCENE_NAME_POPULARIZE = "popularize_rebate_cash_withdrawal";

	private final PromoterCashWithdrawalMapper promoterCashWithdrawalMapper;
	private final RebateCashWithdrawalMerchantPaymentPort rebateCashWithdrawalMerchantPaymentPort;
	private final PopularizeHfpayRebateMerchantWithdrawalPort popularizeHfpayRebateMerchantWithdrawalPort;
	private final PopularizeCashWithdrawalBankcardPayPort popularizeCashWithdrawalBankcardPayPort;
	private final PopularizePromoterCountWriteService popularizePromoterCountWriteService;
	private final TransactionTemplate transactionTemplate;

	public PopularizeCashWithdrawalProcessService(
			PromoterCashWithdrawalMapper promoterCashWithdrawalMapper,
			RebateCashWithdrawalMerchantPaymentPort rebateCashWithdrawalMerchantPaymentPort,
			PopularizeHfpayRebateMerchantWithdrawalPort popularizeHfpayRebateMerchantWithdrawalPort,
			PopularizeCashWithdrawalBankcardPayPort popularizeCashWithdrawalBankcardPayPort,
			PopularizePromoterCountWriteService popularizePromoterCountWriteService,
			PlatformTransactionManager transactionManager) {
		this.promoterCashWithdrawalMapper = promoterCashWithdrawalMapper;
		this.rebateCashWithdrawalMerchantPaymentPort = rebateCashWithdrawalMerchantPaymentPort;
		this.popularizeHfpayRebateMerchantWithdrawalPort = popularizeHfpayRebateMerchantWithdrawalPort;
		this.popularizeCashWithdrawalBankcardPayPort = popularizeCashWithdrawalBankcardPayPort;
		this.popularizePromoterCountWriteService = popularizePromoterCountWriteService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public boolean process(long companyId, String cashWithdrawalIdRaw, String processType, String remarks) {
		if (Objects.equals(processType, "argee")) {
			long id = parseWithdrawalId(cashWithdrawalIdRaw);
			processAgreeWithdrawal(companyId, id);
			return true;
		}
		if (Objects.equals(processType, "reject")) {
			long id = parseWithdrawalId(cashWithdrawalIdRaw);
			processReject(companyId, id, remarks);
			return true;
		}
		throw new ResourceException("参数错误");
	}

	private static long parseWithdrawalId(String raw) {
		try {
			return Long.parseLong(raw.trim());
		} catch (Exception e) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}
	}

	private void processAgreeWithdrawal(long companyId, long id) {
		PromoterCashWithdrawal info = requireApplyWithdrawal(companyId, id);

		LambdaUpdateWrapper<PromoterCashWithdrawal> toProcess = new LambdaUpdateWrapper<>();
		toProcess.eq(PromoterCashWithdrawal::getCompanyId, companyId)
				.eq(PromoterCashWithdrawal::getId, id)
				.set(PromoterCashWithdrawal::getStatus, "process");
		int u1 = promoterCashWithdrawalMapper.update(null, toProcess);
		if (u1 == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> data;
		if (Objects.equals(info.getPayType(), "wechat")) {
			Map<String, Object> paymentData = new LinkedHashMap<>();
			paymentData.put("rel_scene_id", String.valueOf(id));
			paymentData.put("rel_scene_name", REL_SCENE_NAME_POPULARIZE);
			paymentData.put("re_user_name", info.getAccountName());
			paymentData.put("mobile", info.getMobile());
			paymentData.put("amount", info.getMoney() != null ? info.getMoney().longValue() : 0L);
			paymentData.put("user_id", info.getUserId());
			paymentData.put("open_id", info.getPayAccount());
			paymentData.put("payment_desc", PAYMENT_DESC_COMMISSION_WITHDRAWAL);
			paymentData.put("spbill_create_ip", "127.0.0.1");
			data = rebateCashWithdrawalMerchantPaymentPort.merchantPayment(companyId, info.getWxaAppid(), paymentData);
		} else if (Objects.equals(info.getPayType(), "hfpay")) {
			Map<String, Object> paymentData = new LinkedHashMap<>();
			paymentData.put("company_id", companyId);
			paymentData.put("user_id", info.getUserId());
			paymentData.put("rel_scene_id", String.valueOf(id));
			paymentData.put("rel_scene_name", REL_SCENE_NAME_POPULARIZE);
			paymentData.put("trans_amt", info.getMoney());
			paymentData.put("spbill_create_ip", "127.0.0.1");
			data = popularizeHfpayRebateMerchantWithdrawalPort.merchantPayment(paymentData);
		} else if (Objects.equals(info.getPayType(), "bankcard")) {
			data = popularizeCashWithdrawalBankcardPayPort.payToBankcard(companyId, info);
		} else {
			data = new LinkedHashMap<>(Map.of("status", "SUCCESS"));
		}

		String st = PopularizeCashWithdrawalChannelStatus.channelStatus(data == null ? null : data.get("status"));

		if (Objects.equals(st, "SUCCESS")) {
			long userId = parseUserIdOrThrow(info.getUserId());
			int moneyInt = info.getMoney() == null ? 0 : info.getMoney();
			try {
				transactionTemplate.executeWithoutResult(txStatus -> {
					LambdaUpdateWrapper<PromoterCashWithdrawal> uw = new LambdaUpdateWrapper<>();
					uw.eq(PromoterCashWithdrawal::getCompanyId, companyId)
							.eq(PromoterCashWithdrawal::getId, id)
							.set(PromoterCashWithdrawal::getStatus, "success");
					int n = promoterCashWithdrawalMapper.update(null, uw);
					if (n == 0) {
						throw new ResourceException("未查询到更新数据");
					}
					popularizePromoterCountWriteService.agreeCashWithdrawal(companyId, userId, moneyInt);
				});
			} catch (ResourceException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new ResourceException("付款成功，服务器异常，请通过异常处理重试");
			}
			return;
		}

		if (Objects.equals(st, "PROCESS")) {
			LambdaUpdateWrapper<PromoterCashWithdrawal> uw = new LambdaUpdateWrapper<>();
			uw.eq(PromoterCashWithdrawal::getCompanyId, companyId)
					.eq(PromoterCashWithdrawal::getId, id)
					.set(PromoterCashWithdrawal::getStatus, "process");
			int n = promoterCashWithdrawalMapper.update(null, uw);
			if (n == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			return;
		}

		LambdaUpdateWrapper<PromoterCashWithdrawal> revert = new LambdaUpdateWrapper<>();
		revert.eq(PromoterCashWithdrawal::getCompanyId, companyId)
				.eq(PromoterCashWithdrawal::getId, id)
				.set(PromoterCashWithdrawal::getStatus, "apply");
		int r = promoterCashWithdrawalMapper.update(null, revert);
		if (r == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		String msg = PopularizeCashWithdrawalChannelStatus.errorDesc(
				data != null ? data.get("error_desc") : null);
		if (!StringUtils.hasText(msg)) {
			if (Objects.equals(info.getPayType(), "wechat")) {
				msg = "请检查微信支付相关配置是否完成";
			} else if (Objects.equals(info.getPayType(), "hfpay")) {
				msg = "汇付天下参数未配置";
			} else {
				msg = "支付失败";
			}
		}
		throw new ResourceException(msg);
	}

	private void processReject(long companyId, long id, String remarks) {
		PromoterCashWithdrawal info = requireApplyWithdrawal(companyId, id);
		long userId = parseUserIdOrThrow(info.getUserId());
		int moneyInt = info.getMoney() == null ? 0 : info.getMoney();
		try {
			transactionTemplate.executeWithoutResult(txStatus -> {
				PromoterCashWithdrawal patch = new PromoterCashWithdrawal();
				patch.setStatus("reject");
				if (StringUtils.hasText(remarks)) {
					patch.setRemarks(remarks.trim());
				}
				LambdaUpdateWrapper<PromoterCashWithdrawal> uw = new LambdaUpdateWrapper<>();
				uw.eq(PromoterCashWithdrawal::getCompanyId, companyId).eq(PromoterCashWithdrawal::getId, id);
				int n = promoterCashWithdrawalMapper.update(patch, uw);
				if (n == 0) {
					throw new ResourceException("未查询到更新数据");
				}
				popularizePromoterCountWriteService.rejectCashWithdrawal(companyId, userId, moneyInt);
			});
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException("系统错误，请稍后再试");
		}
	}

	private PromoterCashWithdrawal requireApplyWithdrawal(long companyId, long withdrawalId) {
		LambdaQueryWrapper<PromoterCashWithdrawal> q = new LambdaQueryWrapper<>();
		q.eq(PromoterCashWithdrawal::getCompanyId, companyId).eq(PromoterCashWithdrawal::getId, withdrawalId);
		PromoterCashWithdrawal info = promoterCashWithdrawalMapper.selectOne(q);
		if (info == null) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}
		if (!Objects.equals(info.getStatus(), "apply")) {
			throw new ResourceException("当前佣金提现正在处理或已完成");
		}
		return info;
	}

	private static long parseUserIdOrThrow(String rawUserId) {
		String u = rawUserId == null ? "" : rawUserId.trim();
		if (!StringUtils.hasText(u)) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}
		try {
			return Long.parseLong(u);
		} catch (NumberFormatException e) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}
	}
}
