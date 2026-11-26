package cn.shopex.ecshopx.distribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.RebateCashWithdrawalMerchantPaymentPort;
import cn.shopex.ecshopx.distribution.domain.CashWithdrawal;
import cn.shopex.ecshopx.distribution.mapper.CashWithdrawalMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class CashWithdrawalProcessServiceClaimTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, CashWithdrawal.class);
	}

	@Mock
	private CashWithdrawalMapper cashWithdrawalMapper;
	@Mock
	private RebateCashWithdrawalMerchantPaymentPort rebateCashWithdrawalMerchantPaymentPort;
	@Mock
	private DistributeCountWriteService distributeCountWriteService;
	@Mock
	private PlatformTransactionManager transactionManager;

	private CashWithdrawalProcessService service;

	@BeforeEach
	void setUp() {
		service = new CashWithdrawalProcessService(
				cashWithdrawalMapper,
				rebateCashWithdrawalMerchantPaymentPort,
				distributeCountWriteService,
				transactionManager);
	}

	@Test
	@DisplayName("抢占 apply→process 影响行数为 0 时不打款")
	void agree_whenClaimAffectedZero_doesNotPay() {
		when(cashWithdrawalMapper.selectOne(any())).thenReturn(applyRow(9L));
		when(cashWithdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.process(Map.of("company_id", 1L), 9L, "argee", null, "127.0.0.1"))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("正在处理或已完成");

		verify(rebateCashWithdrawalMerchantPaymentPort, never())
				.merchantPayment(eq(1L), anyString(), anyMap());
	}

	@Test
	@DisplayName("抢占更新 WHERE 必须带 status=apply")
	void agree_claimUpdateRequiresStatusApply() {
		when(cashWithdrawalMapper.selectOne(any())).thenReturn(applyRow(9L));
		when(cashWithdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.process(Map.of("company_id", 1L), 9L, "argee", null, "127.0.0.1"))
				.isInstanceOf(ResourceException.class);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<CashWithdrawal>> captor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(cashWithdrawalMapper).update(isNull(), captor.capture());
		LambdaUpdateWrapper<CashWithdrawal> claim = captor.getValue();
		assertThat(claim.getSqlSegment()).containsIgnoringCase("status");
		assertThat(claim.getExpression().getNormal()).hasSizeGreaterThanOrEqualTo(3);
		assertThat(claim.getSqlSet()).containsIgnoringCase("status");
		assertThat(claim.getParamNameValuePairs().values()).contains("process");
	}

	private static CashWithdrawal applyRow(long id) {
		CashWithdrawal row = new CashWithdrawal();
		row.setId(id);
		row.setCompanyId(1L);
		row.setStatus("apply");
		row.setMoney(100);
		row.setDistributorId("2");
		row.setDistributorName("张三");
		row.setDistributorMobile("13800000000");
		row.setUserId("u1");
		row.setOpenId("oid");
		row.setWxaAppid("wxapp");
		return row;
	}
}
