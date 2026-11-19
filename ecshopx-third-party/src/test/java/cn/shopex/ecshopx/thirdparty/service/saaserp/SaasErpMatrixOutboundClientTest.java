package cn.shopex.ecshopx.thirdparty.service.saaserp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.systemlink.OmsQueueLogWritePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

class SaasErpMatrixOutboundClientTest {

	private SaasErpMatrixCertCredentialsReadService credentialsReadService;
	private OmsQueueLogWritePort omsQueueLogWritePort;
	private JdbcTemplate jdbcTemplate;
	private SaasErpMatrixOutboundClient client;

	@BeforeEach
	void setUp() {
		credentialsReadService = mock(SaasErpMatrixCertCredentialsReadService.class);
		omsQueueLogWritePort = mock(OmsQueueLogWritePort.class);
		jdbcTemplate = mock(JdbcTemplate.class);
		client =
				new SaasErpMatrixOutboundClient(
						credentialsReadService,
						omsQueueLogWritePort,
						new ObjectMapper(),
						jdbcTemplate,
						"http://matrix.example/sync",
						"ecos.ecshopx",
						"platform");
	}

	@Test
	void call_whenCredentialsMissing_skipsWithoutLogging() {
		when(credentialsReadService.findCredentials(38L)).thenReturn(Optional.empty());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("tid", "5436647000100278");
		Map<String, Object> result = client.call(38L, "store.trade.add", body);
		assertTrue(result.isEmpty());
		verify(omsQueueLogWritePort, never())
				.insertRequest(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldSkipNonSelfOperated_platformWithDistributor_skips() {
		when(jdbcTemplate.query(eq("SELECT menu_type FROM companys WHERE company_id = ? LIMIT 1"), any(ResultSetExtractor.class), eq(1L)))
				.thenReturn(3);
		when(jdbcTemplate.query(
						eq("SELECT distributor_id FROM orders_normal_orders WHERE order_id = ? LIMIT 1"),
						any(ResultSetExtractor.class),
						eq("9")))
				.thenReturn(129L);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("tid", "9");
		assertTrue(client.shouldSkipNonSelfOperated(1L, "store.trade.add", data));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldSkipNonSelfOperated_standardModel_doesNotSkip() {
		when(jdbcTemplate.query(eq("SELECT menu_type FROM companys WHERE company_id = ? LIMIT 1"), any(ResultSetExtractor.class), eq(38L)))
				.thenReturn(4);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("tid", "5436647000100278");
		assertEquals(false, client.shouldSkipNonSelfOperated(38L, "store.trade.add", data));
	}
}
