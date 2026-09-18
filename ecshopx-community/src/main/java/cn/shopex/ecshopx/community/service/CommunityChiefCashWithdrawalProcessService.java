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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityChiefCashWithdrawal;
import cn.shopex.ecshopx.community.mapper.CommunityChiefCashWithdrawalMapper;
import cn.shopex.ecshopx.distribution.integration.RebateCashWithdrawalMerchantPaymentClient;
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
public class CommunityChiefCashWithdrawalProcessService {

	private static final String PAYMENT_DESC_COMMISSION_WITHDRAWAL = "佣金提现";

	private final CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper;
	private final RebateCashWithdrawalMerchantPaymentClient rebateCashWithdrawalMerchantPaymentClient;
	private final TransactionTemplate transactionTemplate;

	public CommunityChiefCashWithdrawalProcessService(
			CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper,
			RebateCashWithdrawalMerchantPaymentClient rebateCashWithdrawalMerchantPaymentClient,
			PlatformTransactionManager transactionManager) {
		this.communityChiefCashWithdrawalMapper = communityChiefCashWithdrawalMapper;
		this.rebateCashWithdrawalMerchantPaymentClient = rebateCashWithdrawalMerchantPaymentClient;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public boolean process(
			long companyId, long cashWithdrawalId, String processType, String remarks, String clientIp) {
		if (Objects.equals(processType, "argee")) {
			processCashWithdrawal(companyId, cashWithdrawalId, clientIp);
			return true;
		}
		if (Objects.equals(processType, "reject")) {
			rejectCashWithdrawal(companyId, cashWithdrawalId, remarks);
			return true;
		}
		throw new BadRequestException("参数错误");
	}

	private void processCashWithdrawal(long companyId, long cashWithdrawalId, String clientIp) {
		CommunityChiefCashWithdrawal info = requireApplyWithdrawal(companyId, cashWithdrawalId);

		LambdaUpdateWrapper<CommunityChiefCashWithdrawal> toProcess = new LambdaUpdateWrapper<>();
		toProcess.eq(CommunityChiefCashWithdrawal::getCompanyId, companyId)
				.eq(CommunityChiefCashWithdrawal::getId, cashWithdrawalId)
				.set(CommunityChiefCashWithdrawal::getStatus, "process");
		int toProcessRows = communityChiefCashWithdrawalMapper.update(null, toProcess);
		if (toProcessRows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		String payTypeRaw = info.getPayType();
		String payType = payTypeRaw != null ? payTypeRaw.trim().toLowerCase() : "";

		Map<String, Object> data;
		if ("wechat".equals(payType)) {
			Map<String, Object> paymentData = new LinkedHashMap<>();
			paymentData.put("rel_scene_id", String.valueOf(cashWithdrawalId));
			paymentData.put("rel_scene_name", "community_chief_cash_withdrawal");
			paymentData.put("re_user_name", info.getAccountName());
			paymentData.put("mobile", info.getMobile());
			paymentData.put("amount", info.getMoney() != null ? info.getMoney().longValue() : 0L);
			paymentData.put("user_id", info.getChiefId());
			paymentData.put("open_id", info.getPayAccount());
			paymentData.put("payment_desc", PAYMENT_DESC_COMMISSION_WITHDRAWAL);
			String ip = StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1";
			paymentData.put("spbill_create_ip", ip);
			data = rebateCashWithdrawalMerchantPaymentClient.merchantPayment(
					companyId, info.getWxaAppid(), paymentData);
		} else if ("bankcard".equals(payType)) {
			data = new LinkedHashMap<>();
			data.put("status", "SUCCESS");
		} else {
			data = new LinkedHashMap<>();
			data.put("status", "SUCCESS");
		}

		String st = channelStatus(data != null ? data.get("status") : null);

		if ("SUCCESS".equals(st)) {
			try {
				transactionTemplate.executeWithoutResult(txStatus -> {
					LambdaUpdateWrapper<CommunityChiefCashWithdrawal> uw = new LambdaUpdateWrapper<>();
					uw.eq(CommunityChiefCashWithdrawal::getCompanyId, companyId)
							.eq(CommunityChiefCashWithdrawal::getId, cashWithdrawalId)
							.set(CommunityChiefCashWithdrawal::getStatus, "success");
					int n = communityChiefCashWithdrawalMapper.update(null, uw);
					if (n == 0) {
						throw new ResourceException("未查询到更新数据");
					}
				});
			} catch (ResourceException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new ResourceException("付款成功，服务器异常，请通过异常处理重试");
			}
			return;
		}
		if ("PROCESS".equals(st)) {
			LambdaUpdateWrapper<CommunityChiefCashWithdrawal> uw = new LambdaUpdateWrapper<>();
			uw.eq(CommunityChiefCashWithdrawal::getCompanyId, companyId)
					.eq(CommunityChiefCashWithdrawal::getId, cashWithdrawalId)
					.set(CommunityChiefCashWithdrawal::getStatus, "process");
			int n = communityChiefCashWithdrawalMapper.update(null, uw);
			if (n == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			return;
		}

		LambdaUpdateWrapper<CommunityChiefCashWithdrawal> revert = new LambdaUpdateWrapper<>();
		revert.eq(CommunityChiefCashWithdrawal::getCompanyId, companyId)
				.eq(CommunityChiefCashWithdrawal::getId, cashWithdrawalId)
				.set(CommunityChiefCashWithdrawal::getStatus, "apply");
		int rev = communityChiefCashWithdrawalMapper.update(null, revert);
		if (rev == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		String msg = errorDesc(data != null ? data.get("error_desc") : null);
		if (!StringUtils.hasText(msg)) {
			msg = "请检查微信支付相关配置是否完成";
		}
		throw new ResourceException(msg);
	}

	private void rejectCashWithdrawal(long companyId, long cashWithdrawalId, String remarks) {
		requireApplyWithdrawal(companyId, cashWithdrawalId);
		try {
			transactionTemplate.executeWithoutResult(txStatus -> {
				CommunityChiefCashWithdrawal patch = new CommunityChiefCashWithdrawal();
				patch.setStatus("reject");
				if (StringUtils.hasText(remarks)) {
					patch.setRemarks(remarks.trim());
				}
				LambdaUpdateWrapper<CommunityChiefCashWithdrawal> uw = new LambdaUpdateWrapper<>();
				uw.eq(CommunityChiefCashWithdrawal::getCompanyId, companyId)
						.eq(CommunityChiefCashWithdrawal::getId, cashWithdrawalId);
				int n = communityChiefCashWithdrawalMapper.update(patch, uw);
				if (n == 0) {
					throw new ResourceException("未查询到更新数据");
				}
			});
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException("系统错误，请稍后再试");
		}
	}

	private CommunityChiefCashWithdrawal requireApplyWithdrawal(long companyId, long cashWithdrawalId) {
		LambdaQueryWrapper<CommunityChiefCashWithdrawal> q = new LambdaQueryWrapper<>();
		q.eq(CommunityChiefCashWithdrawal::getCompanyId, companyId)
				.eq(CommunityChiefCashWithdrawal::getId, cashWithdrawalId);
		CommunityChiefCashWithdrawal info = communityChiefCashWithdrawalMapper.selectOne(q);
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
}
