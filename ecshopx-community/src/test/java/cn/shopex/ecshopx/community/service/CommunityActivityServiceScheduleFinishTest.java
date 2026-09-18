package cn.shopex.ecshopx.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.CommunitySettingReadPort;
import cn.shopex.ecshopx.common.dispatch.CancelActivityOrdersJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderRow;
import cn.shopex.ecshopx.community.domain.CommunityActivity;
import cn.shopex.ecshopx.community.mapper.CommunityActivityItemMapper;
import cn.shopex.ecshopx.community.mapper.CommunityActivityMapper;
import cn.shopex.ecshopx.community.mapper.CommunityActivityOrderStatsMapper;
import cn.shopex.ecshopx.community.mapper.CommunityActivityZitiMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefZitiMapper;
import cn.shopex.ecshopx.community.mapper.CommunityItemsMapper;
import cn.shopex.ecshopx.community.dto.ScheduleFinishGoodsRow;
import cn.shopex.ecshopx.community.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityActivityServiceScheduleFinishTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CommunityActivity.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
	}

	@Mock
	private CommunityActivityMapper communityActivityMapper;
	@Mock
	private CommunityActivityItemMapper communityActivityItemMapper;
	@Mock
	private CommunityActivityZitiMapper communityActivityZitiMapper;
	@Mock
	private CommunityChiefZitiMapper communityChiefZitiMapper;
	@Mock
	private ItemsMapper itemsMapper;
	@Mock
	private CommunityActivityOrderStatsMapper communityActivityOrderStatsMapper;
	@Mock
	private ObjectMapper objectMapper;
	@Mock
	private TransactionTemplate transactionTemplate;
	@Mock
	private ItemRelAttributesRepository itemRelAttributesRepository;
	@Mock
	private ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;
	@Mock
	private CommunitySettingService communitySettingService;
	@Mock
	private CommunityChiefMapper communityChiefMapper;
	@Mock
	private CommunityItemsMapper communityItemsMapper;
	@Mock
	private CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock
	private MembersInfoMapper membersInfoMapper;
	@Mock
	private CommunitySettingReadPort communitySettingReadPort;
	@Mock
	private CancelActivityOrdersJobDispatchPublisher cancelActivityOrdersJobDispatchPublisher;

	private CommunityActivityService service;

	@BeforeEach
	void setUp() {
		service =
				new CommunityActivityService(
						communityActivityMapper,
						communityActivityItemMapper,
						communityActivityZitiMapper,
						communityChiefZitiMapper,
						itemsMapper,
						communityActivityOrderStatsMapper,
						objectMapper,
						transactionTemplate,
						itemRelAttributesRepository,
						itemsRelAttrValuesQueryService,
						communitySettingService,
						communityChiefMapper,
						communityItemsMapper,
						communityOrderRelActivityMapper,
						normalOrdersMapper,
						normalOrdersItemsMapper,
						membersInfoMapper,
						communitySettingReadPort,
						cancelActivityOrdersJobDispatchPublisher);
		doAnswer(
						invocation -> {
							Consumer<TransactionStatus> c = invocation.getArgument(0);
							c.accept(null);
							return null;
						})
				.when(transactionTemplate)
				.executeWithoutResult(any(Consumer.class));
	}

	@Test
	void noDueActivities_returnsZero() {
		when(communityActivityMapper.selectList(any())).thenReturn(List.of());
		assertThat(service.scheduleFinishActivity()).isZero();
		verify(communityActivityOrderStatsMapper, never()).listScheduleFinishGoodsForActivity(anyLong());
	}

	@Test
	void emptyGoods_numMode_doubleFailThenSuccess_noOrdersToCancel() {
		CommunityActivity a = new CommunityActivity();
		a.setActivityId(10L);
		a.setCompanyId(1L);
		a.setDistributorId(0);
		a.setEndTime(0);
		when(communityActivityMapper.selectList(any())).thenReturn(List.of(a));
		when(communityActivityMapper.selectById(10L)).thenReturn(a);
		when(communityActivityMapper.update(any(), any())).thenReturn(1);
		when(communityActivityOrderStatsMapper.listScheduleFinishGoodsForActivity(10L)).thenReturn(List.of());
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("condition_type", "num");
		st.put("condition_money", 0);
		when(communitySettingReadPort.getSetting(1L, true, 0)).thenReturn(st);
		when(normalOrdersMapper.selectCount(any())).thenReturn(0L);
		assertThat(service.scheduleFinishActivity()).isEqualTo(1);
		verify(communityActivityMapper, times(2)).update(any(), any());
		verify(cancelActivityOrdersJobDispatchPublisher, never()).publishBatch(any());
	}

	@Test
	void scheduleSuccess_withNotpayOrders_publishesCancelBatchViaDispatchPublisher() {
		CommunityActivity a = new CommunityActivity();
		a.setActivityId(10L);
		a.setCompanyId(1L);
		a.setDistributorId(0);
		when(communityActivityMapper.selectList(any())).thenReturn(List.of(a));
		when(communityActivityMapper.selectById(10L)).thenReturn(a);
		when(communityActivityMapper.update(any(), any())).thenReturn(1);

		ScheduleFinishGoodsRow g = new ScheduleFinishGoodsRow();
		g.setGoodsId(1L);
		g.setBuyNum(5);
		g.setTotalFeeCents(0);
		g.setMinDeliveryNum(0);
		when(communityActivityOrderStatsMapper.listScheduleFinishGoodsForActivity(10L)).thenReturn(List.of(g));

		Map<String, Object> st = new LinkedHashMap<>();
		st.put("condition_type", "num");
		st.put("condition_money", 0);
		when(communitySettingReadPort.getSetting(1L, true, 0)).thenReturn(st);

		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		NormalOrders o = new NormalOrders();
		o.setCompanyId(1L);
		o.setOrderId(99L);
		o.setUserId(2L);
		o.setMobile("138");
		when(normalOrdersMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Page<NormalOrders> pageArg = (Page<NormalOrders>) invocation.getArgument(0);
			Page<NormalOrders> pg = new Page<>(pageArg.getCurrent(), pageArg.getSize());
			pg.setRecords(List.of(o));
			pg.setTotal(1L);
			return pg;
		});

		assertThat(service.scheduleFinishActivity()).isEqualTo(1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CommunityActivityCancelOrderRow>> cap = ArgumentCaptor.forClass(List.class);
		verify(cancelActivityOrdersJobDispatchPublisher, times(1)).publishBatch(cap.capture());
		List<CommunityActivityCancelOrderRow> rows = cap.getValue();
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getCancelFrom()).isEqualTo("system");
		assertThat(rows.get(0).getChiefId()).isNull();
		assertThat(rows.get(0).getCancelReason()).isEqualTo("已成团，取消未支付订单");
		assertThat(rows.get(0).getOrderId()).isEqualTo(99L);
	}

	@Test
	void scheduleFinishActivity_whenTxSyncActive_defersPublishBatchUntilAfterCommit() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
		try {
			TransactionTemplate realTx =
					new TransactionTemplate(syncFiringTxManagerForPublishProbe(cancelActivityOrdersJobDispatchPublisher));
			CommunityActivityService svc =
					new CommunityActivityService(
							communityActivityMapper,
							communityActivityItemMapper,
							communityActivityZitiMapper,
							communityChiefZitiMapper,
							itemsMapper,
							communityActivityOrderStatsMapper,
							objectMapper,
							realTx,
							itemRelAttributesRepository,
							itemsRelAttrValuesQueryService,
							communitySettingService,
							communityChiefMapper,
							communityItemsMapper,
							communityOrderRelActivityMapper,
							normalOrdersMapper,
							normalOrdersItemsMapper,
							membersInfoMapper,
							communitySettingReadPort,
							cancelActivityOrdersJobDispatchPublisher);

			CommunityActivity a = new CommunityActivity();
			a.setActivityId(10L);
			a.setCompanyId(1L);
			a.setDistributorId(0);
			when(communityActivityMapper.selectList(any())).thenReturn(List.of(a));
			when(communityActivityMapper.selectById(10L)).thenReturn(a);
			when(communityActivityMapper.update(any(), any())).thenReturn(1);

			ScheduleFinishGoodsRow g = new ScheduleFinishGoodsRow();
			g.setGoodsId(1L);
			g.setBuyNum(5);
			g.setTotalFeeCents(0);
			g.setMinDeliveryNum(0);
			when(communityActivityOrderStatsMapper.listScheduleFinishGoodsForActivity(10L)).thenReturn(List.of(g));

			Map<String, Object> st = new LinkedHashMap<>();
			st.put("condition_type", "num");
			st.put("condition_money", 0);
			when(communitySettingReadPort.getSetting(1L, true, 0)).thenReturn(st);

			when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
			NormalOrders o = new NormalOrders();
			o.setCompanyId(1L);
			o.setOrderId(99L);
			o.setUserId(2L);
			o.setMobile("138");
			when(normalOrdersMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
				@SuppressWarnings("unchecked")
				Page<NormalOrders> pageArg = (Page<NormalOrders>) invocation.getArgument(0);
				Page<NormalOrders> pg = new Page<>(pageArg.getCurrent(), pageArg.getSize());
				pg.setRecords(List.of(o));
				pg.setTotal(1L);
				return pg;
			});

			assertThat(svc.scheduleFinishActivity()).isEqualTo(1);
			verify(cancelActivityOrdersJobDispatchPublisher, times(1)).publishBatch(any());
		} finally {
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.clear();
			}
		}
	}

	private static PlatformTransactionManager syncFiringTxManagerForPublishProbe(
			CancelActivityOrdersJobDispatchPublisher publisher) {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new IllegalStateException("nested tx not expected in unit test");
				}
				TransactionSynchronizationManager.initSynchronization();
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				verify(publisher, never()).publishBatch(any());
				try {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization synchronization :
								new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())) {
							synchronization.afterCommit();
						}
					}
				} finally {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.clearSynchronization();
					}
				}
				verify(publisher, times(1)).publishBatch(any());
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					TransactionSynchronizationManager.clearSynchronization();
				}
			}
		};
	}
}
