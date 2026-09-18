package cn.shopex.ecshopx.promotions.service.register;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeOrderReceiveMembercardPromotionService;
import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Front GET {@code getMemberCard} orchestration: {@link RegisterPromotionsMembercardPromotionService}
 * delegates to kaquan {@code receive} path only — no {@code job:5} / Fire publisher dispatch.
 */
@ExtendWith(MockitoExtension.class)
class RegisterPromotionsMembercardPromotionServiceGetMemberCardNoFireJobTest {

	@Mock
	private RegisterPromotionsMapper registerPromotionsMapper;

	@Mock
	private RegisterPromotionMembercardItemsApplyService registerPromotionMembercardItemsApplyService;

	@Mock
	private RegisterPromotionMembercardCouponsApplyService registerPromotionMembercardCouponsApplyService;

	@Mock
	private VipGradeOrderReceiveMembercardPromotionService vipGradeOrderReceiveMembercardPromotionService;

	/**
	 * Not injected into {@link RegisterPromotionsMembercardPromotionService}; retained only for
	 * {@code verify(never())} contract documentation (same pattern as kaquan no-fire test).
	 */
	@Mock
	private FirePromotionsActivityDispatchPublisher orphanPublisher;

	private RegisterPromotionsMembercardPromotionService sut;

	/**
	 * Mirrors production {@link RegisterPromotionsMembercardPromotionService} constructor (no
	 * {@link FirePromotionsActivityDispatchPublisher} parameter). Same compile-time contract pattern as
	 * kaquan {@code VipGradeOrderReceiveMembercardPromotionNoFireJobTest}.
	 */
	@BeforeEach
	void setUp() {
		sut =
				new RegisterPromotionsMembercardPromotionService(
						registerPromotionsMapper,
						new ObjectMapper(),
						registerPromotionMembercardItemsApplyService,
						registerPromotionMembercardCouponsApplyService,
						vipGradeOrderReceiveMembercardPromotionService);
	}

	@Test
	void constructorParameterTypesExcludeFirePromotionsPublisher() {
		assertNotNull(sut);
	}

	@Test
	void getMembercardPromotions_whenMembercardSectionPresent_delegatesToReceiveOnly() {
		long companyId = 1L;
		long userId = 50L;
		String mobile = "13800000000";

		when(registerPromotionsMapper.selectOne(any())).thenReturn(openMembercardRow());
		Map<String, Object> order = sampleOrderMap();
		when(vipGradeOrderReceiveMembercardPromotionService.receiveMemberCardForPromotionReceive(
						anyLong(), anyLong(), any(), any()))
				.thenReturn(order);

		Map<String, Object> body = sut.getMembercardPromotions(companyId, userId, mobile);

		assertEquals(order, body.get("status"));
		verify(vipGradeOrderReceiveMembercardPromotionService)
				.receiveMemberCardForPromotionReceive(
						eq(companyId),
						eq(userId),
						eq(mobile),
						argThat(m -> m != null && m.containsKey("vip_grade_id")));
		verify(orphanPublisher, never()).publish(anyLong(), any(), any());
		verify(registerPromotionMembercardItemsApplyService, never()).applyItems(anyLong(), anyLong(), any(), any());
		verify(registerPromotionMembercardCouponsApplyService, never())
				.applyCoupons(anyLong(), anyLong(), any(), any());
	}

	private static RegisterPromotions openMembercardRow() {
		RegisterPromotions row = new RegisterPromotions();
		row.setId(1L);
		row.setCompanyId(1L);
		row.setRegisterType("membercard");
		row.setIsOpen("true");
		row.setPromotionsValue("{\"membercard\":{\"vip_grade_id\":1,\"card_type\":\"month\"}}");
		return row;
	}

	private static Map<String, Object> sampleOrderMap() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("source_type", "receive");
		m.put("vip_grade_order_id", 9001L);
		return m;
	}
}
