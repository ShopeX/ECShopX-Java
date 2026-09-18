package cn.shopex.ecshopx.hfpay.service.cashrecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.HfpayDistributorWithdrawEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayBankCard;
import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.domain.HfpayWithdrawSet;
import cn.shopex.ecshopx.hfpay.mapper.HfpayBankCardMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayWithdrawSetMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HfpayCashRecordWithdrawServiceDistributorWithdrawPublishProbeTest {

	@Mock
	private HfpayEnterapplyMapper enterapplyMapper;

	@Mock
	private HfpayWithdrawSetMapper withdrawSetMapper;

	@Mock
	private HfpayBankCardMapper bankCardMapper;

	@Mock
	private HfpayCashRecordMapper cashRecordMapper;

	@Mock
	private HfPayPaymentSettingService paymentSettingService;

	@Mock
	private HfPayAcouJsonPostClient acouJsonPostClient;

	@Mock
	private HfPayOrderApplyIdGenerator orderApplyIdGenerator;

	@Mock
	private HfpayDistributorWithdrawEventDispatchPublisher distributorWithdrawEventDispatchPublisher;

	private HfpayCashRecordWithdrawService service;

	@BeforeEach
	void setUp() {
		service = new HfpayCashRecordWithdrawService(
				enterapplyMapper,
				withdrawSetMapper,
				bankCardMapper,
				cashRecordMapper,
				paymentSettingService,
				acouJsonPostClient,
				orderApplyIdGenerator,
				distributorWithdrawEventDispatchPublisher,
				"");
		ZoneAssumption.assumeWithdrawTimeOpenOrSkip();
	}

	@Test
	@DisplayName("persist 后 publishDistributorWithdrawAfterPersist 四参数与 insert 主键及请求一致（ArgumentCaptor）")
	void withdraw_afterPersist_invokesPublishDistributorWithdrawWithCaptor() {
		stubHappyPathDeps();
		when(orderApplyIdGenerator.nextOrderId()).thenReturn("OID-1");
		doAnswer(
						inv -> {
							HfpayCashRecord row = inv.getArgument(0);
							row.setHfpayCashRecordId(90L);
							return 1;
						})
				.when(cashRecordMapper)
				.insert(any(HfpayCashRecord.class));
		HfpayCashRecord reloaded = new HfpayCashRecord();
		reloaded.setHfpayCashRecordId(90L);
		reloaded.setCompanyId(1L);
		reloaded.setDistributorId(2L);
		reloaded.setTransAmt(1000);
		when(cashRecordMapper.selectById(90L)).thenReturn(reloaded);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("distributor_id", 2L);
		merged.put("withdrawal_amount", "10.00");

		service.withdraw(1L, 99L, merged);

		ArgumentCaptor<Long> cashRecordIdCap = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> companyIdCap = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> distributorIdCap = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> transAmtFenCap = ArgumentCaptor.forClass(Long.class);
		verify(distributorWithdrawEventDispatchPublisher, times(1))
				.publishDistributorWithdrawAfterPersist(
						cashRecordIdCap.capture(),
						companyIdCap.capture(),
						distributorIdCap.capture(),
						transAmtFenCap.capture());
		assertThat(cashRecordIdCap.getValue()).isEqualTo(90L);
		assertThat(companyIdCap.getValue()).isEqualTo(1L);
		assertThat(distributorIdCap.getValue()).isEqualTo(2L);
		assertThat(transAmtFenCap.getValue()).isEqualTo(1000L);
	}

	@Test
	@DisplayName("insert 后 reload 为空时不调用 publish，防误投递")
	void withdraw_whenInsertReloadNull_neverPublishes() {
		stubHappyPathDeps();
		when(orderApplyIdGenerator.nextOrderId()).thenReturn("OID-2");
		doAnswer(
						inv -> {
							HfpayCashRecord row = inv.getArgument(0);
							row.setHfpayCashRecordId(91L);
							return 1;
						})
				.when(cashRecordMapper)
				.insert(any(HfpayCashRecord.class));
		when(cashRecordMapper.selectById(91L)).thenReturn(null);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("distributor_id", 2L);
		merged.put("withdrawal_amount", "10.00");

		assertThatThrownBy(() -> service.withdraw(1L, 99L, merged)).isInstanceOf(ResourceException.class);

		verify(distributorWithdrawEventDispatchPublisher, never())
				.publishDistributorWithdrawAfterPersist(anyLong(), anyLong(), anyLong(), anyLong());
	}

	private void stubHappyPathDeps() {
		HfpayEnterapply enter = new HfpayEnterapply();
		enter.setUserCustId("uc1");
		enter.setAcctId("acct1");
		when(enterapplyMapper.selectOne(any())).thenReturn(enter);

		HfpayWithdrawSet withdrawSet = new HfpayWithdrawSet();
		withdrawSet.setWithdrawMethod(2);
		withdrawSet.setDistributorMoney("0");
		when(withdrawSetMapper.selectOne(any())).thenReturn(withdrawSet);

		HfpayBankCard bank = new HfpayBankCard();
		bank.setBindCardId("bind-1");
		when(bankCardMapper.selectOne(any())).thenReturn(bank);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "mer1");
		when(paymentSettingService.loadForCompany(1L)).thenReturn(setting);

		Map<String, Object> qry = new LinkedHashMap<>();
		qry.put("resp_code", "C00000");
		qry.put("balance", "100.00");
		when(acouJsonPostClient.qry001(any(), any())).thenReturn(qry);
	}

	/** Withdraw is only allowed from 10:00 in the configured zone ({@code ""} → system default). */
	private static final class ZoneAssumption {
		private ZoneAssumption() {}

		static void assumeWithdrawTimeOpenOrSkip() {
			ZoneId z = ZoneId.systemDefault();
			ZonedDateTime now = ZonedDateTime.now(z);
			ZonedDateTime open = now.toLocalDate().atStartOfDay(z).plusHours(10);
			Assumptions.assumeFalse(now.isBefore(open), "withdraw allowed only from 10:00 in default business zone");
		}
	}
}
