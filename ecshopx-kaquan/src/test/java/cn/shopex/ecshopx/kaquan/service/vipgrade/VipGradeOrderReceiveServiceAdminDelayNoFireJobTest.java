package cn.shopex.ecshopx.kaquan.service.vipgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class VipGradeOrderReceiveServiceAdminDelayNoFireJobTest {

	@Mock
	private VipGradeMapper vipGradeMapper;

	@Mock
	private VipGradeOrderMapper vipGradeOrderMapper;

	@Mock
	private VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService;

	@Mock
	private NormalOrderNumericIdService normalOrderNumericIdService;

	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private SetOperations<String, String> setOperations;

	/** Not injected into {@link VipGradeOrderReceiveService}; used only for {@code never()} verification. */
	@Mock
	private FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;

	private VipGradeOrderReceiveService receiveService;

	@BeforeEach
	void setUp() {
		receiveService = new VipGradeOrderReceiveService(
				vipGradeMapper,
				vipGradeOrderMapper,
				vipGradeOrderMemberRelationApplyService,
				normalOrderNumericIdService,
				new ObjectMapper(),
				sensitiveFieldEncryptor,
				companysRedisTemplate);
	}

	@Test
	void receiveAdminCustomDelay_constructorDoesNotRequireFirePromotionsPublisher() {
		assertNotNull(receiveService);
	}

	@Test
	void receiveAdminCustomDelay_executesInsertAndRelation_neverTouchesFirePromotionsPublisher() {
		long companyId = 1L;
		long userId = 50L;
		long vipGradeId = 1L;
		int day = 7;
		long orderId = 9001L;

		when(vipGradeMapper.selectOne(any())).thenReturn(sampleGrade());
		when(normalOrderNumericIdService.generate(userId)).thenReturn(orderId);
		when(sensitiveFieldEncryptor.encrypt("13800000000")).thenReturn("enc-m");
		when(vipGradeOrderMemberRelationApplyService.addMemberVipGrade(companyId, userId, orderId, false))
				.thenReturn(Map.of("vip_type", "vip"));
		when(companysRedisTemplate.opsForSet()).thenReturn(setOperations);

		receiveService.receiveAdminCustomDelay(companyId, userId, "13800000000", vipGradeId, day);

		ArgumentCaptor<VipGradeOrder> orderCaptor = ArgumentCaptor.forClass(VipGradeOrder.class);
		verify(vipGradeOrderMapper).insert(orderCaptor.capture());
		assertEquals("admin", orderCaptor.getValue().getSourceType());

		verify(firePromotionsActivityDispatchPublisher, never()).publish(anyLong(), any(), any());
	}

	@Test
	void receiveMemberCard_jsonRow_executesSameAbsenceSemantics() {
		String json = "[{\"vip_grade_id\":1,\"day\":7}]";
		long companyId = 1L;
		long userId = 50L;
		long orderId = 9002L;

		when(vipGradeMapper.selectOne(any())).thenReturn(sampleGrade());
		when(normalOrderNumericIdService.generate(userId)).thenReturn(orderId);
		when(sensitiveFieldEncryptor.encrypt("13900000000")).thenReturn("enc-m2");
		when(vipGradeOrderMemberRelationApplyService.addMemberVipGrade(companyId, userId, orderId, false))
				.thenReturn(Map.of("vip_type", "vip"));
		when(companysRedisTemplate.opsForSet()).thenReturn(setOperations);

		receiveService.receiveMemberCard(companyId, userId, "13900000000", json);

		ArgumentCaptor<VipGradeOrder> orderCaptor = ArgumentCaptor.forClass(VipGradeOrder.class);
		verify(vipGradeOrderMapper).insert(orderCaptor.capture());
		assertEquals("admin", orderCaptor.getValue().getSourceType());

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
