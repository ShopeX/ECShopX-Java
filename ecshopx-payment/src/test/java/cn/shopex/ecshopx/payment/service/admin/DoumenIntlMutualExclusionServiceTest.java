package cn.shopex.ecshopx.payment.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DoumenIntlMutualExclusionServiceTest {

	@Mock
	private DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private ValueOperations<String, String> companysOps;

	@Mock
	private ValueOperations<String, String> sharedOps;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private DoumenIntlMutualExclusionService service;

	@BeforeEach
	void setUp() {
		lenient().when(companysRedisTemplate.opsForValue()).thenReturn(companysOps);
		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(sharedOps);
		service =
				new DoumenIntlMutualExclusionService(
						doumenIntlPaymentSettingReader,
						companysRedisTemplate,
						sharedStringRedisTemplate,
						objectMapper);
	}

	@Test
	void validateBeforeSave_blocksOtherChannelWhenDoumenOpen() {
		when(doumenIntlPaymentSettingReader.isOpen(9L)).thenReturn(true);
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() -> service.validateBeforeSave(9L, "wxpay", true));
		assertEquals(DoumenIntlMutualExclusionService.MSG_CLOSE_DOUMEN_FIRST, ex.getMessage());
	}

	@Test
	void validateBeforeSave_allowsPointPay() {
		service.validateBeforeSave(9L, "point_pay", true);
	}

	@Test
	void closeAllOther_writesLiteralCloseValues() throws Exception {
		long companyId = 9L;
		String wxKey = PaymentSettingRedisKeys.wxpayRedisKey(companyId, 0L);
		String aliKey = PaymentSettingRedisKeys.alipayRedisKey(companyId, 0L);
		String icbcKey = PaymentSettingRedisKeys.icbcPaymentSettingKey(companyId);
		String adaKey = PaymentSettingRedisKeys.adapaySettingKey(companyId);

		when(companysOps.get(wxKey)).thenReturn("{\"is_open\":\"true\",\"app_id\":\"a\"}");
		when(companysOps.get(aliKey)).thenReturn("{\"is_open\":true,\"app_id\":\"b\"}");
		when(companysOps.get(icbcKey)).thenReturn("{\"is_open\":1,\"appid\":\"c\"}");
		when(companysOps.get(eq(PaymentSettingRedisKeys.paypalRedisKey(companyId, 0L))))
				.thenReturn(null);
		when(companysOps.get(eq(PaymentSettingRedisKeys.chinaumsPaymentSettingKey(companyId, ""))))
				.thenReturn(null);
		when(companysOps.get(eq(PaymentSettingRedisKeys.offlinePaySettingKey(companyId, "zh-CN"))))
				.thenReturn(null);
		when(companysOps.get(eq(PaymentSettingRedisKeys.bspaySettingKey(companyId)))).thenReturn(null);
		when(companysOps.get(eq(PaymentSettingRedisKeys.hfPaymentSettingKey(companyId))))
				.thenReturn(null);
		when(sharedOps.get(adaKey)).thenReturn(null);

		service.closeAllOtherPaymentMethods(companyId);

		ArgumentCaptor<String> wxJson = ArgumentCaptor.forClass(String.class);
		verify(companysOps).set(eq(wxKey), wxJson.capture());
		assertEquals("false", objectMapper.readTree(wxJson.getValue()).get("is_open").asText());

		ArgumentCaptor<String> aliJson = ArgumentCaptor.forClass(String.class);
		verify(companysOps).set(eq(aliKey), aliJson.capture());
		assertEquals(false, objectMapper.readTree(aliJson.getValue()).get("is_open").asBoolean());

		ArgumentCaptor<String> icbcJson = ArgumentCaptor.forClass(String.class);
		verify(companysOps).set(eq(icbcKey), icbcJson.capture());
		assertEquals(0, objectMapper.readTree(icbcJson.getValue()).get("is_open").asInt());
	}
}
