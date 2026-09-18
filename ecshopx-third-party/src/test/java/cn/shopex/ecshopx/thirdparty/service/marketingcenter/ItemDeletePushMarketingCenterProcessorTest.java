package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemDeletePushMarketingCenterProcessorTest {

	@Mock
	private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	private ItemDeletePushMarketingCenterProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new ItemDeletePushMarketingCenterProcessor(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_buildsBasicsItemProccessParams_fromPayload() {
		Map<String, Object> itemInfo = new LinkedHashMap<>();
		itemInfo.put("item_bn", "BN-UNIT-1");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 77L);
		payload.put("del_ids", new ArrayList<>(List.of(10L, 20L)));
		payload.put("item_info", itemInfo);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient).basicsItemProccess(eq(77L), argThat(params -> {
			if (params == null || !params.containsKey("del_ids") || !params.containsKey("item_bn")) {
				return false;
			}
			Object del = params.get("del_ids");
			if (!(del instanceof List<?> list) || list.size() != 2) {
				return false;
			}
			if (((Number) list.get(0)).longValue() != 10L || ((Number) list.get(1)).longValue() != 20L) {
				return false;
			}
			return "BN-UNIT-1".equals(params.get("item_bn"));
		}));
	}

	@Test
	void handle_skipsCallWhenCompanyIdMissing() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("del_ids", List.of(1L));
		Map<String, Object> itemInfo = new LinkedHashMap<>();
		itemInfo.put("item_bn", "x");
		payload.put("item_info", itemInfo);

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_skipsCallWhenDelIdsEmpty() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("del_ids", List.of());
		payload.put("item_info", Map.of("item_bn", "x"));

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_acceptsStringCompanyIdAndIntegerDelIds() {
		Map<String, Object> itemInfo = new LinkedHashMap<>();
		itemInfo.put("item_bn", "Z9");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", "55");
		payload.put("del_ids", new ArrayList<>(List.of(3, 4)));
		payload.put("item_info", itemInfo);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient).basicsItemProccess(eq(55L), argThat(params -> {
			Object del = params.get("del_ids");
			assertThat(del).isInstanceOf(List.class);
			@SuppressWarnings("unchecked")
			List<Long> ids = (List<Long>) del;
			return ids.size() == 2 && ids.get(0) == 3L && ids.get(1) == 4L && "Z9".equals(params.get("item_bn"));
		}));
	}
}
