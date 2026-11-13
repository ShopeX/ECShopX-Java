package cn.shopex.ecshopx.payment.service.settings;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WxpayPaymentSettingAdminReadSupportTest {

	@Test
	void enrich_addsPhpDisplayFieldsWhenPemPresent() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("merchant_id", "1487008022");
		data.put("cert", "-----BEGIN CERTIFICATE-----\nMII\n-----END CERTIFICATE-----\n");
		data.put("cert_key", "-----BEGIN PRIVATE KEY-----\nMII\n-----END PRIVATE KEY-----\n");

		WxpayPaymentSettingAdminReadSupport.enrichAdminDisplayFields(data);

		assertThat(data.get("cert_name")).isEqualTo(WxpayPaymentSettingAdminReadSupport.CERT_FILE_NAME);
		assertThat(data.get("cert_key_name")).isEqualTo(WxpayPaymentSettingAdminReadSupport.KEY_FILE_NAME);
		assertThat(data.get("cert_url"))
				.isEqualTo("/data/httpd/storage/app/wechatPayment/1487008022/apiclient_cert.pem");
		assertThat(data.get("cert_key_url"))
				.isEqualTo("/data/httpd/storage/app/wechatPayment/1487008022/apiclient_key.pem");
		assertThat(data.get("cert")).isEqualTo("-----BEGIN CERTIFICATE-----\nMII\n-----END CERTIFICATE-----\n");
	}

	@Test
	void enrich_skipsCertFieldsWhenMissing() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("merchant_id", "1");
		data.put("app_id", "wx");

		WxpayPaymentSettingAdminReadSupport.enrichAdminDisplayFields(data);

		assertThat(data).doesNotContainKeys("cert_name", "cert_url", "cert_key_name", "cert_key_url");
	}

	@Test
	void enrich_certOnlyWhenKeyAbsent() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("merchant_id", "99");
		data.put("cert", "pem-body");

		WxpayPaymentSettingAdminReadSupport.enrichAdminDisplayFields(data);

		assertThat(data).containsKeys("cert_name", "cert_url");
		assertThat(data).doesNotContainKeys("cert_key_name", "cert_key_url");
	}
}
