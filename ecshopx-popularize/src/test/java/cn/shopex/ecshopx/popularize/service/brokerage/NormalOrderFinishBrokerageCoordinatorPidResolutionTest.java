package cn.shopex.ecshopx.popularize.service.brokerage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.service.brokerage.NormalOrderBrokerageFinishInput;
import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobDispatchPublisher;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.BrokeragePlanCloseTimeService;
import cn.shopex.ecshopx.popularize.service.PopularizeBrokerageStatisticsIncrementService;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class NormalOrderFinishBrokerageCoordinatorPidResolutionTest {

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	@Mock
	private BrokerageMapper brokerageMapper;

	@Mock
	private PromoterMapper promoterMapper;

	@Mock
	private PopularizeSettingSaveService popularizeSettingSaveService;

	@Mock
	private UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher;

	@Mock
	private PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;

	@Mock
	private BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;

	@InjectMocks
	private NormalOrderFinishBrokerageCoordinator coordinator;

	@BeforeEach
	void stubTransaction() {
		when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
	}

	@Test
	void whenBuyerPidIsParentPromoterId_thenBrokerageAndStatsUseParentMemberUserId() {
		when(popularizeSettingSaveService.getOpenPopularizeLiteral(eq(38L))).thenReturn("true");
		when(brokerageMapper.selectCount(any())).thenReturn(0L);
		when(popularizeSettingSaveService.getMergedPopularizeConfig(38L)).thenReturn(samplePopularizeRatio());

		Promoter buyer = new Promoter();
		buyer.setId(1141L);
		buyer.setCompanyId(38L);
		buyer.setUserId(3890L);
		buyer.setPid(250L);
		buyer.setIsBuy(0);

		Promoter parent = new Promoter();
		parent.setId(250L);
		parent.setCompanyId(38L);
		parent.setUserId(261L);
		parent.setIsPromoter(1);

		when(promoterMapper.selectOne(any())).thenReturn(buyer);
		when(promoterMapper.selectById(250L)).thenReturn(parent);

		NormalOrderBrokerageFinishInput order =
				new NormalOrderBrokerageFinishInput(3890L, "normal", "18100", 0);

		coordinator.onNormalOrderFinishBrokerage(38L, 5427890000203890L, order);

		ArgumentCaptor<cn.shopex.ecshopx.popularize.domain.Brokerage> brokerageCaptor =
				ArgumentCaptor.forClass(cn.shopex.ecshopx.popularize.domain.Brokerage.class);
		verify(brokerageMapper).insert(brokerageCaptor.capture());
		org.assertj.core.api.Assertions.assertThat(brokerageCaptor.getValue().getUserId()).isEqualTo(261L);
		org.assertj.core.api.Assertions.assertThat(brokerageCaptor.getValue().getBuyUserId()).isEqualTo(3890L);
		org.assertj.core.api.Assertions.assertThat(brokerageCaptor.getValue().getRebate()).isEqualTo(181);

		verify(popularizeBrokerageStatisticsIncrementService)
				.add(
						eq(
								Map.of(
										"item_total_price",
										18100L,
										"rebate_total",
										181L,
										"no_close_rebate",
										181L)),
						eq(261L),
						eq(38L));

		verify(promoterMapper).update(any(), any());
		verify(upgradePromoterGradeJobDispatchPublisher).enqueueUpgradePromoterGrade(38L, 261L);
	}

	private static Map<String, Object> samplePopularizeRatio() {
		Map<String, Object> fl = new LinkedHashMap<>();
		fl.put("ratio", 1.0d);
		Map<String, Object> om = new LinkedHashMap<>();
		om.put("first_level", fl);
		Map<String, Object> pr = new LinkedHashMap<>();
		pr.put("type", "order_money");
		pr.put("order_money", om);
		Map<String, Object> cfg = new LinkedHashMap<>();
		cfg.put("popularize_ratio", pr);
		return cfg;
	}
}
