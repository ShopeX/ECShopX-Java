package cn.shopex.ecshopx.payment.integration.doumenintl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class DoumenIntlTokenRedisStoreTest {

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private DoumenIntlTokenRedisStore store;

	@BeforeEach
	void setUp() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		store = new DoumenIntlTokenRedisStore(companysRedisTemplate, new ObjectMapper());
	}

	@Test
	void setAndGetValidToken() throws Exception {
		store.setToken("AC1", "tok-1", 120);
		ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(eq("doumen_intl:token:AC1"), json.capture());

		when(valueOperations.get("doumen_intl:token:AC1")).thenReturn(json.getValue());
		assertEquals("tok-1", store.getValidToken("AC1"));
	}

	@Test
	void getValidToken_expiredReturnsNull() {
		long expired = System.currentTimeMillis() / 1000L - 10;
		when(valueOperations.get("doumen_intl:token:AC1"))
				.thenReturn("{\"token\":\"t\",\"expires_at\":" + expired + "}");
		assertNull(store.getValidToken("AC1"));
	}
}
