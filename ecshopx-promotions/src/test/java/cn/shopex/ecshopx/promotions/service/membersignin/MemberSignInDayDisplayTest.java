package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDayStatus;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MemberSignInDayDisplayTest {

	@Test
	void nullableDailyPoints_returnsNullWhenZero() {
		MemberSigninRecord record = new MemberSigninRecord();
		record.setDailyPoints(0);
		assertThat(MemberSignInDayDisplay.nullableDailyPoints(record)).isNull();
	}

	@Test
	void resolveDisplay_showsSignedLabelWhenNoPoints() {
		assertThat(MemberSignInDayDisplay.resolveDisplay(true, null)).isEqualTo("已签");
		assertThat(MemberSignInDayDisplay.resolveDisplay(true, 3)).isEqualTo("+3");
		assertThat(MemberSignInDayDisplay.resolveDisplay(false, 3)).isEqualTo("+3");
		assertThat(MemberSignInDayDisplay.resolveDisplay(false, null)).isNull();
	}

	@Test
	void buildDayMap_includesDayStatusForMonthView() {
		LocalDate today = LocalDate.of(2026, 9, 15);
		MemberSigninRecord record = new MemberSigninRecord();
		record.setDailyPoints(1);
		record.setActivitySeq(2);

		var signed =
				MemberSignInDayDisplay.buildDayMap(
						LocalDate.of(2026, 9, 15), today, record, true);
		assertThat(signed.get("signed")).isEqualTo(true);
		assertThat(signed.get("daily_points")).isEqualTo(1);
		assertThat(signed.get("display")).isEqualTo("+1");
		assertThat(signed.get("day_status")).isEqualTo(MemberSigninDayStatus.SIGNED);

		var missed =
				MemberSignInDayDisplay.buildDayMap(
						LocalDate.of(2026, 9, 10), today, null, true);
		assertThat(missed.get("day_status")).isEqualTo(MemberSigninDayStatus.MISSED);

		var future =
				MemberSignInDayDisplay.buildDayMap(
						LocalDate.of(2026, 9, 20), today, null, true);
		assertThat(future.get("day_status")).isEqualTo(MemberSigninDayStatus.FUTURE);
	}
}
