package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.goods.ItemBatchEditStatusMarketingSkuRowsPort;
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
class ItemBatchEditStatusPushMarketingCenterProcessorTest {

	@Mock
	private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	@Mock
	private ItemBatchEditStatusMarketingSkuRowsPort itemBatchEditStatusMarketingSkuRowsPort;

	private ItemBatchEditStatusPushMarketingCenterProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new ItemBatchEditStatusPushMarketingCenterProcessor(
				marketingCenterOpenApiSignedFormClient, itemBatchEditStatusMarketingSkuRowsPort);
	}

	@Test
	void handle_whenPortReturnsRows_callsBasicsItemProccessWithExpectedParams() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 101L);
		payload.put("goods_id", 202L);
		payload.put("approve_status", "onsale");

		List<Map<String, Object>> portRows = new ArrayList<>();
		LinkedHashMap<String, Object> r0 = new LinkedHashMap<>();
		r0.put("item_bn", "BN-A");
		r0.put("approve_status", "instock");
		portRows.add(r0);
		LinkedHashMap<String, Object> r1 = new LinkedHashMap<>();
		r1.put("item_bn", "BN-B");
		r1.put("approve_status", "onsale");
		portRows.add(r1);

		when(itemBatchEditStatusMarketingSkuRowsPort.listRows(101L, 202L)).thenReturn(portRows);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient)
				.basicsItemProccess(
						eq(101L),
						argThat(
								params -> {
									if (!(params instanceof LinkedHashMap<?, ?> m)) {
										return false;
									}
									if (m.size() != 2) {
										return false;
									}
									Object row0 = m.get("0");
									Object row1 = m.get("1");
									if (!(row0 instanceof Map<?, ?> map0) || !(row1 instanceof Map<?, ?> map1)) {
										return false;
									}
									return "BN-A".equals(map0.get("item_bn"))
											&& "instock".equals(map0.get("approve_status"))
											&& "BN-B".equals(map1.get("item_bn"))
											&& "onsale".equals(map1.get("approve_status"));
								}));
	}

	@Test
	void handle_whenPortReturnsEmpty_doesNotCallMarketingClient() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("goods_id", 2L);
		payload.put("approve_status", "onsale");

		when(itemBatchEditStatusMarketingSkuRowsPort.listRows(1L, 2L)).thenReturn(List.of());

		processor.handle(payload);

		verifyNoInteractions(marketingCenterOpenApiSignedFormClient);
	}
}
