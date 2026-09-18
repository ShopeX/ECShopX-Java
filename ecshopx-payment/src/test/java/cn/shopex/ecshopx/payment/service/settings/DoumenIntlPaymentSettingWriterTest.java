package cn.shopex.ecshopx.payment.service.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DoumenIntlPaymentSettingWriterTest {

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private DoumenIntlPaymentSettingWriter writer;

	@BeforeEach
	void setUp() {
		lenient().when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		writer = new DoumenIntlPaymentSettingWriter(companysRedisTemplate, objectMapper);
	}

	@Test
	void write_persistsPlatformKeyAndBoolIsOpen() throws Exception {
		Map<String, Object> scalar = new LinkedHashMap<>();
		scalar.put("is_open", "true");
		scalar.put("X-AccessCode", "AC");
		scalar.put("X-SecretKey", "SECRETKEY123");
		scalar.put("appId", "APP");
		scalar.put("return_url", "https://example.com/r");

		writer.write(command(10001L, scalar));

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations)
				.set(eq(PaymentSettingRedisKeys.doumenIntlPaymentSettingKey(10001L)), jsonCaptor.capture());
		@SuppressWarnings("unchecked")
		Map<String, Object> saved = objectMapper.readValue(jsonCaptor.getValue(), Map.class);
		assertEquals(Boolean.TRUE, saved.get("is_open"));
		assertEquals("AC", saved.get("X-AccessCode"));
		assertEquals("SECRETKEY123", saved.get("X-SecretKey"));
		assertEquals("APP", saved.get("appId"));
		assertEquals("https://example.com/r", saved.get("return_url"));
	}

	@Test
	void write_emptySecretPreservesExisting() throws Exception {
		String key = PaymentSettingRedisKeys.doumenIntlPaymentSettingKey(10001L);
		when(valueOperations.get(key))
				.thenReturn(
						"{\"is_open\":true,\"X-AccessCode\":\"AC\",\"X-SecretKey\":\"OLDSECRET\",\"appId\":\"APP\",\"return_url\":\"https://x\"}");

		Map<String, Object> scalar = new LinkedHashMap<>();
		scalar.put("is_open", "true");
		scalar.put("X-AccessCode", "AC");
		scalar.put("X-SecretKey", "");
		scalar.put("appId", "APP");
		scalar.put("return_url", "https://x");

		writer.write(command(10001L, scalar));

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(eq(key), jsonCaptor.capture());
		@SuppressWarnings("unchecked")
		Map<String, Object> saved = objectMapper.readValue(jsonCaptor.getValue(), Map.class);
		assertEquals("OLDSECRET", saved.get("X-SecretKey"));
	}

	@Test
	void maskSecret_rules() {
		assertEquals("", DoumenIntlPaymentSettingReader.maskSecret(null));
		assertEquals("", DoumenIntlPaymentSettingReader.maskSecret(""));
		assertEquals("****", DoumenIntlPaymentSettingReader.maskSecret("abcd"));
		assertEquals("****6789", DoumenIntlPaymentSettingReader.maskSecret("123456789"));
	}

	@Test
	void reader_masksSecretAndIsConfigured() {
		String key = PaymentSettingRedisKeys.doumenIntlPaymentSettingKey(7L);
		when(valueOperations.get(key))
				.thenReturn(
						"{\"is_open\":true,\"X-AccessCode\":\"AC\",\"X-SecretKey\":\"SECRETKEY99\",\"appId\":\"APP\",\"return_url\":\"https://x\"}");
		DoumenIntlPaymentSettingReader reader =
				new DoumenIntlPaymentSettingReader(companysRedisTemplate, objectMapper);

		Map<String, Object> masked = reader.getMasked(7L);
		assertEquals("****EY99", masked.get("X-SecretKey"));
		assertTrue(reader.isConfigured(7L));
		assertTrue(reader.isOpen(7L));
	}

	@Test
	void reader_emptyConfig() {
		when(valueOperations.get(anyString())).thenReturn(null);
		DoumenIntlPaymentSettingReader reader =
				new DoumenIntlPaymentSettingReader(companysRedisTemplate, objectMapper);
		assertTrue(reader.getMasked(1L).isEmpty());
		assertFalse(reader.isConfigured(1L));
	}

	private static PaymentSettingCommand command(long companyId, Map<String, Object> scalar) {
		return new PaymentSettingCommand(
				companyId, 0L, "admin", 0L, "doumen_intl", new LinkedHashMap<>(scalar), Map.of());
	}
}
