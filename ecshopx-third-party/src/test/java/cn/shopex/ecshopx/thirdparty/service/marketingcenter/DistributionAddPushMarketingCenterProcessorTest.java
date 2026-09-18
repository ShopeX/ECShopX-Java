package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributionAddPushMarketingCenterProcessorTest {

	@Mock
	private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	private DistributionAddPushMarketingCenterProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new DistributionAddPushMarketingCenterProcessor(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_whenCompanyAndDistributorPresent_invokesBasicsDistributionProccessWithExpectedParams() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 77L);
		entities.put("distributor_id", 99L);
		entities.put("distributor_bn", "BN-D-1");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient).basicsDistributionProccess(eq(77L), argThat(params -> {
			if (params == null) {
				return false;
			}
			if (!Long.valueOf(77L).equals(toLong(params.get("company_id")))) {
				return false;
			}
			if (!Long.valueOf(99L).equals(toLong(params.get("distributor_id")))) {
				return false;
			}
			return "BN-D-1".equals(params.get("distributor_bn"));
		}));
	}

	@Test
	void handle_whenEntitiesMissing_skipsOpenApi() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("other", 1L);

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_whenCompanyIdMissing_skipsOpenApi() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("distributor_id", 1L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_acceptsStringCompanyIdInEntities() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", "55");
		entities.put("distributor_id", "3");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient).basicsDistributionProccess(eq(55L), argThat(params -> {
			if (params == null) {
				return false;
			}
			return Long.valueOf(55L).equals(toLong(params.get("company_id")))
					&& Long.valueOf(3L).equals(toLong(params.get("distributor_id")));
		}));
	}

	private static Long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return null;
	}
}
