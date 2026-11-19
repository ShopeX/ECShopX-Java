package cn.shopex.ecshopx.popularize.service.brokerage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.service.brokerage.NormalOrderBrokerageFinishInput;
import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobDispatchPublisher;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.BrokeragePlanCloseTimeService;
import cn.shopex.ecshopx.popularize.service.PopularizeBrokerageStatisticsIncrementService;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class NormalOrderFinishBrokerageCoordinatorRebateDetailTest {

	private static final ObjectMapper OM = new ObjectMapper();

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
	@DisplayName("有行项目时 detail.rebate_detail 按 item_id 写入件数与分佣")
	void whenLinesPresent_thenDetailContainsRebateDetailByItemId() throws Exception {
		when(popularizeSettingSaveService.getOpenPopularizeLiteral(eq(38L))).thenReturn("true");
		when(brokerageMapper.selectCount(any())).thenReturn(0L);
		when(popularizeSettingSaveService.getMergedPopularizeConfig(38L)).thenReturn(samplePopularizeRatio());
		when(brokeragePlanCloseTimeService.pendingPlanCloseTimeForInsert()).thenReturn(Integer.MAX_VALUE);

		Promoter buyer = new Promoter();
		buyer.setId(1L);
		buyer.setCompanyId(38L);
		buyer.setUserId(100L);
		buyer.setPid(2L);
		buyer.setIsBuy(1);

		Promoter parent = new Promoter();
		parent.setId(2L);
		parent.setCompanyId(38L);
		parent.setUserId(200L);
		parent.setIsPromoter(1);

		when(promoterMapper.selectOne(any())).thenReturn(buyer);
		when(promoterMapper.selectById(2L)).thenReturn(parent);

		NormalOrderBrokerageFinishInput order =
				new NormalOrderBrokerageFinishInput(
						100L,
						"normal",
						"10000",
						0,
						List.of(
								new NormalOrderBrokerageFinishInput.Line(11L, 2, 6000L, 0L),
								new NormalOrderBrokerageFinishInput.Line(22L, 1, 4000L, 0L)));

		coordinator.onNormalOrderFinishBrokerage(38L, 9001L, order);

		ArgumentCaptor<Brokerage> cap = ArgumentCaptor.forClass(Brokerage.class);
		verify(brokerageMapper).insert(cap.capture());
		Brokerage row = cap.getValue();
		assertThat(row.getRebate()).isEqualTo(100);
		JsonNode detail = OM.readTree(row.getDetail());
		JsonNode rebateDetail = detail.get("rebate_detail");
		assertThat(rebateDetail.get("11").get("item_num").asInt()).isEqualTo(2);
		assertThat(rebateDetail.get("11").get("rebate").asInt()).isEqualTo(60);
		assertThat(rebateDetail.get("11").get("total_fee").asLong()).isEqualTo(6000L);
		assertThat(rebateDetail.get("22").get("item_num").asInt()).isEqualTo(1);
		assertThat(rebateDetail.get("22").get("rebate").asInt()).isEqualTo(40);
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
