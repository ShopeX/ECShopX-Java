package cn.shopex.ecshopx.aftersales.service.saaserp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AftersalesSaasErpAftersaleStatusAdapterTest {

	private static final long COMPANY_ID = 10L;
	private static final long ORDER_ID = 2002L;
	private static final long AFTERSALES_BN = 1001L;
	private static final long DISTRIBUTOR_ID = 7L;
	private static final long ADDRESS_ID = 88L;

	@Mock
	private AftersalesMapper aftersalesMapper;

	@Mock
	private AftersalesDetailMapper aftersalesDetailMapper;

	@Mock
	private AftersalesReviewService reviewService;

	@Mock
	private DistributorDefaultAftersalesAddressReadPort addressPort;

	@Mock
	private PlatformTransactionManager transactionManager;

	private AftersalesSaasErpAftersaleStatusAdapter adapter;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AftersalesDetail.class);
	}

	@BeforeEach
	void setUp() {
		adapter =
				new AftersalesSaasErpAftersaleStatusAdapter(
						aftersalesMapper,
						aftersalesDetailMapper,
						reviewService,
						addressPort,
						transactionManager);
	}

	@Test
	void missingRowThrowsExactMessageAndDoesNotReview() {
		when(aftersalesMapper.selectOne(any())).thenReturn(null);

		IllegalStateException ex =
				assertThrows(
						IllegalStateException.class,
						() -> adapter.update(params("1001", "2002", "4", null)));

		assertEquals("未获取到售后单信息", ex.getMessage());
		ArgumentCaptor<LambdaQueryWrapper<Aftersales>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(aftersalesMapper).selectOne(query.capture());
		String sql = query.getValue().getCustomSqlSegment();
		assertTrue(sql.contains("aftersales_bn"));
		assertTrue(sql.contains("order_id"));
		assertTrue(!sql.contains("company_id"));
		verifyNoInteractions(reviewService);
	}

	@Test
	void status4Progress0RefundGoodsCallsReviewApprovedWithLogisticsAddress() {
		when(aftersalesMapper.selectOne(any())).thenReturn(row("REFUND_GOODS", 0));
		when(addressPort.findDefaultLogisticsReturnAddressId(COMPANY_ID, DISTRIBUTOR_ID))
				.thenReturn(Optional.of(ADDRESS_ID));
		Map<String, Object> reviewed = Map.of("aftersales_bn", AFTERSALES_BN);
		when(reviewService.review(any(), isNull())).thenReturn(reviewed);

		List<Object> result = adapter.update(params("1001", "2002", "4", "ignored"));

		assertEquals(List.of(reviewed), result);
		Map<String, Object> reviewParams = captureReviewParams();
		assertEquals(1, ((Number) reviewParams.get("is_approved")).intValue());
		assertEquals(AFTERSALES_BN, ((Number) reviewParams.get("aftersales_bn")).longValue());
		assertEquals(COMPANY_ID, ((Number) reviewParams.get("company_id")).longValue());
		assertEquals(ADDRESS_ID, ((Number) reviewParams.get("aftersales_address_id")).longValue());
	}

	@Test
	void status4Progress0RefundGoodsWithoutAddressUsesZero() {
		when(aftersalesMapper.selectOne(any())).thenReturn(row("REFUND_GOODS", 0));
		when(addressPort.findDefaultLogisticsReturnAddressId(COMPANY_ID, DISTRIBUTOR_ID))
				.thenReturn(Optional.empty());
		when(reviewService.review(any(), isNull())).thenReturn(Map.of("ok", true));

		adapter.update(params("1001", "2002", "4", null));

		Map<String, Object> reviewParams = captureReviewParams();
		assertEquals(0, ((Number) reviewParams.get("aftersales_address_id")).intValue());
		assertEquals(1, ((Number) reviewParams.get("is_approved")).intValue());
	}

	@Test
	void status4Progress0OnlyRefundSetsAddressIdZeroWithoutLookup() {
		when(aftersalesMapper.selectOne(any())).thenReturn(row("ONLY_REFUND", 0));
		when(reviewService.review(any(), isNull())).thenReturn(Map.of("ok", true));

		adapter.update(params("1001", "2002", "4", null));

		Map<String, Object> reviewParams = captureReviewParams();
		assertEquals(1, ((Number) reviewParams.get("is_approved")).intValue());
		assertEquals(0, ((Number) reviewParams.get("aftersales_address_id")).intValue());
		verify(addressPort, never()).findDefaultLogisticsReturnAddressId(anyLong(), anyLong());
	}

	@Test
	void status4Progress1DoesNotCallReview() {
		when(aftersalesMapper.selectOne(any())).thenReturn(row("REFUND_GOODS", 1));

		List<Object> result = adapter.update(params("1001", "2002", "4", null));

		assertTrue(result.isEmpty());
		verifyNoInteractions(reviewService);
		verify(addressPort, never()).findDefaultLogisticsReturnAddressId(anyLong(), anyLong());
	}

	@Test
	void status2AddonNullStringUsesEmptyRefuseReason() {
		when(aftersalesMapper.selectOne(any())).thenReturn(row("REFUND_GOODS", 0));
		when(reviewService.review(any(), isNull())).thenReturn(Map.of("ok", true));

		adapter.update(params("1001", "2002", "2", "null"));

		Map<String, Object> reviewParams = captureReviewParams();
		assertEquals(0, ((Number) reviewParams.get("is_approved")).intValue());
		assertEquals("", reviewParams.get("refuse_reason"));
		verify(addressPort, never()).findDefaultLogisticsReturnAddressId(anyLong(), anyLong());
	}

	@Test
	void status1Progress2CallsSendBackConfirm() {
		when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
		when(aftersalesMapper.selectOne(any())).thenReturn(row("REFUND_GOODS", 2));
		when(aftersalesMapper.update(isNull(), any())).thenReturn(1);
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.update(isNull(), any())).thenReturn(1);

		List<Object> result = adapter.update(params("1001", "2002", "1", null));

		assertEquals(1, result.size());
		assertEquals(8, ((Number) ((Map<?, ?>) result.get(0)).get("progress")).intValue());
		assertEquals(AFTERSALES_BN, ((Number) ((Map<?, ?>) result.get(0)).get("aftersales_bn")).longValue());
		verifyNoInteractions(reviewService);
		assertProgressSetToConfirmed(captureMainUpdate());
		assertProgressSetToConfirmed(captureDetailUpdate());
	}

	@Test
	void status1OtherProgressDoesNotSendBack() {
		when(aftersalesMapper.selectOne(any())).thenReturn(row("REFUND_GOODS", 1));

		List<Object> result = adapter.update(params("1001", "2002", "1", null));

		assertTrue(result.isEmpty());
		verify(aftersalesMapper, never()).update(any(), any());
		verifyNoInteractions(reviewService);
	}

	@Test
	void reviewExceptionReturnsEmptyList() {
		when(aftersalesMapper.selectOne(any())).thenReturn(row("REFUND_GOODS", 0));
		when(addressPort.findDefaultLogisticsReturnAddressId(COMPANY_ID, DISTRIBUTOR_ID))
				.thenReturn(Optional.of(ADDRESS_ID));
		when(reviewService.review(any(), isNull())).thenThrow(new IllegalStateException("审核失败"));

		List<Object> result = adapter.update(params("1001", "2002", "4", null));

		assertTrue(result.isEmpty());
	}

	@SuppressWarnings("unchecked")
	private LambdaUpdateWrapper<Aftersales> captureMainUpdate() {
		ArgumentCaptor<LambdaUpdateWrapper<Aftersales>> mainUpdate =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(aftersalesMapper).update(isNull(), mainUpdate.capture());
		return mainUpdate.getValue();
	}

	@SuppressWarnings("unchecked")
	private LambdaUpdateWrapper<AftersalesDetail> captureDetailUpdate() {
		ArgumentCaptor<LambdaUpdateWrapper<AftersalesDetail>> detailUpdate =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(aftersalesDetailMapper).update(isNull(), detailUpdate.capture());
		return detailUpdate.getValue();
	}

	private static void assertProgressSetToConfirmed(LambdaUpdateWrapper<?> update) {
		assertTrue(update.getSqlSet().contains("progress"));
		assertTrue(update.getParamNameValuePairs().containsValue(8));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> captureReviewParams() {
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(reviewService).review(captor.capture(), isNull());
		return captor.getValue();
	}

	private static Map<String, String> params(String aftersaleId, String orderId, String status, String addon) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("aftersale_id", aftersaleId);
		params.put("order_id", orderId);
		params.put("status", status);
		if (addon != null) {
			params.put("addon", addon);
		}
		return params;
	}

	private static Aftersales row(String type, int progress) {
		Aftersales row = new Aftersales();
		row.setAftersalesBn(AFTERSALES_BN);
		row.setOrderId(ORDER_ID);
		row.setCompanyId(COMPANY_ID);
		row.setDistributorId(DISTRIBUTOR_ID);
		row.setAftersalesType(type);
		row.setProgress(progress);
		row.setAftersalesStatus(0);
		return row;
	}
}
