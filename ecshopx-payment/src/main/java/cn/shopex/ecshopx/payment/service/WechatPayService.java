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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.common.cron.merchant.CronCommunityChiefCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.common.cron.merchant.CronDistributionCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeListForWechatQueryPort;
import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeRow;
import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeStatusWritePort;
import cn.shopex.ecshopx.common.cron.merchant.CronPopularizeCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.common.port.weixin.WechatBatchTransferQueryPort;
import cn.shopex.ecshopx.common.port.weixin.WechatMerchantV3ApiMaterial;
import cn.shopex.ecshopx.common.port.weixin.WechatPayMerchantContextPort;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatPayService {

	private static final String REL_COMMUNITY = "community_chief_cash_withdrawal";
	private static final String REL_REBATE = "rebate_cash_withdrawal";
	private static final String REL_POPULARIZE = "popularize_rebate_cash_withdrawal";

	private final CronMerchantPaymentTradeListForWechatQueryPort merchantPaymentTradeListPort;
	private final CronMerchantPaymentTradeStatusWritePort merchantPaymentTradeStatusWritePort;
	private final CronCommunityChiefCashWithdrawalStatusWritePort communityChiefCashWithdrawalStatusWritePort;
	private final CronDistributionCashWithdrawalStatusWritePort distributionCashWithdrawalStatusWritePort;
	private final CronPopularizeCashWithdrawalStatusWritePort popularizeCashWithdrawalStatusWritePort;
	private final WechatPayMerchantContextPort wechatPayMerchantContextPort;
	private final WechatBatchTransferQueryPort wechatBatchTransferQueryPort;

	public WechatPayService(
			CronMerchantPaymentTradeListForWechatQueryPort merchantPaymentTradeListPort,
			CronMerchantPaymentTradeStatusWritePort merchantPaymentTradeStatusWritePort,
			CronCommunityChiefCashWithdrawalStatusWritePort communityChiefCashWithdrawalStatusWritePort,
			CronDistributionCashWithdrawalStatusWritePort distributionCashWithdrawalStatusWritePort,
			CronPopularizeCashWithdrawalStatusWritePort popularizeCashWithdrawalStatusWritePort,
			WechatPayMerchantContextPort wechatPayMerchantContextPort,
			WechatBatchTransferQueryPort wechatBatchTransferQueryPort) {
		this.merchantPaymentTradeListPort = merchantPaymentTradeListPort;
		this.merchantPaymentTradeStatusWritePort = merchantPaymentTradeStatusWritePort;
		this.communityChiefCashWithdrawalStatusWritePort = communityChiefCashWithdrawalStatusWritePort;
		this.distributionCashWithdrawalStatusWritePort = distributionCashWithdrawalStatusWritePort;
		this.popularizeCashWithdrawalStatusWritePort = popularizeCashWithdrawalStatusWritePort;
		this.wechatPayMerchantContextPort = wechatPayMerchantContextPort;
		this.wechatBatchTransferQueryPort = wechatBatchTransferQueryPort;
	}

	public int scheduleQueryMerchantPayment() {
		List<CronMerchantPaymentTradeRow> rows = merchantPaymentTradeListPort.listWechatProcess(1, 100);
		if (rows.isEmpty()) {
			return 0;
		}
		int terminalWrites = 0;
		for (CronMerchantPaymentTradeRow row : rows) {
			WechatMerchantV3ApiMaterial material = wechatPayMerchantContextPort.requireForV3Api(row.getCompanyId());
			JsonNode result =
					wechatBatchTransferQueryPort.queryBalanceOrder(
							row.getCompanyId(), material, row.getPaymentNo());
			if (result == null || !result.has("transfer_batch") || result.get("transfer_batch").isNull()) {
				continue;
			}
			JsonNode batch = result.get("transfer_batch");
			String status = "PROCESS";
			String tradeStatus = null;
			String batchStatus = text(batch, "batch_status");
			if ("FINISHED".equals(batchStatus)) {
				int successNum = batch.path("success_num").asInt(0);
				if (successNum > 0) {
					status = "SUCCESS";
					tradeStatus = "success";
				} else {
					status = "FAIL";
					tradeStatus = "apply";
				}
			} else if ("CLOSED".equals(batchStatus)) {
				status = "FAIL";
				tradeStatus = "apply";
			}
			if ("PROCESS".equals(status)) {
				continue;
			}
			merchantPaymentTradeStatusWritePort.updateStatusOnly(
					row.getCompanyId(), row.getMerchantTradeId(), status);
			terminalWrites++;
			if (tradeStatus != null) {
				applySceneWithdrawal(row, tradeStatus);
			}
		}
		return terminalWrites;
	}

	private void applySceneWithdrawal(CronMerchantPaymentTradeRow row, String tradeStatus) {
		String name = row.getRelSceneName();
		if (!StringUtils.hasText(name)) {
			return;
		}
		Long sceneId = parseRelSceneId(row.getRelSceneId());
		if (sceneId == null) {
			return;
		}
		switch (name) {
			case REL_COMMUNITY -> communityChiefCashWithdrawalStatusWritePort.updateStatusById(
					sceneId, tradeStatus);
			case REL_REBATE -> distributionCashWithdrawalStatusWritePort.updateStatusById(sceneId, tradeStatus);
			case REL_POPULARIZE -> popularizeCashWithdrawalStatusWritePort.updateStatusById(
					sceneId, tradeStatus);
			default -> {
			}
		}
	}

	private static Long parseRelSceneId(String relSceneId) {
		if (!StringUtils.hasText(relSceneId)) {
			return null;
		}
		try {
			return Long.parseLong(relSceneId.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String text(JsonNode n, String field) {
		JsonNode v = n == null ? null : n.get(field);
		if (v == null || v.isNull()) {
			return null;
		}
		return v.asText(null);
	}
}
