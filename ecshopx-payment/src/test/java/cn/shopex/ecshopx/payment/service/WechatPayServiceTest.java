package cn.shopex.ecshopx.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.merchant.CronCommunityChiefCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.common.cron.merchant.CronDistributionCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeListForWechatQueryPort;
import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeRow;
import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeStatusWritePort;
import cn.shopex.ecshopx.common.cron.merchant.CronPopularizeCashWithdrawalStatusWritePort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.weixin.WechatBatchTransferQueryPort;
import cn.shopex.ecshopx.common.port.weixin.WechatMerchantV3ApiMaterial;
import cn.shopex.ecshopx.common.port.weixin.WechatPayMerchantContextPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.KeyPairGenerator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * analysis §3 可追溯编号：4.10（relSceneName + batch_status 的 switch/组合分派）由带 4.5.x / 4.6 / 4.9 / 4.10.x 的
 * {@link DisplayName} 的 Nested 共同覆盖，见各 {@code @DisplayName}。
 */
@ExtendWith(MockitoExtension.class)
class WechatPayServiceTest {

	private static final String REL_COMMUNITY = "community_chief_cash_withdrawal";
	private static final String REL_REBATE = "rebate_cash_withdrawal";
	private static final String REL_POPULARIZE = "popularize_rebate_cash_withdrawal";

	private static final WechatMerchantV3ApiMaterial MAT = sampleMaterial();
	private static final ObjectMapper M = new ObjectMapper();

	private static WechatMerchantV3ApiMaterial sampleMaterial() {
		try {
			KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
			g.initialize(512);
			return new WechatMerchantV3ApiMaterial("m", "01", g.generateKeyPair().getPrivate());
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Mock
	private CronMerchantPaymentTradeListForWechatQueryPort merchantPaymentTradeListPort;

	@Mock
	private CronMerchantPaymentTradeStatusWritePort merchantPaymentTradeStatusWritePort;

	@Mock
	private CronCommunityChiefCashWithdrawalStatusWritePort communityChiefCashWithdrawalStatusWritePort;

	@Mock
	private CronDistributionCashWithdrawalStatusWritePort distributionCashWithdrawalStatusWritePort;

	@Mock
	private CronPopularizeCashWithdrawalStatusWritePort popularizeCashWithdrawalStatusWritePort;

	@Mock
	private WechatPayMerchantContextPort wechatPayMerchantContextPort;

	@Mock
	private WechatBatchTransferQueryPort wechatBatchTransferQueryPort;

	@InjectMocks
	private WechatPayService wechatPayService;

	private static JsonNode responseWithBatch(String batchStatus, int successNum) {
		ObjectNode root = M.createObjectNode();
		ObjectNode tb = root.putObject("transfer_batch");
		tb.put("batch_status", batchStatus);
		tb.put("success_num", successNum);
		return root;
	}

	private static JsonNode noBatch() {
		return M.createObjectNode();
	}

	private static CronMerchantPaymentTradeRow row(
			String merchantId, long companyId, String relSceneName, String relSceneId, String paymentNo) {
		return new CronMerchantPaymentTradeRow(merchantId, companyId, relSceneId, relSceneName, paymentNo);
	}

	@Nested
	@DisplayName("3 空列表早退")
	class EmptyList {

		@Test
		void returns_zero_with_no_side_effects() {
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of());
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(0);
			verify(wechatPayMerchantContextPort, never()).requireForV3Api(anyLong());
			verify(wechatBatchTransferQueryPort, never())
					.queryBalanceOrder(anyLong(), any(WechatMerchantV3ApiMaterial.class), any());
			verify(merchantPaymentTradeStatusWritePort, never()).updateStatusOnly(anyLong(), any(), any());
		}
	}

	@Nested
	@DisplayName("4+4.1+4.1.2+4.2+4.3 无 transfer_batch 不写主表（4.1.2 配置有效）")
	class NoTransferBatch {

		@Test
		void skips_update() {
			CronMerchantPaymentTradeRow r = row("m1", 1L, REL_COMMUNITY, "10", "pno");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(r));
			when(wechatPayMerchantContextPort.requireForV3Api(1L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(1L, MAT, "pno")).thenReturn(noBatch());
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(0);
			verify(merchantPaymentTradeStatusWritePort, never()).updateStatusOnly(anyLong(), any(), any());
		}
	}

	@Nested
	@DisplayName("4.5.1+4.9+4.10.1 FINISHED+success+团长")
	class FinishedSuccessCommunity {

		@Test
		void updates_main_and_community() {
			CronMerchantPaymentTradeRow r = row("mt", 2L, REL_COMMUNITY, "99", "out1");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(r));
			when(wechatPayMerchantContextPort.requireForV3Api(2L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(2L, MAT, "out1"))
					.thenReturn(responseWithBatch("FINISHED", 1));
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(1);
			verify(merchantPaymentTradeStatusWritePort, times(1))
					.updateStatusOnly(2L, "mt", "SUCCESS");
			verify(communityChiefCashWithdrawalStatusWritePort, times(1)).updateStatusById(99L, "success");
			verify(distributionCashWithdrawalStatusWritePort, never()).updateStatusById(anyLong(), any());
		}
	}

	@Nested
	@DisplayName("4.5.2+4.10.2 FINISHED+success_num=0（4.10.2 分销）")
	class FinishedFailBySuccessZero {

		@Test
		void status_fail_and_trade_apply() {
			CronMerchantPaymentTradeRow r = row("mt2", 3L, REL_REBATE, "5", "out2");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(r));
			when(wechatPayMerchantContextPort.requireForV3Api(3L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(3L, MAT, "out2"))
					.thenReturn(responseWithBatch("FINISHED", 0));
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(1);
			verify(merchantPaymentTradeStatusWritePort, times(1))
					.updateStatusOnly(3L, "mt2", "FAIL");
			verify(distributionCashWithdrawalStatusWritePort, times(1)).updateStatusById(5L, "apply");
		}
	}

	@Nested
	@DisplayName("4.6+4.10.3 CLOSED（4.10.3 推客）")
	class Closed {

		@Test
		void same_as_fail() {
			CronMerchantPaymentTradeRow r = row("mt3", 4L, REL_POPULARIZE, "6", "out3");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(r));
			when(wechatPayMerchantContextPort.requireForV3Api(4L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(4L, MAT, "out3"))
					.thenReturn(responseWithBatch("CLOSED", 0));
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(1);
			verify(merchantPaymentTradeStatusWritePort, times(1))
					.updateStatusOnly(4L, "mt3", "FAIL");
			verify(popularizeCashWithdrawalStatusWritePort, times(1)).updateStatusById(6L, "apply");
		}
	}

	@Nested
	@DisplayName("4.7+4.8 非终态不更新")
	class NotTerminal {

		@Test
		void processing_leaves_merchant_process() {
			CronMerchantPaymentTradeRow r = row("mt4", 5L, REL_COMMUNITY, "1", "out4");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(r));
			when(wechatPayMerchantContextPort.requireForV3Api(5L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(5L, MAT, "out4"))
					.thenReturn(responseWithBatch("PROCESSING", 0));
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(0);
			verify(merchantPaymentTradeStatusWritePort, never()).updateStatusOnly(anyLong(), any(), any());
		}
	}

	@Nested
	@DisplayName("4.10.4 未知场景仅主表")
	class OtherScene {

		@Test
		void only_merchant_table() {
			CronMerchantPaymentTradeRow r = row("mt5", 6L, "unknown", "1", "out5");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(r));
			when(wechatPayMerchantContextPort.requireForV3Api(6L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(6L, MAT, "out5"))
					.thenReturn(responseWithBatch("FINISHED", 1));
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(1);
			verify(merchantPaymentTradeStatusWritePort, times(1))
					.updateStatusOnly(6L, "mt5", "SUCCESS");
			verify(communityChiefCashWithdrawalStatusWritePort, never()).updateStatusById(anyLong(), any());
			verify(distributionCashWithdrawalStatusWritePort, never()).updateStatusById(anyLong(), any());
			verify(popularizeCashWithdrawalStatusWritePort, never()).updateStatusById(anyLong(), any());
		}
	}

	@Nested
	@DisplayName("4.1.1 无配置")
	class BadConfig {

		@Test
		void propagates() {
			CronMerchantPaymentTradeRow r = row("m", 7L, REL_COMMUNITY, "1", "p");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(r));
			when(wechatPayMerchantContextPort.requireForV3Api(7L))
					.thenThrow(new ResourceException("bad"));
			assertThrows(ResourceException.class, () -> wechatPayService.scheduleQueryMerchantPayment());
		}
	}

	@Nested
	@DisplayName("1 与 2 列表查询+循环进入")
	class Wiring {

		@Test
		void list_called_and_loop_iterates() {
			CronMerchantPaymentTradeRow a = row("a", 1L, "x", "1", "p1");
			CronMerchantPaymentTradeRow b = row("b", 1L, "x", "2", "p2");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(a, b));
			when(wechatPayMerchantContextPort.requireForV3Api(1L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(1L, MAT, "p1")).thenReturn(noBatch());
			when(wechatBatchTransferQueryPort.queryBalanceOrder(1L, MAT, "p2")).thenReturn(noBatch());
			wechatPayService.scheduleQueryMerchantPayment();
			verify(merchantPaymentTradeListPort, times(1)).listWechatProcess(1, 100);
			verify(wechatPayMerchantContextPort, times(2)).requireForV3Api(1L);
		}
	}

	@Nested
	@DisplayName("4.4 与 5 多行仅部分落库")
	class MultipleRows {

		@Test
		void two_terminal_in_batch() {
			CronMerchantPaymentTradeRow a = row("a1", 8L, "other", "1", "o1");
			CronMerchantPaymentTradeRow b = row("a2", 8L, "other", "1", "o2");
			when(merchantPaymentTradeListPort.listWechatProcess(1, 100)).thenReturn(List.of(a, b));
			when(wechatPayMerchantContextPort.requireForV3Api(8L)).thenReturn(MAT);
			when(wechatBatchTransferQueryPort.queryBalanceOrder(8L, MAT, "o1"))
					.thenReturn(responseWithBatch("PROCESSING", 0));
			when(wechatBatchTransferQueryPort.queryBalanceOrder(8L, MAT, "o2"))
					.thenReturn(responseWithBatch("FINISHED", 1));
			assertThat(wechatPayService.scheduleQueryMerchantPayment()).isEqualTo(1);
		}
	}
}
