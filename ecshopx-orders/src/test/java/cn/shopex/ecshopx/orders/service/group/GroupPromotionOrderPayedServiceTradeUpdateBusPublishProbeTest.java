package cn.shopex.ecshopx.orders.service.group;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OmeTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.SubOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.SubOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupPromotionOrderPayedServiceTradeUpdateBusPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), ServiceOrders.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), SubOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Rights.class);
	}

	@Mock
	private ServiceOrdersMapper serviceOrdersMapper;
	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;
	@Mock
	private SubOrdersMapper subOrdersMapper;
	@Mock
	private RightsMapper rightsMapper;
	@Mock
	private TradeMapper tradeMapper;
	@Mock
	private OmeTradeUpdateDispatchPublisher omeTradeUpdateDispatchPublisher;
	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	private GroupPromotionOrderPayedService service;

	@BeforeEach
	void setUp() {
		service =
				new GroupPromotionOrderPayedService(
						serviceOrdersMapper,
						normalOrdersMapper,
						orderAssociationsMapper,
						subOrdersMapper,
						rightsMapper,
						tradeMapper,
						new ObjectMapper(),
						omeTradeUpdateDispatchPublisher,
						thirdPartyTradeUpdateDispatchPublisher);
	}

	@Test
	void markNormalGroupOrderPayed_invokesThirdPartyTradeUpdatePublishWithExpectedEntitiesMap() {
		when(tradeMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

		service.markNormalGroupOrderPayedAndPublishErpSync(1L, 5L, 200L);

		verify(thirdPartyTradeUpdateDispatchPublisher, times(1))
				.publish(
						argThat(
								map ->
										map != null
												&& Long.valueOf(1L).equals(map.get("company_id"))
												&& "200".equals(map.get("order_id"))
												&& Long.valueOf(5L).equals(map.get("user_id"))
												&& "normal_groups".equals(map.get("order_class"))));
		verify(omeTradeUpdateDispatchPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& m.size() == 4
												&& Long.valueOf(1L).equals(m.get("company_id"))
												&& "200".equals(m.get("order_id"))
												&& "normal_groups".equals(m.get("order_class"))
												&& Long.valueOf(5L).equals(m.get("user_id"))));
	}
}
