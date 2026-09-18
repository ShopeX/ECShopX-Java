package cn.shopex.ecshopx.orders.statement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatementTimeWindowCalculatorTest {

	@Test
	void scheduleWindow_day_movesForward() {
		long last = 1_704_000_000L;
		long end = StatementTimeWindowCalculator.computeScheduleWindowEndForLastLine(last, 1, "day");
		assertThat(end).isGreaterThan(last);
	}
}
