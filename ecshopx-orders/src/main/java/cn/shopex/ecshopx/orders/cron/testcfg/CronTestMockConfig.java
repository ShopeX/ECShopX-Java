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

package cn.shopex.ecshopx.orders.cron.testcfg;

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalRetFileAccessPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteDownloadPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRetSignVerifyPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsPaymentSettingLoadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsDivisionLocalArtifactWriterPort;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsDivisionLocalRetFileAccessPort;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsDivisionRemoteDownloadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsDivisionRemoteUploadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsDivisionRetSignVerifyPort;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsPaymentSettingLoadPort;
import cn.shopex.ecshopx.common.dispatch.ChinaumsDivisionDetailExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ChinaumsDivisionListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ConsumptionOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopLuckyDrawLogExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsDivisionDetailExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopChinaumsDivisionListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopOfflinePaymentExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopOrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopStatementDetailsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopStatementsSummarizedExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.FinishOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceCreateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OfflinePaymentExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopFinishOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopInvoiceQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopInvoiceRedQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopMemberTotalConsumptionMutatePort;
import cn.shopex.ecshopx.common.cron.port.MemberTotalConsumptionMutatePort;
import cn.shopex.ecshopx.common.cron.mock.NoopInvoiceCreateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.GenerateStatementsJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.LuckyDrawLogExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoicePushOmsJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendInvoiceEmailJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.StatementDetailsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.StatementsSummarizedExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopConsumptionOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopDmCrmManualPointChangePort;
import cn.shopex.ecshopx.common.cron.mock.NoopGenerateStatementsJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopInvoicePushOmsJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopSendInvoiceEmailJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopHfPayPaymentSettingService;
import cn.shopex.ecshopx.common.cron.mock.NoopPointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.common.cron.mock.NoopStatementSettlementCursorRedis;
import cn.shopex.ecshopx.common.cron.mock.NoopTurntablePayGetTimesOnOrderPort;
import cn.shopex.ecshopx.common.cron.port.TurntablePayGetTimesOnOrderPort;
import cn.shopex.ecshopx.common.cron.statement.StatementSettlementCursorRedisPort;
import cn.shopex.ecshopx.common.hfpay.payment.HfPayPaymentSettingLoadPort;
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmManualPointChangePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖配置；仅在 spring.profiles.active 包含 test-cron 时生效。
 * 作用：把"不影响 DB diff"的副作用 bean 替换为 Noop 实现，避免阶段 4 快照对比期间真写 Redis / 调第三方。
 * 已声明 @Bean @Primary 的 Noop bean：
 * - NoopDmCrmManualPointChangePort（cancel-group-orders）—— 覆盖 ecshopx-third-party DmCrmManualPointChangeNoOp
 * - NoopPointsmallPartialCancelItemStorePort（cancel-pointsmall-orders）—— 覆盖 goods 侧 PointsmallPartialCancelItemStorePortImpl
	 * - NoopHfPayPaymentSettingService（HfPayPaymentSettingLoadPort，share-order-profit）—— 覆盖汇付 loadForCompany：Redis/证书
	 * - NoopChinaums*（划付 upload）—— 覆盖 UMS SFTP/本地落盘/Redis 读支付配置
	 * - NoopMemberTotalConsumptionMutatePort（consumption-orders）—— 覆盖会员累计消费 Redis 写入
	 * - NoopTurntablePayGetTimesOnOrderPort（finish-orders）—— 覆盖大转盘 hincrby Redis
	 * - NoopInvoiceCreateJobDispatchPublisher（create-invoice）—— 避免 test-cron 真投递发票创建 Bus job
	 * - NoopInvoiceQueryJobDispatchPublisher（query-invoice）—— 避免 test-cron 真投递蓝票查询 Bus job
	 * - NoopFinishOrderJobDispatchPublisher（finish-orders）—— 避免 test-cron 向 slow 真投递自动完成订单 Bus job
	 * - NoopConsumptionOrderJobDispatchPublisher（consumption-orders）—— 避免 test-cron 向 slow 真投递消费累加 Bus job
	 * - NoopInvoiceRedQueryJobDispatchPublisher（query-invoice-red）—— 避免 test-cron 真投递红冲查询 Bus job
	 * - NoopGenerateStatementsJobDispatchPublisher（generate-statements）—— 避免 test-cron 真写结算单 job 慢队列
	 * - NoopInvoicePushOmsJobDispatchPublisher（invoice-push-oms）—— 避免 test-cron 真写发票推送 OMS job 默认队列
	 * - NoopSendInvoiceEmailJobDispatchPublisher（send-invoice-email）—— 避免 test-cron 真写发送发票邮件 job 默认队列
	 * - NoopOfflinePaymentExportFileJobDispatchPublisher（offline_payment 导出）—— 避免 test-cron 向 slow 真投递线下转账导出 file job
	 * - NoopOrderListExportFileJobDispatchPublisher（订单列表导出）—— 避免 test-cron 向 slow 真投递订单列表导出 file job
	 * - NoopStatementsSummarizedExportFileJobDispatchPublisher（结算汇总导出）—— 避免 test-cron 向 slow 真投递结算汇总导出 file job
	 * - NoopStatementDetailsExportFileJobDispatchPublisher（结算明细导出）—— 避免 test-cron 向 slow 真投递结算明细导出 file job
	 * - NoopLuckyDrawLogExportFileJobDispatchPublisher（大转盘中奖记录导出）—— 避免 test-cron 向 slow 真投递大转盘导出 file job
	 * - NoopChinaumsDivisionListExportFileJobDispatchPublisher（分账单列表导出）—— 避免 test-cron 向 slow 真投递分账单列表导出 file job
	 * - NoopChinaumsDivisionDetailExportFileJobDispatchPublisher（分账单明细导出）—— 避免 test-cron 向 slow 真投递分账单明细导出 file job
	 */
@Profile("test-cron")
@Configuration("ordersCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 替换 {@link MemberTotalConsumptionMutatePort} 生产实现，避免阶段 4 快照对比时污染 totalConsumption Redis。
	 */
	@Bean
	@Primary
	public MemberTotalConsumptionMutatePort memberTotalConsumptionMutatePort() {
		return new NoopMemberTotalConsumptionMutatePort();
	}

	/**
	 * 替换 DmCrmManualPointChangePort 真实实现，避免阶段 4 DB 快照期间真调达摩 CRM 积分接口。
	 */
	@Bean
	@Primary
	public DmCrmManualPointChangePort dmCrmManualPointChangePort() {
		return new NoopDmCrmManualPointChangePort();
	}

	/**
	 * 替换真实 PointsmallPartialCancelItemStorePort，避免阶段 4 双端比对时写 Redis pointsmall_item_store 与 pointsmall_items。
	 */
	@Bean
	@Primary
	public PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort() {
		return new NoopPointsmallPartialCancelItemStorePort();
	}

	/**
	 * 替换仅 {@link HfPayPaymentSettingLoadPort}（如 OrderProfitSharingService#getOrderHfAccount），避免 test-cron
	 * 读 companys Redis/写证书记录。其它仍注入具体 {@code HfPayPaymentSettingService} 的 bean 不受影响。
	 */
	@Bean
	@Primary
	public HfPayPaymentSettingLoadPort hfPayPaymentSettingLoadPort() {
		return new NoopHfPayPaymentSettingService();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.orders.integration.statement.StatementSettlementCursorRedisAdapter}，避免
	 * test-cron 双端对比时污染共享 Redis。
	 */
	@Bean
	@Primary
	public StatementSettlementCursorRedisPort statementSettlementCursorRedisPort() {
		return new NoopStatementSettlementCursorRedis();
	}

	/**
	 * 替换生产 {@link ChinaumsDivisionRemoteUploadPort}，避免阶段 4 真联 UMS SFTP。
	 */
	@Bean
	@Primary
	public ChinaumsDivisionRemoteUploadPort chinaumsDivisionRemoteUploadPort() {
		return new NoopChinaumsDivisionRemoteUploadPort();
	}

	/**
	 * 替换生产 {@link ChinaumsDivisionLocalArtifactWriterPort}，避免阶段 4 在 storage 下真写大文件。
	 */
	@Bean
	@Primary
	public ChinaumsDivisionLocalArtifactWriterPort chinaumsDivisionLocalArtifactWriterPort() {
		return new NoopChinaumsDivisionLocalArtifactWriterPort();
	}

	/**
	 * 替换生产 {@link ChinaumsPaymentSettingLoadPort}，避免 test-cron 下读 companys Redis 银联划付键。
	 */
	@Bean
	@Primary
	public ChinaumsPaymentSettingLoadPort chinaumsPaymentSettingLoadPort() {
		return new NoopChinaumsPaymentSettingLoadPort();
	}

	/**
	 * 替换生产 {@link ChinaumsDivisionRemoteDownloadPort}，避免阶段 4 真联 UMS 拉取回盘文件。
	 */
	@Bean
	@Primary
	public ChinaumsDivisionRemoteDownloadPort chinaumsDivisionRemoteDownloadPort() {
		return new NoopChinaumsDivisionRemoteDownloadPort();
	}

	/**
	 * 替换生产 {@link ChinaumsDivisionRetSignVerifyPort}，避免阶段 4 验签时读公钥/拉 .chk。
	 */
	@Bean
	@Primary
	public ChinaumsDivisionRetSignVerifyPort chinaumsDivisionRetSignVerifyPort() {
		return new NoopChinaumsDivisionRetSignVerifyPort();
	}

	/**
	 * 替换生产 {@link ChinaumsDivisionLocalRetFileAccessPort}，避免阶段 4 在 storage 下真读/写日终
	 * .ret。
	 */
	@Bean
	@Primary
	public ChinaumsDivisionLocalRetFileAccessPort chinaumsDivisionLocalRetFileAccessPort() {
		return new NoopChinaumsDivisionLocalRetFileAccessPort();
	}

	/**
	 * 替换生产 {@link TurntablePayGetTimesOnOrderPort}，避免 test-cron 下写大转盘剩余次数 Redis。
	 */
	@Bean
	@Primary
	public TurntablePayGetTimesOnOrderPort turntablePayGetTimesOnOrderPort() {
		return new NoopTurntablePayGetTimesOnOrderPort();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.InvoiceCreateJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递发票创建 job。
	 */
	@Bean
	@Primary
	public InvoiceCreateJobDispatchPublisher invoiceCreateJobDispatchPublisher() {
		return new NoopInvoiceCreateJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.InvoiceQueryJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递蓝票查询 job。
	 */
	@Bean
	@Primary
	public InvoiceQueryJobDispatchPublisher invoiceQueryJobDispatchPublisher() {
		return new NoopInvoiceQueryJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.FinishOrderJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递自动完成订单 job。
	 */
	@Bean
	@Primary
	public FinishOrderJobDispatchPublisher finishOrderJobDispatchPublisher() {
		return new NoopFinishOrderJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.ConsumptionOrderJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递消费累加 job。
	 */
	@Bean
	@Primary
	public ConsumptionOrderJobDispatchPublisher consumptionOrderJobDispatchPublisher() {
		return new NoopConsumptionOrderJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.InvoiceRedQueryJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递红冲查询 job。
	 */
	@Bean
	@Primary
	public InvoiceRedQueryJobDispatchPublisher invoiceRedQueryJobDispatchPublisher() {
		return new NoopInvoiceRedQueryJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.GenerateStatementsJobDispatchPublisherImpl}，避免向 Redis
	 * 结算单慢队列真投递。
	 */
	@Bean
	@Primary
	public GenerateStatementsJobDispatchPublisher generateStatementsJobDispatchPublisher() {
		return new NoopGenerateStatementsJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.InvoicePushOmsJobDispatchPublisherImpl}，避免向 Redis 默认队列真投递
	 * 用户开票后的 OMS 同步 job。
	 */
	@Bean
	@Primary
	public InvoicePushOmsJobDispatchPublisher invoicePushOmsJobDispatchPublisher() {
		return new NoopInvoicePushOmsJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.SendInvoiceEmailJobDispatchPublisherImpl}，避免向 Redis 默认队列真投递
	 * 发送发票邮件 job。
	 */
	@Bean
	@Primary
	public SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher() {
		return new NoopSendInvoiceEmailJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.OfflinePaymentExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递线下转账导出 job。
	 */
	@Bean
	@Primary
	public OfflinePaymentExportFileJobDispatchPublisher offlinePaymentExportFileJobDispatchPublisher() {
		return new NoopOfflinePaymentExportFileJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.OrderListExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递订单列表导出 job。
	 */
	@Bean
	@Primary
	public OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher() {
		return new NoopOrderListExportFileJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.StatementsSummarizedExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递结算汇总导出 job。
	 */
	@Bean
	@Primary
	public StatementsSummarizedExportFileJobDispatchPublisher statementsSummarizedExportFileJobDispatchPublisher() {
		return new NoopStatementsSummarizedExportFileJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.StatementDetailsExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递结算明细导出 job。
	 */
	@Bean
	@Primary
	public StatementDetailsExportFileJobDispatchPublisher statementDetailsExportFileJobDispatchPublisher() {
		return new NoopStatementDetailsExportFileJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.LuckyDrawLogExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递大转盘中奖记录导出 job。
	 */
	@Bean
	@Primary
	public LuckyDrawLogExportFileJobDispatchPublisher luckyDrawLogExportFileJobDispatchPublisher() {
		return new NoopLuckyDrawLogExportFileJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.ChinaumsDivisionListExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递分账单列表导出 job。
	 */
	@Bean
	@Primary
	public ChinaumsDivisionListExportFileJobDispatchPublisher chinaumsDivisionListExportFileJobDispatchPublisher() {
		return new NoopChinaumsDivisionListExportFileJobDispatchPublisher();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.ChinaumsDivisionDetailExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 队列真投递分账单明细导出 job。
	 */
	@Bean
	@Primary
	public ChinaumsDivisionDetailExportFileJobDispatchPublisher chinaumsDivisionDetailExportFileJobDispatchPublisher() {
		return new NoopChinaumsDivisionDetailExportFileJobDispatchPublisher();
	}
}
