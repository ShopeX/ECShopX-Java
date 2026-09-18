package cn.shopex.ecshopx.promotions.domain.turntable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TurntableAdminSaveRulesTest {

	@Test
	void endedAndInProgress_useTimeWindow() {
		assertThat(TurntableAdminSaveRules.isEnded(100, 200, 200)).isTrue();
		assertThat(TurntableAdminSaveRules.isEnded(100, 200, 199)).isFalse();
		assertThat(TurntableAdminSaveRules.isInProgress(100, 200, 100)).isTrue();
		assertThat(TurntableAdminSaveRules.isInProgress(100, 200, 200)).isFalse();
		assertThat(TurntableAdminSaveRules.isInProgress(100, 200, 99)).isFalse();
	}

	@Test
	void normalSave_requiresEndAfterNow() {
		assertThat(TurntableAdminSaveRules.isEndTimeValidForNormalSave(101, 100)).isTrue();
		assertThat(TurntableAdminSaveRules.isEndTimeValidForNormalSave(100, 100)).isFalse();
	}

	@Test
	void limits_requiredAndDayNotExceedTotal() {
		assertThat(TurntableAdminSaveRules.validateLimits(null, 0L)).isNotEmpty();
		assertThat(TurntableAdminSaveRules.validateLimits(0L, 0L)).isEmpty();
		assertThat(TurntableAdminSaveRules.validateLimits(5L, 6L))
				.anyMatch(e -> e.contains("每日次数"));
		assertThat(TurntableAdminSaveRules.validateLimits(0L, 99L)).isEmpty();
	}

	@Test
	void normalize_generatesPrizeId_andMapsLegacyKeys() {
		Map<String, Object> legacy = new LinkedHashMap<>();
		legacy.put("prize_type", "coupon");
		legacy.put("prize_probability", 20);
		legacy.put("prize_value", "88");
		legacy.put("stock", 3);
		legacy.put("name", "券A");
		List<Map<String, Object>> out =
				TurntableAdminSaveRules.normalizeAndAssignPrizeIds(List.of(legacy), null);
		assertThat(out).hasSize(1);
		assertThat(out.get(0).get("prize_id").toString()).isNotBlank();
		assertThat(out.get(0).get("type")).isEqualTo("coupon");
		assertThat(out.get(0).get("probability")).isEqualTo(20);
		assertThat(out.get(0).get("dailyStock")).isEqualTo(3);
		assertThat(TurntablePrizeDataSchema.isValidPrizeList(
						List.of(
								TurntablePrizeDataSchema.sampleThanks("t1", 1, 80),
								out.get(0))))
				.isTrue();
	}

	@Test
	void normalize_keepsExistingPrizeIdByIndex() {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("type", "thanks");
		item.put("probability", 100);
		item.put("name", "谢谢");
		List<Map<String, Object>> out =
				TurntableAdminSaveRules.normalizeAndAssignPrizeIds(List.of(item), List.of("keep-me"));
		assertThat(out.get(0).get("prize_id")).isEqualTo("keep-me");
	}

	@Test
	void regenerateAllPrizeIds_changesEveryId() {
		List<Map<String, Object>> src = new ArrayList<>();
		src.add(TurntablePrizeDataSchema.sampleThanks("old-a", 1, 50));
		src.add(TurntablePrizeDataSchema.sampleCoupon("old-b", 2, 50, "1", 1));
		List<Map<String, Object>> copied = TurntableAdminSaveRules.regenerateAllPrizeIds(src);
		assertThat(copied.get(0).get("prize_id")).isNotEqualTo("old-a");
		assertThat(copied.get(1).get("prize_id")).isNotEqualTo("old-b");
	}
}
