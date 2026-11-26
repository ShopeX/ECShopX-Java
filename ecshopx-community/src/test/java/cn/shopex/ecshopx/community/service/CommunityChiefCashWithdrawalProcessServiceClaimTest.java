package cn.shopex.ecshopx.community.service;

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
import cn.shopex.ecshopx.community.domain.CommunityChiefCashWithdrawal;
import cn.shopex.ecshopx.community.mapper.CommunityChiefCashWithdrawalMapper;
import cn.shopex.ecshopx.distribution.integration.RebateCashWithdrawalMerchantPaymentClient;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
class CommunityChiefCashWithdrawalProcessServiceClaimTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, CommunityChiefCashWithdrawal.class);
	}

	@Mock
	private CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper;
	@Mock
	private RebateCashWithdrawalMerchantPaymentClient rebateCashWithdrawalMerchantPaymentClient;
	@Mock
	private PlatformTransactionManager transactionManager;

	private CommunityChiefCashWithdrawalProcessService service;

	@BeforeEach
	void setUp() {
		service = new CommunityChiefCashWithdrawalProcessService(
				communityChiefCashWithdrawalMapper,
				rebateCashWithdrawalMerchantPaymentClient,
				transactionManager);
	}

	@Test
	@DisplayName("抢占 apply→process 影响行数为 0 时不打款")
	void agree_whenClaimAffectedZero_doesNotPay() {
		when(communityChiefCashWithdrawalMapper.selectOne(any())).thenReturn(applyRow(9L));
		when(communityChiefCashWithdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.process(1L, 9L, "argee", null, "127.0.0.1"))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("正在处理或已完成");

		verify(rebateCashWithdrawalMerchantPaymentClient, never())
				.merchantPayment(eq(1L), anyString(), anyMap());
	}

	@Test
	@DisplayName("抢占更新 WHERE 必须带 status=apply")
	void agree_claimUpdateRequiresStatusApply() {
		when(communityChiefCashWithdrawalMapper.selectOne(any())).thenReturn(applyRow(9L));
		when(communityChiefCashWithdrawalMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.process(1L, 9L, "argee", null, "127.0.0.1"))
				.isInstanceOf(ResourceException.class);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<CommunityChiefCashWithdrawal>> captor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(communityChiefCashWithdrawalMapper).update(isNull(), captor.capture());
		LambdaUpdateWrapper<CommunityChiefCashWithdrawal> claim = captor.getValue();
		assertThat(claim.getSqlSegment()).containsIgnoringCase("status");
		assertThat(claim.getExpression().getNormal()).hasSizeGreaterThanOrEqualTo(3);
		assertThat(claim.getSqlSet()).containsIgnoringCase("status");
		assertThat(claim.getParamNameValuePairs().values()).contains("process");
	}

	private static CommunityChiefCashWithdrawal applyRow(long id) {
		CommunityChiefCashWithdrawal row = new CommunityChiefCashWithdrawal();
		row.setId(id);
		row.setCompanyId(1L);
		row.setStatus("apply");
		row.setMoney(100);
		row.setPayType("wechat");
		row.setAccountName("李四");
		row.setMobile("13900000000");
		row.setChiefId(3L);
		row.setPayAccount("oid");
		row.setWxaAppid("wxapp");
		return row;
	}
}
