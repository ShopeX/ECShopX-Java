package cn.shopex.ecshopx.orders.service.normal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.port.MemberTotalConsumptionMutatePort;
import cn.shopex.ecshopx.common.kaquan.port.MemberCardGradeListForCronPort;
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.stats.MemberTotalConsumptionReadService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingAdminService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class MemberConsumptionOnNormalOrderFinishServiceJob187ProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), MembersInfo.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Members.class);
	}

	@Mock
	MembersInfoMapper membersInfoMapper;
	@Mock
	NormalOrdersMapper normalOrdersMapper;
	@Mock
	MembersMapper membersMapper;
	@Mock
	MemberTotalConsumptionMutatePort memberTotalConsumptionMutatePort;
	@Mock
	MemberTotalConsumptionReadService memberTotalConsumptionReadService;
	@Mock
	MemberCardGradeListForCronPort memberCardGradeListForCronPort;
	@Mock
	DmCrmSettingAdminService dmCrmSettingAdminService;
	@Mock
	FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;

	MemberConsumptionOnNormalOrderFinishService svcDefaultOem;
	MemberConsumptionOnNormalOrderFinishService svcOemShuyun;

	@BeforeEach
	void setUp() {
		svcDefaultOem =
				new MemberConsumptionOnNormalOrderFinishService(
						membersInfoMapper,
						normalOrdersMapper,
						membersMapper,
						memberTotalConsumptionMutatePort,
						memberTotalConsumptionReadService,
						memberCardGradeListForCronPort,
						dmCrmSettingAdminService,
						firePromotionsActivityDispatchPublisher,
						false);
		svcOemShuyun =
				new MemberConsumptionOnNormalOrderFinishService(
						membersInfoMapper,
						normalOrdersMapper,
						membersMapper,
						memberTotalConsumptionMutatePort,
						memberTotalConsumptionReadService,
						memberCardGradeListForCronPort,
						dmCrmSettingAdminService,
						firePromotionsActivityDispatchPublisher,
						true);
		lenient().when(dmCrmSettingAdminService.getSetting(anyLong())).thenReturn(Map.of("is_open", false));
	}

	@Test
	@DisplayName("grade promoted: publishMemberUpgradeAfterOrderConsumption runs after transaction commit")
	void updateMemberConsumption_whenGradePromoted_dispatchesJob187AfterCommit() {
		MembersInfo info = new MembersInfo();
		info.setCompanyId(1L);
		info.setUserId(100L);
		when(membersInfoMapper.selectOne(any())).thenReturn(info);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(membersMapper.selectOne(any())).thenReturn(memberRow(1L, 100L, 1L, "13900000000"));
		when(memberTotalConsumptionReadService.getTotalConsumption(100L)).thenReturn(new BigDecimal("5000"));

		Map<String, Object> promo = new LinkedHashMap<>();
		promo.put("total_consumption", new BigDecimal("50"));
		Map<String, Object> gradeRow = new LinkedHashMap<>();
		gradeRow.put("grade_id", 10L);
		gradeRow.put("grade_name", "Gold");
		gradeRow.put("promotion_condition", promo);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(gradeRow);
		when(memberCardGradeListForCronPort.listForConsumptionUpgradeScan(1L)).thenReturn(rows);
		when(membersMapper.update(any(), any())).thenReturn(1);

		NormalOrders order = new NormalOrders();
		order.setOrderId(9001L);
		order.setUserId(100L);
		order.setPayType("wxpay");
		order.setTotalFee("5000");

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> svcDefaultOem.updateMemberConsumptionIfNotPointPay(1L, order));

		verify(firePromotionsActivityDispatchPublisher)
				.publishMemberUpgradeAfterOrderConsumption(
						eq(1L),
						argThat(
								m ->
										Long.valueOf(10L).equals(toLong(m.get("grade_id")))
												&& Long.valueOf(100L).equals(toLong(m.get("user_id")))
												&& "13900000000".equals(m.get("mobile"))
												&& "Gold".equals(m.get("grade_name"))));
		verify(memberTotalConsumptionMutatePort).addFenToTotalOrSetZero(eq(100L), eq(new BigDecimal("5000")));
	}

	@Test
	@DisplayName("oem-shuyun: skip aggregate and promotion dispatch")
	void updateMemberConsumption_whenOemShuyun_skipsPromotionDispatch() {
		when(membersInfoMapper.selectOne(any())).thenReturn(new MembersInfo());
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		NormalOrders order = new NormalOrders();
		order.setOrderId(9002L);
		order.setUserId(200L);
		order.setPayType("wxpay");
		order.setTotalFee("10000");

		svcOemShuyun.updateMemberConsumptionIfNotPointPay(1L, order);

		verify(memberTotalConsumptionMutatePort, never()).addFenToTotalOrSetZero(anyLong(), any());
		verify(firePromotionsActivityDispatchPublisher, never())
				.publishMemberUpgradeAfterOrderConsumption(anyLong(), anyMap());
	}

	private static Members memberRow(long companyId, long userId, long gradeId, String mobile) {
		Members m = new Members();
		m.setCompanyId(companyId);
		m.setUserId(userId);
		m.setGradeId(gradeId);
		m.setMobile(mobile);
		return m;
	}

	private static Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}

	private static PlatformTransactionManager syncFiringTxManager() {
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
				try {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization s :
								new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())) {
							s.afterCommit();
						}
					}
				} finally {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.clearSynchronization();
					}
				}
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
