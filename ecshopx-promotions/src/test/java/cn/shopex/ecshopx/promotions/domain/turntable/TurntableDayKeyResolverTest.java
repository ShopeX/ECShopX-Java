package cn.shopex.ecshopx.promotions.domain.turntable;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TurntableDayKeyResolverTest {

	@Test
	void dayKey_usesConfiguredZoneNotSystemDefault() {
		Clock clock = Clock.fixed(Instant.parse("2026-09-01T16:30:00Z"), ZoneId.of("UTC"));
		TurntableDayKeyResolver resolver = new TurntableDayKeyResolver("Asia/Shanghai", clock);
		assertThat(resolver.dayKey(1L)).isEqualTo("2026-09-02");
	}

	@Test
	void invalidZone_fallsBackToShanghai() {
		TurntableDayKeyResolver resolver = new TurntableDayKeyResolver("Not/AZone", Clock.systemUTC());
		assertThat(resolver.resolveZone(1L).getId()).isEqualTo("Asia/Shanghai");
	}
}
