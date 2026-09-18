package cn.shopex.ecshopx.promotions.domain.turntable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TurntablePrizeSelectorTest {

	@Test
	void pick_usesAbsolutePercentNotRelativeWeight() {
		List<Map<String, Object>> prizes = new ArrayList<>();
		prizes.add(TurntablePrizeDataSchema.sampleCoupon("A", 1, 20, "11", 10));
		prizes.add(TurntablePrizeDataSchema.sampleThanks("T1", 2, 10));

		assertThat(TurntablePrizeSelector.pick(prizes, 1).prize().get("prize_id")).isEqualTo("A");
		assertThat(TurntablePrizeSelector.pick(prizes, 20).prize().get("prize_id")).isEqualTo("A");
		assertThat(TurntablePrizeSelector.pick(prizes, 21).prize().get("prize_id")).isEqualTo("T1");
		assertThat(TurntablePrizeSelector.pick(prizes, 30).remainderHit()).isFalse();
		assertThat(TurntablePrizeSelector.pick(prizes, 31).remainderHit()).isTrue();
		assertThat(TurntablePrizeSelector.pick(prizes, 31).prize().get("prize_id")).isEqualTo("T1");
		assertThat(TurntablePrizeSelector.pick(prizes, 100).remainderHit()).isTrue();
	}

	@Test
	void pick_zeroProbabilityNeverSelfHits() {
		List<Map<String, Object>> prizes = new ArrayList<>();
		prizes.add(TurntablePrizeDataSchema.sampleCoupon("A", 1, 0, "11", 10));
		prizes.add(TurntablePrizeDataSchema.sampleThanks("T1", 2, 100));
		assertThat(TurntablePrizeSelector.pick(prizes, 1).prize().get("prize_id")).isEqualTo("T1");
	}

	@Test
	void pick_sortsBeforeAccumulate() {
		List<Map<String, Object>> prizes = new ArrayList<>();
		prizes.add(TurntablePrizeDataSchema.sampleThanks("T1", 2, 10));
		prizes.add(TurntablePrizeDataSchema.sampleCoupon("A", 1, 20, "11", 10));
		TurntablePrizeSelector.DrawPick hit = TurntablePrizeSelector.pick(prizes, 15);
		assertThat(hit.sectorIndex()).isEqualTo(0);
		assertThat(hit.prize().get("prize_id")).isEqualTo("A");
	}
}
