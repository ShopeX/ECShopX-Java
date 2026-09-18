package cn.shopex.ecshopx.payment.service.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.payment.FrontDepositPayEnabledPort;
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingInputResolver;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class FrontPaymentSettingReadServiceDepositListTest {

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private ValueOperations<String, String> companysOps;

	@Mock
	private ValueOperations<String, String> sharedOps;

	@Mock
	private PointMemberRuleReadService pointMemberRuleReadService;

	@Mock
	private PaymentSettingInputResolver paymentSettingInputResolver;

	@Mock
	private PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;

	@Mock
	private DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader;

	@Mock
	private FrontDepositPayEnabledPort frontDepositPayEnabledPort;

	@Mock
	private HttpServletRequest request;

	private FrontPaymentSettingReadService service;

	@BeforeEach
	void setUp() {
		lenient().when(companysRedisTemplate.opsForValue()).thenReturn(companysOps);
		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(sharedOps);
		lenient().when(companysOps.get(anyString())).thenReturn(null);
		lenient().when(sharedOps.get(anyString())).thenReturn(null);
		lenient().when(paymentSubjectDistributorIdPort.resolveActualDistributorId(anyLong(), anyLong()))
				.thenAnswer(inv -> inv.getArgument(1));
		lenient().when(doumenIntlPaymentSettingReader.isConfigured(anyLong())).thenReturn(false);
		service = new FrontPaymentSettingReadService(
				companysRedisTemplate,
				sharedStringRedisTemplate,
				new ObjectMapper(),
				pointMemberRuleReadService,
				paymentSettingInputResolver,
				paymentSubjectDistributorIdPort,
				doumenIntlPaymentSettingReader,
				frontDepositPayEnabledPort);
	}

	@Test
	void h5List_appendsDepositAfterCashChannelsWhenRechargeOpen() {
		when(frontDepositPayEnabledPort.isDepositPayEnabled(141L)).thenReturn(true);
		when(companysOps.get(PaymentSettingRedisKeys.wxpayRedisKey(141L, 0L)))
				.thenReturn("{\"is_open\":\"true\"}");
		when(companysOps.get(PaymentSettingRedisKeys.alipayRedisKey(141L, 0L)))
				.thenReturn("{\"is_open\":\"true\"}");

		List<Map<String, Object>> list = service.getPaymentSettingList(request, 141L, 0L, "h5", "", "zh-CN");

		assertEquals(List.of("wxpayh5", "alipayh5", "deposit"), payTypeCodes(list));
		assertEquals("余额支付", list.get(2).get("pay_type_name"));
	}

	@Test
	void h5List_omitsDepositWhenRechargeClosed() {
		when(frontDepositPayEnabledPort.isDepositPayEnabled(141L)).thenReturn(false);

		List<Map<String, Object>> list = service.getPaymentSettingList(request, 141L, 0L, "h5", "", "zh-CN");

		assertFalse(payTypeCodes(list).contains("deposit"));
	}

	@Test
	void h5List_omitsDepositForEmployeePurchaseEvenWhenRechargeOpen() {
		List<Map<String, Object>> list =
				service.getPaymentSettingList(request, 141L, 0L, "h5", "normal_employee_purchase", "zh-CN");

		assertFalse(payTypeCodes(list).contains("deposit"));
		verify(frontDepositPayEnabledPort, never()).isDepositPayEnabled(anyLong());
	}

	@Test
	void h5List_usesEnglishNameWhenLangIsEnCn() {
		when(frontDepositPayEnabledPort.isDepositPayEnabled(141L)).thenReturn(true);

		List<Map<String, Object>> list = service.getPaymentSettingList(request, 141L, 0L, "h5", "", "en-CN");

		assertTrue(payTypeCodes(list).contains("deposit"));
		Map<String, Object> deposit =
				list.stream().filter(r -> "deposit".equals(r.get("pay_type_code"))).findFirst().orElseThrow();
		assertEquals("Balance Payment", deposit.get("pay_type_name"));
	}

	private static List<String> payTypeCodes(List<Map<String, Object>> list) {
		return list.stream().map(r -> String.valueOf(r.get("pay_type_code"))).toList();
	}
}
