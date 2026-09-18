package cn.shopex.ecshopx.orders.service.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OmeTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.SubOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.SubOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class GroupPromotionOrderPayedServiceTest {

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
	@DisplayName("consume_type=all: marks paid and inserts orders_rights")
	void markServiceGroupOrderPayed_andGrantRights_allBranch_insertsOrdersRights() {
		ServiceOrders order = new ServiceOrders();
		order.setOrderId(100L);
		order.setCompanyId(9L);
		order.setConsumeType("all");
		order.setDateType("DATE_TYPE_FIX_TIME_RANGE");
		order.setBeginDate(1000);
		order.setEndDate(2000);
		order.setItemNum("3");
		order.setTitle("次卡单");
		order.setOrderSource("web");

		SubOrders sub = new SubOrders();
		sub.setLabelId(10L);
		sub.setLabelName("次数");

		when(serviceOrdersMapper.selectOne(any())).thenReturn(order);
		when(subOrdersMapper.selectList(any())).thenReturn(List.of(sub));

		service.markServiceGroupOrderPayedAndTryGrantRights(9L, 5L, 100L);

		verify(serviceOrdersMapper, times(1)).update(any(), any());
		verify(orderAssociationsMapper, times(1)).update(any(), any());
		ArgumentCaptor<Rights> cap = ArgumentCaptor.forClass(Rights.class);
		verify(rightsMapper, times(1)).insert(cap.capture());
		verify(tradeMapper, never()).selectOne(any());
	}

	@Test
	@DisplayName("non-eligible consume_type: no rights insertion")
	void markServiceGroupOrderPayed_nonRightsConsumeType_noRightsInsert() {
		ServiceOrders order = new ServiceOrders();
		order.setOrderId(200L);
		order.setCompanyId(1L);
		order.setConsumeType("none");

		when(serviceOrdersMapper.selectOne(any())).thenReturn(order);

		service.markServiceGroupOrderPayedAndTryGrantRights(1L, 2L, 200L);

		verify(serviceOrdersMapper, times(1)).update(any(), any());
		verify(orderAssociationsMapper, times(1)).update(any(), any());
		verify(subOrdersMapper, never()).selectList(any());
		verify(rightsMapper, never()).insert(any(Rights.class));
	}

	@Test
	void markNormalGroupOrderPayedAndPublishErpSync_invokesOmeTradeUpdateDispatchPublisherWithFourKeyEntitiesMap() {
		when(tradeMapper.selectOne(any())).thenReturn(null);

		service.markNormalGroupOrderPayedAndPublishErpSync(1L, 5L, 200L);

		verify(normalOrdersMapper, times(1)).update(any(), any());
		verify(orderAssociationsMapper, times(1)).update(any(), any());
		verify(tradeMapper, times(1)).selectOne(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(omeTradeUpdateDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertThat(payload)
				.hasSize(4)
				.containsEntry("company_id", 1L)
				.containsEntry("order_id", "200")
				.containsEntry("order_class", "normal_groups")
				.containsEntry("user_id", 5L);
	}
}
