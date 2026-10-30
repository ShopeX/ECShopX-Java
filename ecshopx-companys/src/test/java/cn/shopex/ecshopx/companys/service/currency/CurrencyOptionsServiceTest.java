package cn.shopex.ecshopx.companys.service.currency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CurrencyOptionsServiceTest {

	private final CurrencyOptionsService service = new CurrencyOptionsService();

	@Test
	@DisplayName("返回全部可选币种且含日元卢布与埃及镑")
	void returnsAllAllowedCurrencyOptions() {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) service.getOptions().get("list");

		assertThat(list).hasSize(CurrencyCreateValidator.ALLOWED_CURRENCY_CODES.size());
		assertThat(list).extracting(row -> row.get("currency"))
				.containsExactly("CNY", "HKD", "USD", "TWD", "JPY", "RUB", "EGP");

		Map<String, Object> jpy = list.get(4);
		assertThat(jpy.get("label")).isEqualTo("日元");
		assertThat(jpy.get("title")).isEqualTo("日本日元");
		assertThat(jpy.get("symbol")).isEqualTo("¥");

		Map<String, Object> rub = list.get(5);
		assertThat(rub.get("label")).isEqualTo("卢布");
		assertThat(rub.get("title")).isEqualTo("俄罗斯卢布");
		assertThat(rub.get("symbol")).isEqualTo("₽");

		Map<String, Object> egp = list.get(6);
		assertThat(egp.get("label")).isEqualTo("埃及镑");
		assertThat(egp.get("title")).isEqualTo("埃及镑");
		assertThat(egp.get("symbol")).isEqualTo("E£");
	}
}
