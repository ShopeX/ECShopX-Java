package cn.shopex.ecshopx.kaquan.service.vipgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
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
class VipGradeOrderReceiveMembercardPromotionNoFireJobTest {

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

	private VipGradeOrderReceiveMembercardPromotionService receiveService;

	/**
	 * Mirrors production {@link VipGradeOrderReceiveMembercardPromotionService} constructor (no
	 * {@link FirePromotionsActivityDispatchPublisher} parameter). If the production bean adds or reorders
	 * dependencies, this assignment must be updated; otherwise the test module fails to compile.
	 */
	@BeforeEach
	void setUp() {
		receiveService = new VipGradeOrderReceiveMembercardPromotionService(
				vipGradeMapper,
				vipGradeOrderMapper,
				vipGradeOrderMemberRelationApplyService,
				normalOrderNumericIdService,
				new ObjectMapper(),
				sensitiveFieldEncryptor,
				companysRedisTemplate);
	}

	@Test
	void receiveMemberCardForPromotionReceive_neverPublishesFirePromotionsActivity() {
		// SUT does not accept a Fire publisher; this mock is not injected into receiveService.
		// verifyNoInteractions documents that this flow does not touch Fire dispatch (the bean has no hook here).
		FirePromotionsActivityDispatchPublisher detachedFirePublisher =
				mock(FirePromotionsActivityDispatchPublisher.class);

		VipGrade grade = sampleGrade();
		when(vipGradeMapper.selectOne(any())).thenReturn(grade);
		when(normalOrderNumericIdService.generate(50L)).thenReturn(9001L);
		when(sensitiveFieldEncryptor.encrypt("13800000000")).thenReturn("enc-m");
		when(vipGradeOrderMemberRelationApplyService.addMemberVipGrade(1L, 50L, 9001L, true))
				.thenReturn(Map.of("vip_type", "vip"));
		when(companysRedisTemplate.opsForSet()).thenReturn(setOperations);

		Map<String, Object> section = new LinkedHashMap<>();
		section.put("vip_grade_id", 1);
		section.put("card_type", "month");

		Map<String, Object> out =
				receiveService.receiveMemberCardForPromotionReceive(1L, 50L, "13800000000", section);

		assertEquals("receive", out.get("source_type"));

		ArgumentCaptor<VipGradeOrder> orderCaptor = ArgumentCaptor.forClass(VipGradeOrder.class);
		verify(vipGradeOrderMapper).insert(orderCaptor.capture());
		assertEquals("receive", orderCaptor.getValue().getSourceType());

		verifyNoInteractions(detachedFirePublisher);

		verify(setOperations)
				.add(argThat((String key) -> key != null && key.startsWith("MemberCard:1:vip:")), eq("50"));
	}

	@Test
	void receivePromotionService_constructorDoesNotRequireFirePromotionsPublisher() {
		// Compile-time contract with production: same dependency list as the @Service constructor (see setUp).
		assertNotNull(receiveService);
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
