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

package cn.shopex.ecshopx.common.refund;

import java.util.Locale;

/**
 * 售后原路退款调度上下文（由各支付渠道执行器消费）。
 */
public final class AftersalesRefundPaymentContext {

	private final long companyId;
	private final String wxaAppId;
	private final long refundBn;
	private final long orderId;
	private final long userId;
	private final long shopId;
	private final long distributorId;
	private final long merchantId;
	private final long supplierId;
	private final String payTypeRaw;
	private final String payChannel;
	private final String tradeId;
	private final String currency;
	private final int payFeeFen;
	private final int refundFeeFen;
	private final String bspayReqDate;
	private final String transactionId;
	private final boolean resubmit;
	private final String hfOrderId;
	private final String hfpayOrgOrderDateYmd;
	private final String hfpayRefundOrderDateYmd;
	private final int hfpayOrderIsProfitsharing;

	public AftersalesRefundPaymentContext(
			long companyId,
			String wxaAppId,
			long refundBn,
			long orderId,
			long userId,
			long shopId,
			long distributorId,
			long merchantId,
			long supplierId,
			String payTypeRaw,
			String payChannel,
			String tradeId,
			int payFeeFen,
			int refundFeeFen,
			String bspayReqDate,
			String transactionId,
			boolean resubmit,
			String hfOrderId,
			String hfpayOrgOrderDateYmd,
			String hfpayRefundOrderDateYmd,
			int hfpayOrderIsProfitsharing) {
		this(companyId, wxaAppId, refundBn, orderId, userId, shopId, distributorId, merchantId,
				supplierId, payTypeRaw, payChannel, tradeId, "", payFeeFen, refundFeeFen, bspayReqDate,
				transactionId, resubmit, hfOrderId, hfpayOrgOrderDateYmd, hfpayRefundOrderDateYmd,
				hfpayOrderIsProfitsharing);
	}

	public AftersalesRefundPaymentContext(
			long companyId,
			String wxaAppId,
			long refundBn,
			long orderId,
			long userId,
			long shopId,
			long distributorId,
			long merchantId,
			long supplierId,
			String payTypeRaw,
			String payChannel,
			String tradeId,
			String currency,
			int payFeeFen,
			int refundFeeFen,
			String bspayReqDate,
			String transactionId,
			boolean resubmit,
			String hfOrderId,
			String hfpayOrgOrderDateYmd,
			String hfpayRefundOrderDateYmd,
			int hfpayOrderIsProfitsharing) {
		this.companyId = companyId;
		this.wxaAppId = wxaAppId == null ? "" : wxaAppId;
		this.refundBn = refundBn;
		this.orderId = orderId;
		this.userId = userId;
		this.shopId = shopId;
		this.distributorId = distributorId;
		this.merchantId = merchantId;
		this.supplierId = supplierId;
		this.payTypeRaw = payTypeRaw == null ? "" : payTypeRaw;
		this.payChannel = payChannel == null ? "" : payChannel;
		this.tradeId = tradeId == null ? "" : tradeId;
		this.currency = currency == null ? "" : currency;
		this.payFeeFen = payFeeFen;
		this.refundFeeFen = refundFeeFen;
		this.bspayReqDate = bspayReqDate == null ? "" : bspayReqDate;
		this.transactionId = transactionId == null ? "" : transactionId;
		this.resubmit = resubmit;
		this.hfOrderId = hfOrderId == null ? "" : hfOrderId;
		this.hfpayOrgOrderDateYmd = hfpayOrgOrderDateYmd == null ? "" : hfpayOrgOrderDateYmd;
		this.hfpayRefundOrderDateYmd = hfpayRefundOrderDateYmd == null ? "" : hfpayRefundOrderDateYmd;
		this.hfpayOrderIsProfitsharing = hfpayOrderIsProfitsharing;
	}

	public String payTypeLower() {
		return payTypeRaw.toLowerCase(Locale.ROOT);
	}

	public long getCompanyId() {
		return companyId;
	}

	public String getWxaAppId() {
		return wxaAppId;
	}

	public long getRefundBn() {
		return refundBn;
	}

	public long getOrderId() {
		return orderId;
	}

	public long getUserId() {
		return userId;
	}

	public long getShopId() {
		return shopId;
	}

	public long getDistributorId() {
		return distributorId;
	}

	public long getMerchantId() {
		return merchantId;
	}

	public long getSupplierId() {
		return supplierId;
	}

	public String getPayTypeRaw() {
		return payTypeRaw;
	}

	public String getPayChannel() {
		return payChannel;
	}

	public String getTradeId() {
		return tradeId;
	}

	public String getCurrency() {
		return currency;
	}

	public int getPayFeeFen() {
		return payFeeFen;
	}

	public int getRefundFeeFen() {
		return refundFeeFen;
	}

	public String getBspayReqDate() {
		return bspayReqDate;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public boolean isResubmit() {
		return resubmit;
	}

	public String getHfOrderId() {
		return hfOrderId;
	}

	public String getHfpayOrgOrderDateYmd() {
		return hfpayOrgOrderDateYmd;
	}

	public String getHfpayRefundOrderDateYmd() {
		return hfpayRefundOrderDateYmd;
	}

	public int getHfpayOrderIsProfitsharing() {
		return hfpayOrderIsProfitsharing;
	}
}
