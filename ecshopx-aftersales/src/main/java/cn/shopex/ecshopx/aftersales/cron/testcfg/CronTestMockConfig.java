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

package cn.shopex.ecshopx.aftersales.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopAdapayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.cron.mock.NoopAdminOrderDetailSalespersonLookupPort;
import cn.shopex.ecshopx.common.cron.mock.NoopAftersalesAutoRefuseWxaTemplatePort;
import cn.shopex.ecshopx.common.cron.mock.NoopAftersalesCancelNoticeJobPort;
import cn.shopex.ecshopx.common.cron.mock.NoopAftersalesRefundJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAftersalesSuccessSendMsgJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopOrderRefundCompleteJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAftersaleFinancialExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAftersaleRecordCountExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopRefundRecordCountExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopSendAfterSaleCancelNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopSendAfterSaleWaitConfirmNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopRefundByOrderUpdateOrderStatusJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopTradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopSendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopBspayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.cron.mock.NoopDistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopDistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.cron.mock.NoopInvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopJushuitanSettingReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopOrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopOrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopTradeByIdReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopBargainOrderActivityStatusPort;
import cn.shopex.ecshopx.common.cron.mock.NoopPaymentProviderRefundPort;
import cn.shopex.ecshopx.common.cron.mock.NoopOperatorInfoReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopOrderNormalOrderServiceOrderDataReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopSupplierOperatorInfoReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopSupplierOperatorRowByOperatorIdsReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopTradeRefundFinishExternalNotifyPort;
import cn.shopex.ecshopx.common.cron.payment.AdapayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.cron.payment.BspayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailSalespersonLookupPort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAutoRefuseWxaTemplatePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCancelNoticeJobPort;
import cn.shopex.ecshopx.common.dispatch.RefundByOrderUpdateOrderStatusJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AftersalesRefundJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AftersalesSuccessSendMsgJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrderRefundCompleteJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AftersaleFinancialExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AftersaleRecordCountExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.RefundRecordCountExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleCancelNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitConfirmNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.payment.PaymentProviderRefundPort;
import cn.shopex.ecshopx.common.port.thirdparty.TradeRefundFinishExternalNotifyPort;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.TradeByIdReadPort;
import cn.shopex.ecshopx.common.port.promotions.BargainOrderActivityStatusPort;
import cn.shopex.ecshopx.common.port.companys.OperatorInfoReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderServiceOrderDataReadPort;
import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorRowByOperatorIdsReadPort;
import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorInfoReadPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖；仅 spring.profiles.active 含 test-cron 时生效。
 */
@Profile("test-cron")
@Configuration("aftersalesCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 替换退款 Job 的 Bus 投递实现，避免 test-cron 下写 dispatch Redis 键。
	 */
	@Bean
	@Primary
	public AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher() {
		return new NoopAftersalesRefundJobDispatchPublisher();
	}

	@Bean
	@Primary
	public TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher() {
		return new NoopTradeRefundStatisticsJobDispatchPublisher();
	}

	@Bean
	@Primary
	public RefundByOrderUpdateOrderStatusJobDispatchPublisher refundByOrderUpdateOrderStatusJobDispatchPublisher() {
		return new NoopRefundByOrderUpdateOrderStatusJobDispatchPublisher();
	}

	@Bean
	@Primary
	public SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher() {
		return new NoopSendAfterSaleWaitDealNoticeJobDispatchPublisher();
	}

	@Bean
	@Primary
	public SendAfterSaleWaitConfirmNoticeJobDispatchPublisher sendAfterSaleWaitConfirmNoticeJobDispatchPublisher() {
		return new NoopSendAfterSaleWaitConfirmNoticeJobDispatchPublisher();
	}

	@Bean
	@Primary
	public SendAfterSaleCancelNoticeJobDispatchPublisher sendAfterSaleCancelNoticeJobDispatchPublisher() {
		return new NoopSendAfterSaleCancelNoticeJobDispatchPublisher();
	}

	@Bean
	@Primary
	public RefundRecordCountExportFileJobDispatchPublisher refundRecordCountExportFileJobDispatchPublisher() {
		return new NoopRefundRecordCountExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public AftersaleRecordCountExportFileJobDispatchPublisher aftersaleRecordCountExportFileJobDispatchPublisher() {
		return new NoopAftersaleRecordCountExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public AftersaleFinancialExportFileJobDispatchPublisher aftersaleFinancialExportFileJobDispatchPublisher() {
		return new NoopAftersaleFinancialExportFileJobDispatchPublisher();
	}

	/**
	 * 替换仅退款完成后异步 Job 的 Bus 投递，避免 test-cron 下写 dispatch Redis 键。
	 */
	@Bean
	@Primary
	public OrderRefundCompleteJobDispatchPublisher orderRefundCompleteJobDispatchPublisher() {
		return new NoopOrderRefundCompleteJobDispatchPublisher();
	}

	@Bean
	@Primary
	public AftersalesSuccessSendMsgJobDispatchPublisher aftersalesSuccessSendMsgJobDispatchPublisher() {
		return new NoopAftersalesSuccessSendMsgJobDispatchPublisher();
	}

	@Bean
	@Primary
	public InvoiceRedJobDispatchPublisher invoiceRedJobDispatchPublisher() {
		return new NoopInvoiceRedJobDispatchPublisher();
	}

	/**
	 * 预留消费端 <code>doRefund</code> 支付外呼的 Noop，阶段 4 可 grep
	 * <code>[cron-mock][payment-do-refund]</code> 断言；非 <code>schedule_refund</code> 同步路径所必需。
	 */
	@Bean
	@Primary
	public PaymentProviderRefundPort paymentProviderRefundPort() {
		return new NoopPaymentProviderRefundPort();
	}

	/**
	 * 预留 <code>TradeRefundFinish</code> 外发链路的 Noop；与慢队列排程 handler 不直接相关。
	 */
	@Bean
	@Primary
	public TradeRefundFinishExternalNotifyPort tradeRefundFinishExternalNotifyPort() {
		return new NoopTradeRefundFinishExternalNotifyPort();
	}

	/**
	 * 覆盖 {@code AftersalesAutoRefuseWxaTemplatePortImpl}，避免对微信侧发请求。
	 */
	@Bean
	@Primary
	public AftersalesAutoRefuseWxaTemplatePort aftersalesAutoRefuseWxaTemplatePort() {
		return new NoopAftersalesAutoRefuseWxaTemplatePort();
	}

	/**
	 * 覆盖汇付 Ada 支付确认外发，避免 test-cron 下真实打 Ada HTTP。
	 */
	@Bean
	@Primary
	public AdapayPaymentConfirmHttpGateway adapayPaymentConfirmHttpGateway() {
		return new NoopAdapayPaymentConfirmHttpGateway();
	}

	/**
	 * 覆盖斗拱延迟交易确认外发，避免 test-cron 下真实打斗拱 HTTP。
	 */
	@Bean
	@Primary
	public BspayPaymentConfirmHttpGateway bspayPaymentConfirmHttpGateway() {
		return new NoopBspayPaymentConfirmHttpGateway();
	}

	/**
	 * 替换售后取消通知外排，避免 test-cron 下真实投 MQ。
	 */
	@Bean
	@Primary
	public AftersalesCancelNoticeJobPort aftersalesCancelNoticeJobPort() {
		return new NoopAftersalesCancelNoticeJobPort();
	}

	/**
	 * 替换 {@code cn.shopex.ecshopx.distribution.integration.DistributorDefaultAftersalesAddressReadPortImpl}，
	 * test-cron 下避免查库/缓存，保证 Cron 单测与阶段 4 快照稳定。
	 */
	@Bean
	@Primary
	public DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort() {
		return new NoopDistributorDefaultAftersalesAddressReadPort();
	}

	/**
	 * 替换 {@code cn.shopex.ecshopx.salesperson.service.AdminOrderDetailSalespersonLookupPortImpl}，
	 * test-cron 下不外读业务员信息。
	 */
	@Bean
	@Primary
	public AdminOrderDetailSalespersonLookupPort adminOrderDetailSalespersonLookupPort() {
		return new NoopAdminOrderDetailSalespersonLookupPort();
	}

	/**
	 * 替换 {@code cn.shopex.ecshopx.orders.integration.OrderNormalOrderHeaderReadPortImpl}，
	 * test-cron 下不外读订单头。
	 */
	@Bean
	@Primary
	public OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort() {
		return new NoopOrderNormalOrderHeaderReadPort();
	}

	@Bean
	@Primary
	public TradeByIdReadPort tradeByIdReadPort() {
		return new NoopTradeByIdReadPort();
	}

	@Bean
	@Primary
	public BargainOrderActivityStatusPort bargainOrderActivityStatusPort() {
		return new NoopBargainOrderActivityStatusPort();
	}

	/**
	 * 替换 {@code cn.shopex.ecshopx.orders.integration.OrderNormalOrderItemsReadPortImpl}，
	 * test-cron 下不外读订单明细。
	 */
	@Bean
	@Primary
	public OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort() {
		return new NoopOrderNormalOrderItemsReadPort();
	}

	/**
	 * 替换 {@code cn.shopex.ecshopx.distribution.service.DistributorGetInfoSimpleByDistributorIdPortImpl}，
	 * test-cron 下不外读分销商摘要。
	 */
	@Bean
	@Primary
	public DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort() {
		return new NoopDistributorGetInfoSimpleByDistributorIdPort();
	}

	/**
	 * 替换 {@code cn.shopex.ecshopx.orders.integration.JushuitanSettingReadPortImpl}，
	 * test-cron 下不外读聚水潭配置。
	 */
	@Bean
	@Primary
	public JushuitanSettingReadPort jushuitanSettingReadPort() {
		return new NoopJushuitanSettingReadPort();
	}

	/**
	 * 替换 {@code cn.shopex.ecshopx.supplier.integration.SupplierOperatorInfoReadPortImpl}，
	 * test-cron 下不外读供应商操作人信息。
	 */
	@Bean
	@Primary
	public SupplierOperatorInfoReadPort supplierOperatorInfoReadPort() {
		return new NoopSupplierOperatorInfoReadPort();
	}

	@Bean
	@Primary
	public OrderNormalOrderServiceOrderDataReadPort orderNormalOrderServiceOrderDataReadPort() {
		return new NoopOrderNormalOrderServiceOrderDataReadPort();
	}

	@Bean
	@Primary
	public OperatorInfoReadPort operatorInfoReadPort() {
		return new NoopOperatorInfoReadPort();
	}

	@Bean
	@Primary
	public SupplierOperatorRowByOperatorIdsReadPort supplierOperatorRowByOperatorIdsReadPort() {
		return new NoopSupplierOperatorRowByOperatorIdsReadPort();
	}
}
