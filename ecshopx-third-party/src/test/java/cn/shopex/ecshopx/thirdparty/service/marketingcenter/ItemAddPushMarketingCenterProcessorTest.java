package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ItemAddPushMarketingCenterProcessorTest {

	@Mock
	private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	@Mock
	private JdbcTemplate jdbcTemplate;

	private ItemAddPushMarketingCenterProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new ItemAddPushMarketingCenterProcessor(marketingCenterOpenApiSignedFormClient, jdbcTemplate);
	}

	@Test
	void handle_whenCompanyAndItemPresent_invokesBasicsItemProccessWithExpectedParams() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_bn", "BN-ADD-1");
		row.put("approve_status", "onsale");
		when(jdbcTemplate.queryForList(anyString(), eq(77L), eq(99L))).thenReturn(List.of(row));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 77L);
		payload.put("item_id", 99L);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient).basicsItemProccess(eq(77L), argThat(params -> {
			if (params == null) {
				return false;
			}
			if (!"BN-ADD-1".equals(params.get("item_bn"))) {
				return false;
			}
			if (!"onsale".equals(params.get("approve_status"))) {
				return false;
			}
			Object rawId = params.get("item_id");
			return rawId instanceof Number && ((Number) rawId).longValue() == 99L;
		}));
	}

	@Test
	void handle_whenCompanyIdMissing_skipsOpenApi() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("item_id", 1L);

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_whenItemIdMissing_skipsOpenApi() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_whenNoDbRow_skipsOpenApi() {
		when(jdbcTemplate.queryForList(anyString(), eq(1L), eq(2L))).thenReturn(List.of());

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("item_id", 2L);

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_acceptsStringCompanyIdAndItemId() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_bn", "Z9");
		row.put("approve_status", "approved");
		when(jdbcTemplate.queryForList(anyString(), eq(55L), eq(3L))).thenReturn(List.of(row));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", "55");
		payload.put("item_id", "3");

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient).basicsItemProccess(eq(55L), argThat(params -> "Z9".equals(params.get("item_bn"))));
	}
}
