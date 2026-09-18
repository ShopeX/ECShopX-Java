package cn.shopex.ecshopx.kaquan.service.vipgrade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class VipGradeMembercardSalePaidFlowTest {

	@Mock
	private VipGradeOrderMapper vipGradeOrderMapper;

	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@Mock
	private VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService;

	@Mock
	private FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private SetOperations<String, String> setOperations;

	@InjectMocks
	private VipGradeSalePaidCompletionService completionService;

	@Test
	void sale_notpay_completes_dispatches_member_vip_upgrade_once() {
		VipGradeOrder notPay = baseOrder();
		notPay.setOrderStatus("NOTPAY");

		when(companysRedisTemplate.opsForSet()).thenReturn(setOperations);

		when(vipGradeOrderMapper.selectOne(any())).thenReturn(notPay);
		when(vipGradeOrderMapper.update(any(), any())).thenReturn(1);

		VipGradeOrder done = baseOrder();
		done.setOrderStatus("DONE");
		when(vipGradeOrderMapper.selectById(100L)).thenReturn(done);

		when(sensitiveFieldEncryptor.decrypt("enc")).thenReturn("13800000000");
		when(vipGradeOrderMemberRelationApplyService.addMemberVipGrade(1L, 50L, 100L, false))
				.thenReturn(Map.of("vip_type", "vip"));

		completionService.completeSaleOrderAfterTradePaid(1L, 100L);

		verify(firePromotionsActivityDispatchPublisher)
				.publish(
						eq(1L),
						argThat(
								m ->
										"vip".equals(m.get("vip_grade_type"))
												&& Long.valueOf(50L).equals(((Number) m.get("user_id")).longValue())
												&& "13800000000".equals(m.get("mobile"))
												&& "Gold".equals(m.get("grade_name"))),
						eq("member_vip_upgrade"));
	}

	@Test
	void non_sale_source_never_dispatches() {
		VipGradeOrder o = baseOrder();
		o.setSourceType("receive");
		when(vipGradeOrderMapper.selectOne(any())).thenReturn(o);

		completionService.completeSaleOrderAfterTradePaid(1L, 100L);

		verify(firePromotionsActivityDispatchPublisher, never()).publish(any(Long.class), any(), any());
	}

	private static VipGradeOrder baseOrder() {
		VipGradeOrder o = new VipGradeOrder();
		o.setOrderId(100L);
		o.setCompanyId(1);
		o.setUserId(50L);
		o.setSourceType("sale");
		o.setLvType("vip");
		o.setTitle("Gold");
		o.setMobile("enc");
		return o;
	}
}
