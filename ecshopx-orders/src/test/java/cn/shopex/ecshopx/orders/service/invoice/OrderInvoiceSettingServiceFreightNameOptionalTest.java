package cn.shopex.ecshopx.orders.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderInvoiceSettingServiceFreightNameOptionalTest {

	@Mock
	private InvoiceSettingRedisService invoiceSettingRedisService;

	private OrderInvoiceSettingService service;

	@BeforeEach
	void setUp() {
		service = new OrderInvoiceSettingService(invoiceSettingRedisService, new ObjectMapper());
	}

	@Test
	@DisplayName("运费支持开票时名称可空且税率合法：允许保存")
	void setInvoiceSetting_freightSupported_emptyName_validTax_persists() {
		Map<String, Object> data = validAdminPayload();
		data.put("freight_invoice", "2");
		data.put("freight_name", "");
		data.put("freight_tax_rate", "13");

		service.setInvoiceSetting(1001L, data);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(invoiceSettingRedisService).setInvoiceSetting(eq(1001L), captor.capture());
		assertThat(captor.getValue().get("freight_tax_rate")).isEqualTo("13");
		assertThat(captor.getValue().get("freight_invoice")).isEqualTo("2");
	}

	@Test
	@DisplayName("运费支持开票时名称与税率皆空：提示运费税率规则")
	void setInvoiceSetting_freightSupported_emptyNameAndTax_throwsTaxRateMessage() {
		Map<String, Object> data = validAdminPayload();
		data.put("freight_invoice", "2");
		data.put("freight_name", "");
		data.put("freight_tax_rate", "");

		assertThatThrownBy(() -> service.setInvoiceSetting(1001L, data))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("设置开票配置出错:运费税率最小为0，最大为100的整数");
	}

	private static Map<String, Object> validAdminPayload() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("invoice_status", "1");
		data.put("invoice_limit", "order");
		data.put("invoice_method", "offline");
		data.put("freight_invoice", "2");
		data.put("freight_name", "运费");
		data.put("apply_type", "1");
		data.put("invoice_open_term", "0");
		data.put("special_invoice", "1");
		data.put("apply_node", "2");
		data.put("invoice_seller_type", "1");
		data.put("freight_tax_rate", "13");
		return data;
	}
}
