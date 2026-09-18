package cn.shopex.ecshopx.popularize.service.brokerage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.service.brokerage.NormalOrderBrokerageFinishInput;
import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobDispatchPublisher;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class TradeFinishCountBrokerageUpgradePromoterGradeEnqueueTest {

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

	@InjectMocks
	private NormalOrderFinishBrokerageCoordinator coordinator;

	@BeforeEach
	void stubTransaction() {
		when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
	}

	@Test
	void whenFirstLevelBrokerageInserted_thenDispatchUpgradePromoterGradeJobOncePerRow() {
		when(popularizeSettingSaveService.getOpenPopularizeLiteral(eq(1L))).thenReturn("true");
		when(brokerageMapper.selectCount(any())).thenReturn(0L);
		when(popularizeSettingSaveService.getMergedPopularizeConfig(1L)).thenReturn(samplePopularizeRatio());

		Promoter buyer = new Promoter();
		buyer.setUserId(300L);
		buyer.setPid(400L);
		Promoter parent = new Promoter();
		parent.setUserId(400L);
		parent.setIsPromoter(1);

		when(promoterMapper.selectOne(any())).thenReturn(buyer, parent);

		NormalOrderBrokerageFinishInput order =
				new NormalOrderBrokerageFinishInput(300L, "normal", "1000", 100);

		coordinator.onNormalOrderFinishBrokerage(1L, 10L, order);

		Promoter buyer2 = new Promoter();
		buyer2.setUserId(301L);
		buyer2.setPid(500L);
		Promoter parent2 = new Promoter();
		parent2.setUserId(500L);
		parent2.setIsPromoter(1);
		when(promoterMapper.selectOne(any())).thenReturn(buyer2, parent2);
		NormalOrderBrokerageFinishInput order2 =
				new NormalOrderBrokerageFinishInput(301L, "normal", "2000", 200);

		coordinator.onNormalOrderFinishBrokerage(1L, 11L, order2);

		verify(upgradePromoterGradeJobDispatchPublisher, times(1)).enqueueUpgradePromoterGrade(1L, 400L);
		verify(upgradePromoterGradeJobDispatchPublisher, times(1)).enqueueUpgradePromoterGrade(1L, 500L);
		verify(brokerageMapper, times(2)).insert(any(Brokerage.class));
	}

	private static Map<String, Object> samplePopularizeRatio() {
		Map<String, Object> fl = new LinkedHashMap<>();
		fl.put("ratio", 10.0d);
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
