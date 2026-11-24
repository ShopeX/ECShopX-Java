package cn.shopex.ecshopx.popularize.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.RebateCashWithdrawalMerchantPaymentPort;
import cn.shopex.ecshopx.popularize.mapper.PromoterCashWithdrawalMapper;
import cn.shopex.ecshopx.popularize.port.PopularizeCashWithdrawalBankcardPayPort;
import cn.shopex.ecshopx.popularize.port.PopularizeHfpayRebateMerchantWithdrawalPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class PopularizeCashWithdrawalProcessServiceProcessTypeTest {

	@Mock
	private PromoterCashWithdrawalMapper promoterCashWithdrawalMapper;

	@Mock
	private RebateCashWithdrawalMerchantPaymentPort rebateCashWithdrawalMerchantPaymentPort;

	@Mock
	private PopularizeHfpayRebateMerchantWithdrawalPort popularizeHfpayRebateMerchantWithdrawalPort;

	@Mock
	private PopularizeCashWithdrawalBankcardPayPort popularizeCashWithdrawalBankcardPayPort;

	@Mock
	private PopularizePromoterCountWriteService popularizePromoterCountWriteService;

	@Mock
	private PlatformTransactionManager transactionManager;

	private PopularizeCashWithdrawalProcessService service;

	@BeforeEach
	void setUp() {
		service =
				new PopularizeCashWithdrawalProcessService(
						promoterCashWithdrawalMapper,
						rebateCashWithdrawalMerchantPaymentPort,
						popularizeHfpayRebateMerchantWithdrawalPort,
						popularizeCashWithdrawalBankcardPayPort,
						popularizePromoterCountWriteService,
						transactionManager);
	}

	@Test
	void process_throws参数错误_whenProcessTypeNullOrUnknown() {
		assertThatThrownBy(() -> service.process(38L, "11", null, null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("参数错误");
		assertThatThrownBy(() -> service.process(38L, "11", "agree", null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("参数错误");
	}
}
