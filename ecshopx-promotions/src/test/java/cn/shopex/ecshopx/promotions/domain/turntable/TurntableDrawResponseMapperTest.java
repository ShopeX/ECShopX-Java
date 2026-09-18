package cn.shopex.ecshopx.promotions.domain.turntable;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TurntableDrawResponseMapperTest {

	@Test
	void requestId_validation() {
		assertThat(TurntableDrawResponseMapper.isValidRequestId(null)).isFalse();
		assertThat(TurntableDrawResponseMapper.isValidRequestId("")).isFalse();
		assertThat(TurntableDrawResponseMapper.isValidRequestId("  ")).isFalse();
		assertThat(TurntableDrawResponseMapper.isValidRequestId("01Jabc")).isTrue();
		assertThat(TurntableDrawResponseMapper.isValidRequestId("x".repeat(65))).isFalse();
	}

	@Test
	void fromLog_mapsSuccessWin() {
		TurntableLog log = new TurntableLog();
		log.setId(9L);
		log.setActId(100L);
		log.setRequestId("r1");
		log.setStatus(TurntableDrawStatus.SUCCESS);
		log.setPrizeId("p-coupon");
		log.setPrizeType("coupon");
		log.setPrizeTitle("券");
		log.setSectorIndex(2);
		log.setCostPoints(10L);
		Map<String, Object> out = TurntableDrawResponseMapper.fromLog(log, 90L);
		assertThat(out.get("status")).isEqualTo("SUCCESS");
		assertThat(out.get("isWin")).isEqualTo(true);
		assertThat(out.get("remainPoints")).isEqualTo(90L);
		assertThat(out.get("sectorIndex")).isEqualTo(2);
	}

	@Test
	void fromLog_thanksIsNotWin() {
		TurntableLog log = new TurntableLog();
		log.setStatus(TurntableDrawStatus.SUCCESS);
		log.setPrizeType("thanks");
		assertThat(TurntableDrawResponseMapper.fromLog(log, null).get("isWin")).isEqualTo(false);
	}

	@Test
	void processing_shape() {
		Map<String, Object> out = TurntableDrawResponseMapper.processing(1L, 2L, "r");
		assertThat(out.get("status")).isEqualTo(TurntableDrawStatus.PROCESSING);
		assertThat(out).containsKeys("activityId", "recordId", "requestId");
	}
}
