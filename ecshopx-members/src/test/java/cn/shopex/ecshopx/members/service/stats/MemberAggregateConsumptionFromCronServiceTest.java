package cn.shopex.ecshopx.members.service.stats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.port.MemberTotalConsumptionMutatePort;
import cn.shopex.ecshopx.common.kaquan.port.GradeCardPackageOnUpgradeTriggerPort;
import cn.shopex.ecshopx.common.kaquan.port.MemberCardGradeListForCronPort;
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.stats.MemberAggregateConsumptionFromCronService.ConsumptionAggregate;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingAdminService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class MemberAggregateConsumptionFromCronServiceTest {

	@BeforeAll
	static void initTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Members.class);
	}

	@Mock
	MemberTotalConsumptionMutatePort mutatePort;
	@Mock
	MemberTotalConsumptionReadService readService;
	@Mock
	MemberCardGradeListForCronPort gradeListPort;
	@Mock
	DmCrmSettingAdminService dmCrmSettingAdminService;
	@Mock
	MembersMapper membersMapper;
	@Mock
	FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;
	@Mock
	GradeCardPackageOnUpgradeTriggerPort gradeCardPackageOnUpgradeTriggerPort;

	@InjectMocks
	MemberAggregateConsumptionFromCronService service;

	@BeforeEach
	void oemOff() {
		ReflectionTestUtils.setField(service, "oemShuyun", false);
	}

	/** 5.1 */
	@Test
	void applyAggregates_oemTrue_noMutate() {
		ReflectionTestUtils.setField(service, "oemShuyun", true);
		Map<Long, ConsumptionAggregate> m = new HashMap<>();
		m.put(1L, new ConsumptionAggregate(1L, 10L, new BigDecimal("100")));
		service.applyAggregates(m);
		verify(mutatePort, never()).addFenToTotalOrSetZero(any(long.class), any());
		ReflectionTestUtils.setField(service, "oemShuyun", false);
	}

	/** 5.2 */
	@Test
	void applyAggregates_dmOpen_skipsMutate() {
		Map<String, Object> dm = new HashMap<>();
		dm.put("is_open", true);
		when(dmCrmSettingAdminService.getSetting(10L)).thenReturn(dm);
		Map<Long, ConsumptionAggregate> m = new HashMap<>();
		m.put(1L, new ConsumptionAggregate(1L, 10L, new BigDecimal("50")));
		service.applyAggregates(m);
		verify(mutatePort, never()).addFenToTotalOrSetZero(any(long.class), any());
	}

	/** 5.3 */
	@Test
	void applyAggregates_noMember_noMutate() {
		when(dmCrmSettingAdminService.getSetting(10L)).thenReturn(Map.of("is_open", false));
		when(membersMapper.selectOne(any())).thenReturn(null);
		Map<Long, ConsumptionAggregate> m = new HashMap<>();
		m.put(1L, new ConsumptionAggregate(1L, 10L, new BigDecimal("1")));
		service.applyAggregates(m);
		verify(mutatePort, never()).addFenToTotalOrSetZero(any(long.class), any());
	}

	/** 4.1-1 */
	@Test
	void applyAggregates_invalidPay_zeroSkipped() {
		Map<Long, ConsumptionAggregate> m = new HashMap<>();
		m.put(1L, new ConsumptionAggregate(1L, 10L, BigDecimal.ZERO));
		service.applyAggregates(m);
		verify(mutatePort, never()).addFenToTotalOrSetZero(any(long.class), any());
	}

	/** 5.4, 4.1-2, 5.5, 5.6 */
	@Test
	void applyAggregates_incrementsAndMayUpgrade() {
		when(dmCrmSettingAdminService.getSetting(10L)).thenReturn(Map.of("is_open", false));
		Members mem = new Members();
		mem.setUserId(1L);
		mem.setCompanyId(10L);
		mem.setGradeId(1L);
		mem.setMobile("130");
		when(membersMapper.selectOne(any())).thenReturn(mem);
		when(readService.getTotalConsumption(1L)).thenReturn(new BigDecimal("200"));
		Map<String, Object> grade = new HashMap<>();
		grade.put("grade_id", 2L);
		grade.put("grade_name", "G2");
		Map<String, Object> cond = new HashMap<>();
		cond.put("total_consumption", 1);
		grade.put("promotion_condition", cond);
		when(gradeListPort.listForConsumptionUpgradeScan(10L)).thenReturn(List.of(grade));
		Map<Long, ConsumptionAggregate> m = new HashMap<>();
		m.put(1L, new ConsumptionAggregate(1L, 10L, new BigDecimal("200")));
		service.applyAggregates(m);
		verify(mutatePort, times(1)).addFenToTotalOrSetZero(eq(1L), eq(new BigDecimal("200")));
		verify(membersMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
		verify(firePromotionsActivityDispatchPublisher, times(1))
				.publishMemberUpgradeAfterOrderConsumption(
						eq(10L),
						argThat(
								mi ->
										Long.valueOf(2L).equals(toLong(mi.get("grade_id")))
												&& Long.valueOf(1L).equals(toLong(mi.get("user_id")))
												&& "130".equals(mi.get("mobile"))
												&& "G2".equals(mi.get("grade_name"))));
		verify(gradeCardPackageOnUpgradeTriggerPort, times(1)).trigger(eq(10L), eq(1L), eq(2L), eq("grade"), eq(true));
	}

	@Test
	@DisplayName("grade promoted: publishMemberUpgradeAfterOrderConsumption runs after transaction commit")
	void applyAggregates_whenGradePromoted_publishesAfterCommit() {
		when(dmCrmSettingAdminService.getSetting(10L)).thenReturn(Map.of("is_open", false));
		Members mem = new Members();
		mem.setUserId(1L);
		mem.setCompanyId(10L);
		mem.setGradeId(1L);
		mem.setMobile("130");
		when(membersMapper.selectOne(any())).thenReturn(mem);
		when(readService.getTotalConsumption(1L)).thenReturn(new BigDecimal("200"));
		Map<String, Object> grade = new HashMap<>();
		grade.put("grade_id", 2L);
		grade.put("grade_name", "G2");
		Map<String, Object> cond = new HashMap<>();
		cond.put("total_consumption", 1);
		grade.put("promotion_condition", cond);
		when(gradeListPort.listForConsumptionUpgradeScan(10L)).thenReturn(List.of(grade));
		Map<Long, ConsumptionAggregate> m = new HashMap<>();
		m.put(1L, new ConsumptionAggregate(1L, 10L, new BigDecimal("200")));

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> service.applyAggregates(m));

		verify(firePromotionsActivityDispatchPublisher)
				.publishMemberUpgradeAfterOrderConsumption(
						eq(10L),
						argThat(
								mm ->
										Long.valueOf(2L).equals(toLong(mm.get("grade_id")))
												&& Long.valueOf(1L).equals(toLong(mm.get("user_id")))
												&& "130".equals(mm.get("mobile"))
												&& "G2".equals(mm.get("grade_name"))));
		verify(mutatePort, times(1)).addFenToTotalOrSetZero(eq(1L), eq(new BigDecimal("200")));
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
