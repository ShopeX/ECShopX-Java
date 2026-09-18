package cn.shopex.ecshopx.promotions.domain.turntable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TurntablePublicConfigRulesTest {

	@Test
	void displayStatus_matchesTimeWindow() {
		assertThat(TurntablePublicConfigRules.displayStatus(100, 200, 99)).isEqualTo("notstart");
		assertThat(TurntablePublicConfigRules.displayStatus(100, 200, 100)).isEqualTo("online");
		assertThat(TurntablePublicConfigRules.displayStatus(100, 200, 200)).isEqualTo("expire");
	}

	@Test
	void remainCount_zeroMeansUnlimited() {
		assertThat(TurntablePublicConfigRules.remainCount(0, 99)).isNull();
		assertThat(TurntablePublicConfigRules.remainCount(5, 2)).isEqualTo(3L);
		assertThat(TurntablePublicConfigRules.remainCount(5, 9)).isEqualTo(0L);
	}

	@Test
	void sanitize_stripsProbabilityAndStock() {
		Map<String, Object> raw = new LinkedHashMap<>();
		raw.put("prize_id", "p1");
		raw.put("name", "券");
		raw.put("type", "coupon");
		raw.put("value", "88");
		raw.put("probability", 20);
		raw.put("prize_probability", 20);
		raw.put("dailyStock", 3);
		raw.put("stock", 3);
		raw.put("backgroundColor", "#fff");
		List<Map<String, Object>> out =
				TurntablePublicConfigRules.sanitizePrizesForPublic(List.of(raw));
		assertThat(out).hasSize(1);
		assertThat(TurntablePublicConfigRules.containsSensitivePrizeFields(out.get(0))).isFalse();
		assertThat(out.get(0).get("prizeId")).isEqualTo("p1");
		assertThat(out.get(0).get("backgroundColor")).isEqualTo("#fff");
		assertThat(out.get(0).get("sector_index")).isEqualTo(0);
	}
}
