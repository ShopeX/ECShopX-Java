package cn.shopex.ecshopx.systemlink.service.ome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpOpenApiClient;
import cn.shopex.ecshopx.systemlink.service.third.ThirdShopexErpSettingAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeAfterLogiSendOmeNotificationProcessorTest {

	@Mock
	private ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService;

	@Mock
	private AftersalesMapper aftersalesMapper;

	@Mock
	private ShopexErpOpenApiClient shopexErpOpenApiClient;

	private TradeAfterLogiSendOmeNotificationProcessor processor;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		processor = new TradeAfterLogiSendOmeNotificationProcessor(
				thirdShopexErpSettingAdminService, aftersalesMapper, shopexErpOpenApiClient, objectMapper);
	}

	@Test
	void handle_mapsYdCorpCodeToYundaInLogisticsInfo_notLogiNo() throws Exception {
		when(thirdShopexErpSettingAdminService.getShopexErpSetting(10L)).thenReturn(Map.of("is_open", true));

		Aftersales row = new Aftersales();
		row.setCompanyId(10L);
		row.setAftersalesBn(9001L);
		row.setOrderId(77001L);
		row.setSendbackData(
				objectMapper.writeValueAsString(
						Map.of("logi_no", "YD123456", "corp_code", "YD")));
		when(aftersalesMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("aftersales_bn", 9001L);

		processor.handle(payload);

		ArgumentCaptor<Map<String, Object>> afterData = ArgumentCaptor.forClass(Map.class);
		verify(shopexErpOpenApiClient).call(eq(10L), eq("ome.aftersale.logistics_update"), afterData.capture());

		Map<String, Object> sent = afterData.getValue();
		JsonNode logistics = objectMapper.readTree((String) sent.get("logistics_info"));
		assertThat(logistics.path("logi_no").asText()).isEqualTo("YD123456");
		assertThat(logistics.path("logi_company").asText()).isEqualTo("YUNDA");
	}

	@Test
	void handle_leavesTrackingNumberUnchangedWhenItEqualsYdLiteral() throws Exception {
		when(thirdShopexErpSettingAdminService.getShopexErpSetting(11L)).thenReturn(Map.of("is_open", true));

		Aftersales row = new Aftersales();
		row.setCompanyId(11L);
		row.setAftersalesBn(9002L);
		row.setOrderId(77002L);
		row.setSendbackData(
				objectMapper.writeValueAsString(
						Map.of("logi_no", "YD", "corp_code", "SF")));
		when(aftersalesMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 11L);
		payload.put("aftersales_bn", 9002L);

		processor.handle(payload);

		ArgumentCaptor<Map<String, Object>> afterData = ArgumentCaptor.forClass(Map.class);
		verify(shopexErpOpenApiClient).call(eq(11L), eq("ome.aftersale.logistics_update"), afterData.capture());

		JsonNode logistics = objectMapper.readTree((String) afterData.getValue().get("logistics_info"));
		assertThat(logistics.path("logi_no").asText()).isEqualTo("YD");
		assertThat(logistics.path("logi_company").asText()).isEqualTo("SF");
	}
}
