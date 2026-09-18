package cn.shopex.ecshopx.hfpay.service.enterapply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.hfpay.domain.HfpayBankCard;
import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.domain.HfpayWithdrawSet;
import cn.shopex.ecshopx.common.dispatch.HfpayDistributorWithdrawEventDispatchPublisher;
import cn.shopex.ecshopx.hfpay.mapper.HfpayBankCardMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayWithdrawSetMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HfpayEnterapplyServiceTest {

	@Mock
	private HfpayEnterapplyMapper enterapplyMapper;

	@Mock
	private HfpayBankCardMapper bankCardMapper;

	@Mock
	private HfpayWithdrawSetMapper withdrawSetMapper;

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

	@InjectMocks
	private HfpayEnterapplyService service;

	@Test
	@DisplayName("analysis §3 步骤2: count=0 不取页")
	void when_countZero_noPageQuery() {
		when(enterapplyMapper.selectCount(any())).thenReturn(0L);
		service.distributorWithdraw();
		verify(enterapplyMapper, never()).selectPage(any(), any());
		verify(cashRecordMapper, never()).insert(any(HfpayCashRecord.class));
		verify(distributorWithdrawEventDispatchPublisher, never())
				.publishDistributorWithdrawAfterPersist(anyLong(), anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("analysis §3 步骤3: count=600 时 round(600/50)=12 次分页 每页 50")
	@SuppressWarnings("unchecked")
	void when_count600_paginateTwelveTimes() {
		when(enterapplyMapper.selectCount(any())).thenReturn(600L);
		when(enterapplyMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						inv -> {
							Page<HfpayEnterapply> p = inv.getArgument(0);
							Page<HfpayEnterapply> out = new Page<>(p.getCurrent(), p.getSize());
							out.setRecords(Collections.emptyList());
							return out;
						});
		service.distributorWithdraw();
		ArgumentCaptor<Page<HfpayEnterapply>> cap = ArgumentCaptor.forClass(Page.class);
		verify(enterapplyMapper, times(12)).selectPage(cap.capture(), any());
		assertThat(cap.getAllValues()).allMatch(p -> p.getSize() == 50L);
	}

	@Test
	@DisplayName("analysis §3 附注: count=501 时仅 10 页 500 行（分页页数按向上取整）")
	void when_count501_tenPagesOnly() {
		when(enterapplyMapper.selectCount(any())).thenReturn(501L);
		when(enterapplyMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						inv -> {
							Page<HfpayEnterapply> p = inv.getArgument(0);
							Page<HfpayEnterapply> out = new Page<>(p.getCurrent(), p.getSize());
							out.setRecords(Collections.emptyList());
							return out;
						});
		service.distributorWithdraw();
		verify(enterapplyMapper, times(10)).selectPage(any(Page.class), any());
	}

	@Test
	@DisplayName("5.1 addCash 空 list: analysis §3 步骤4 / plan §5, count≤500 时一次 lists max 500")
	void when_count100_singleSelectPage() {
		when(enterapplyMapper.selectCount(any())).thenReturn(100L);
		Page<HfpayEnterapply> page = new Page<>(1, 500);
		page.setRecords(Collections.emptyList());
		when(enterapplyMapper.selectPage(any(Page.class), any())).thenReturn(page);
		service.distributorWithdraw();
		verify(enterapplyMapper, times(1)).selectPage(any(Page.class), any());
	}

	@Nested
	@DisplayName("addCash 分支 5.2.x")
	class AddCashBranches {

		private HfpayEnterapply oneEnter() {
			HfpayEnterapply e = new HfpayEnterapply();
			e.setCompanyId(1L);
			e.setDistributorId(2L);
			e.setUserCustId("uc");
			e.setAcctId("ac");
			return e;
		}

		private void stubBaseListOne(HfpayEnterapply e) {
			when(enterapplyMapper.selectCount(any())).thenReturn(1L);
			Page<HfpayEnterapply> page = new Page<>(1, 500);
			page.setRecords(e == null ? Collections.emptyList() : List.of(e));
			when(enterapplyMapper.selectPage(any(Page.class), any())).thenReturn(page);
		}

		@Test
		@DisplayName("5.2.1 无银行卡: 无 insert / 无事件")
		void noBankCard_noInsert() {
			HfpayEnterapply e = oneEnter();
			stubBaseListOne(e);
			when(bankCardMapper.selectOne(any())).thenReturn(null);
			service.distributorWithdraw();
			verify(cashRecordMapper, never()).insert(any(HfpayCashRecord.class));
			verify(distributorWithdrawEventDispatchPublisher, never())
				.publishDistributorWithdrawAfterPersist(anyLong(), anyLong(), anyLong(), anyLong());
		}

		@Test
		@DisplayName("5.2.2 qry 非 C00000: 无 insert")
		void qryNotSuccess_noInsert() {
			HfpayEnterapply e = oneEnter();
			stubBaseListOne(e);
			HfpayBankCard c = new HfpayBankCard();
			c.setBindCardId("b1");
			when(bankCardMapper.selectOne(any())).thenReturn(c);
			Map<String, Object> st = new LinkedHashMap<>();
			st.put("mer_cust_id", "m");
			when(paymentSettingService.loadForCompany(1L)).thenReturn(st);
			Map<String, Object> qry = new LinkedHashMap<>();
			qry.put("resp_code", "E00001");
			when(acouJsonPostClient.qry001(any(), any())).thenReturn(qry);
			service.distributorWithdraw();
			verify(cashRecordMapper, never()).insert(any(HfpayCashRecord.class));
		}

		@Test
		@DisplayName("5.2.4 withdraw_method=2: 无 insert")
		void manualWithdrawMode_skip() {
			HfpayEnterapply e = oneEnter();
			stubBaseListOne(e);
			HfpayBankCard c = new HfpayBankCard();
			c.setBindCardId("b1");
			when(bankCardMapper.selectOne(any())).thenReturn(c);
			Map<String, Object> st = new LinkedHashMap<>();
			st.put("mer_cust_id", "m");
			when(paymentSettingService.loadForCompany(1L)).thenReturn(st);
			Map<String, Object> qry = new LinkedHashMap<>();
			qry.put("resp_code", "C00000");
			qry.put("balance", "100.00");
			when(acouJsonPostClient.qry001(any(), any())).thenReturn(qry);
			HfpayWithdrawSet w = new HfpayWithdrawSet();
			w.setWithdrawMethod(2);
			when(withdrawSetMapper.selectOne(any())).thenReturn(w);
			service.distributorWithdraw();
			verify(cashRecordMapper, never()).insert(any(HfpayCashRecord.class));
		}

		@Test
		@DisplayName("5.2.5-5.2.6 余额扣留存后 <0: 无 insert")
		void lowBalanceAfterRetain_noInsert() {
			HfpayEnterapply e = oneEnter();
			stubBaseListOne(e);
			HfpayBankCard c = new HfpayBankCard();
			c.setBindCardId("b1");
			when(bankCardMapper.selectOne(any())).thenReturn(c);
			Map<String, Object> st = new LinkedHashMap<>();
			st.put("mer_cust_id", "m");
			when(paymentSettingService.loadForCompany(1L)).thenReturn(st);
			Map<String, Object> qry = new LinkedHashMap<>();
			qry.put("resp_code", "C00000");
			qry.put("balance", "1.00");
			when(acouJsonPostClient.qry001(any(), any())).thenReturn(qry);
			HfpayWithdrawSet w = new HfpayWithdrawSet();
			w.setWithdrawMethod(1);
			w.setDistributorMoney("2.00");
			when(withdrawSetMapper.selectOne(any())).thenReturn(w);
			service.distributorWithdraw();
			verify(cashRecordMapper, never()).insert(any(HfpayCashRecord.class));
		}

		@Test
		@DisplayName("5.2.7-5.2.9 成功: insert 且经 publisher 投递分销商取现")
		void success_insertsAndPublishes() {
			HfpayEnterapply e = oneEnter();
			stubBaseListOne(e);
			HfpayBankCard c = new HfpayBankCard();
			c.setBindCardId("b1");
			when(bankCardMapper.selectOne(any())).thenReturn(c);
			Map<String, Object> st = new LinkedHashMap<>();
			st.put("mer_cust_id", "m");
			when(paymentSettingService.loadForCompany(1L)).thenReturn(st);
			Map<String, Object> qry = new LinkedHashMap<>();
			qry.put("resp_code", "C00000");
			qry.put("balance", "10.00");
			when(acouJsonPostClient.qry001(any(), any())).thenReturn(qry);
			when(withdrawSetMapper.selectOne(any())).thenReturn(null);
			when(orderApplyIdGenerator.nextOrderId()).thenReturn("20240101000000001");
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
			when(cashRecordMapper.selectById(90L)).thenReturn(reloaded);
			service.distributorWithdraw();
			verify(cashRecordMapper, times(1)).insert(any(HfpayCashRecord.class));
			verify(distributorWithdrawEventDispatchPublisher, times(1))
					.publishDistributorWithdrawAfterPersist(90L, 1L, 2L, 1000L);
		}
	}
}
