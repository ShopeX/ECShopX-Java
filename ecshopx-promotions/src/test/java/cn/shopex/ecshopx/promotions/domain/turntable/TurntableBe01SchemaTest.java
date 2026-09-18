package cn.shopex.ecshopx.promotions.domain.turntable;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.TurntablePrizeDayStock;
import cn.shopex.ecshopx.promotions.domain.TurntableUserCount;
import cn.shopex.ecshopx.promotions.domain.TurntableUserDayCount;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TurntableBe01SchemaTest {

	@Test
	void luckyDrawActivity_defaultsConfigVersionToOne() {
		LuckyDrawActivity activity = new LuckyDrawActivity();
		assertThat(activity.getConfigVersion()).isEqualTo(1L);
	}

	@Test
	void turntableLog_exposesBe01SnapshotAndIdempotencyFields() {
		Set<String> fields = fieldNames(TurntableLog.class);
		assertThat(fields)
				.contains(
						"requestId",
						"status",
						"processStep",
						"configVersion",
						"prizeId",
						"sectorIndex",
						"randomValue",
						"costPoints",
						"originalPrizeId",
						"grantNo",
						"errorCode",
						"errorMessage");
	}

	@Test
	void countAndDayStockEntities_exposeUniqueKeyFields() {
		assertThat(fieldNames(TurntableUserCount.class))
				.contains("companyId", "userId", "actId", "totalCount");
		assertThat(fieldNames(TurntableUserDayCount.class))
				.contains("companyId", "userId", "actId", "dayKey", "dayCount");
		assertThat(fieldNames(TurntablePrizeDayStock.class))
				.contains("actId", "prizeId", "dayKey", "reservedCount");
	}

	@Test
	void processStepAndStatus_constantsMatchPrd() {
		assertThat(TurntableDrawStatus.PROCESSING).isEqualTo("PROCESSING");
		assertThat(TurntableDrawStatus.SUCCESS).isEqualTo("SUCCESS");
		assertThat(TurntableDrawStatus.GRANT_FAILED).isEqualTo("GRANT_FAILED");
		assertThat(TurntableDrawStatus.COST_FAILED).isEqualTo("COST_FAILED");

		assertThat(TurntableProcessStep.CREATED).isEqualTo("CREATED");
		assertThat(TurntableProcessStep.PRIZE_SELECTED).isEqualTo("PRIZE_SELECTED");
		assertThat(TurntableProcessStep.DRAW_STOCK_RESERVED).isEqualTo("DRAW_STOCK_RESERVED");
		assertThat(TurntableProcessStep.COUNT_RESERVED).isEqualTo("COUNT_RESERVED");
		assertThat(TurntableProcessStep.POINT_DEDUCTED).isEqualTo("POINT_DEDUCTED");
		assertThat(TurntableProcessStep.GRANTING).isEqualTo("GRANTING");
	}

	@Test
	void prizeSchema_requiresThanksAndPrizeId_andDailyStockForCoupon() {
		List<Map<String, Object>> missingThanks =
				List.of(TurntablePrizeDataSchema.sampleCoupon("p1", 1, 100, "c1", 1));
		assertThat(TurntablePrizeDataSchema.validatePrizeList(missingThanks))
				.anyMatch(e -> e.contains("thanks"));

		List<Map<String, Object>> ok = new ArrayList<>();
		ok.add(TurntablePrizeDataSchema.sampleThanks("t1", 1, 40));
		ok.add(TurntablePrizeDataSchema.sampleCoupon("c1", 2, 60, "100", 0));
		assertThat(TurntablePrizeDataSchema.isValidPrizeList(ok)).isTrue();
		assertThat(TurntablePrizeDataSchema.isDailyStockBlockedToday(0)).isTrue();
		assertThat(TurntablePrizeDataSchema.isDailyStockBlockedToday(1)).isFalse();
	}

	@Test
	void prizeSchema_rejectsMissingDailyStockOnCoupon() {
		Map<String, Object> coupon = TurntablePrizeDataSchema.sampleCoupon("c1", 1, 50, "100", 1);
		coupon.remove("dailyStock");
		List<Map<String, Object>> prizes =
				List.of(TurntablePrizeDataSchema.sampleThanks("t1", 2, 50), coupon);
		assertThat(TurntablePrizeDataSchema.validatePrizeList(prizes))
				.anyMatch(e -> e.contains("dailyStock"));
	}

	@Test
	void limitZero_meansUnlimited() {
		assertThat(TurntablePrizeDataSchema.isUnlimitedCount(0L)).isTrue();
		assertThat(TurntablePrizeDataSchema.isUnlimitedCount(1L)).isFalse();
	}

	private static Set<String> fieldNames(Class<?> type) {
		return Stream.of(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
	}
}
