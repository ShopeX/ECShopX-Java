package cn.shopex.ecshopx.kaquan.service.vipgrade;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.kaquan.port.BatchReceiveMemberCardDispatchPublisher;
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class VipGradeBatchActiveDelayServiceExpiredBatchNoFireJobTest {

	@Mock
	private VipGradeMapper vipGradeMapper;

	@Mock
	private VipGradeRelUserMapper vipGradeRelUserMapper;

	@Mock
	private MembersMapper membersMapper;

	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@Mock
	private VipGradeOrderReceiveService vipGradeOrderReceiveService;

	@Mock
	private BatchReceiveMemberCardDispatchPublisher batchReceiveMemberCardDispatchPublisher;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	/**
	 * Not a constructor parameter of {@link VipGradeBatchActiveDelayService}; used only for {@code never()}
	 * verification (same pattern as {@link VipGradeOrderReceiveServiceAdminDelayNoFireJobTest}).
	 */
	@Mock
	private FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;

	private VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService;

	@BeforeEach
	void setUp() {
		lenient()
				.when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
		vipGradeBatchActiveDelayService = new VipGradeBatchActiveDelayService(
				vipGradeMapper,
				vipGradeRelUserMapper,
				membersMapper,
				sensitiveFieldEncryptor,
				vipGradeOrderReceiveService,
				batchReceiveMemberCardDispatchPublisher,
				new ObjectMapper(),
				platformTransactionManager);
	}

	@Test
	void vipGradeBatchActiveDelayService_constructorDoesNotRequireFirePromotionsPublisher() {
		assertNotNull(vipGradeBatchActiveDelayService);
	}

	@Test
	void runExpiredJobInOneTransaction_executesStubbedLoop_neverPublishesFirePromotions() {
		when(vipGradeRelUserMapper.selectCount(any())).thenReturn(0L);

		vipGradeBatchActiveDelayService.runExpiredJobInOneTransaction(1L, 1L, 7, "vip");

		verify(batchReceiveMemberCardDispatchPublisher, never()).publish(anyLong(), anyLong(), anyInt(), any());
		verify(firePromotionsActivityDispatchPublisher, never()).publish(anyLong(), any(), any());
	}

	@Test
	void processBatchActiveDelay_expiredBranch_reachesRunExpiredOrEquivalent_neverPublishesFirePromotions() {
		when(vipGradeMapper.selectOne(any())).thenReturn(sampleGrade());
		when(vipGradeRelUserMapper.selectCount(any())).thenReturn(5L);
		when(vipGradeRelUserMapper.selectPage(any(Page.class), any()))
				.thenAnswer(invocation -> {
					@SuppressWarnings("unchecked")
					Page<VipGradeRelUser> page = invocation.getArgument(0);
					VipGradeRelUser rel = new VipGradeRelUser();
					rel.setId(1L);
					rel.setCompanyId(1);
					rel.setUserId(42L);
					rel.setVipType("vip");
					rel.setEndDate("1");
					page.setRecords(List.of(rel));
					return page;
				});

		Members member = new Members();
		member.setUserId(42L);
		member.setCompanyId(1L);
		member.setMobile("enc-mobile");
		when(membersMapper.selectList(any())).thenReturn(List.of(member));
		when(sensitiveFieldEncryptor.decrypt("enc-mobile")).thenReturn("13800000000");

		vipGradeBatchActiveDelayService.processBatchActiveDelay(1L, 1L, 7, "expired", null);

		verify(vipGradeOrderReceiveService)
				.receiveAdminCustomDelay(eq(1L), eq(42L), eq("13800000000"), eq(1L), eq(7));
		verify(batchReceiveMemberCardDispatchPublisher, never()).publish(anyLong(), anyLong(), anyInt(), any());
		verify(firePromotionsActivityDispatchPublisher, never()).publish(anyLong(), any(), any());
	}

	@Test
	void processBatchActiveDelay_expiredBranch_overFifty_onlyPublishesJob_notSyncPath() {
		when(vipGradeMapper.selectOne(any())).thenReturn(sampleGrade());
		when(vipGradeRelUserMapper.selectCount(any())).thenReturn(51L);

		vipGradeBatchActiveDelayService.processBatchActiveDelay(1L, 2L, 7, "expired", null);

		verify(batchReceiveMemberCardDispatchPublisher).publish(eq(1L), eq(2L), eq(7), eq("vip"));
		verify(vipGradeOrderReceiveService, never()).receiveAdminCustomDelay(anyLong(), anyLong(), any(), anyLong(), anyInt());
		verify(firePromotionsActivityDispatchPublisher, never()).publish(anyLong(), any(), any());
	}

	private static VipGrade sampleGrade() {
		VipGrade g = new VipGrade();
		g.setVipGradeId(1L);
		g.setCompanyId(1);
		g.setLvType("vip");
		g.setGradeName("Gold");
		g.setIsDisabled(Boolean.FALSE);
		g.setPriceList("[{\"name\":\"month\",\"day\":30,\"desc\":\"m\",\"price\":0}]");
		g.setPrivileges("{\"discount\":10}");
		return g;
	}
}
