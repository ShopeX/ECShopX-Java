package cn.shopex.ecshopx.employeepurchase.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActivityListDisplayStatusQueryTest {

	private static final int NOW = 1_800_000_000;

	@Test
	@DisplayName("PHP对齐：未开通亲友购 relative_begin=null 的预热活动判为 warm_up")
	void prepaidWarmUp_nullRelativeBegin_isWarmUp() {
		Map<String, Object> row = rewrite("active", NOW - 100, NOW + 1000, NOW + 5000, null, null);
		assertEquals("warm_up", row.get("status"));
		assertEquals("预热中", row.get("status_desc"));
	}

	@Test
	@DisplayName("PHP对齐：relative_begin=0 的预热活动判为 warm_up，不能因 0<now 误判 ongoing")
	void prepaidWarmUp_zeroRelativeBegin_isWarmUpNotOngoing() {
		Map<String, Object> row = rewrite("active", NOW - 100, NOW + 1000, NOW + 5000, 0L, 0L);
		assertEquals("warm_up", row.get("status"));
		assertEquals("预热中", row.get("status_desc"));
	}

	@Test
	@DisplayName("亲友开始时间也在未来时仍为预热中")
	void warmUp_futureRelativeBegin() {
		Map<String, Object> row =
				rewrite("active", NOW - 100, NOW + 1000, NOW + 5000, NOW + 2000L, NOW + 6000L);
		assertEquals("warm_up", row.get("status"));
	}

	@Test
	@DisplayName("员工已开始且未结束为进行中")
	void ongoing_employeeStarted() {
		Map<String, Object> row = rewrite("active", NOW - 1000, NOW - 10, NOW + 5000, 0L, 0L);
		assertEquals("ongoing", row.get("status"));
		assertEquals("进行中", row.get("status_desc"));
	}

	@Test
	@DisplayName("亲友已开始（>0）即使员工未开始也是进行中")
	void ongoing_relativeStartedEmployeeNot() {
		Map<String, Object> row =
				rewrite("active", NOW - 100, NOW + 1000, NOW + 5000, NOW - 10L, NOW + 6000L);
		assertEquals("ongoing", row.get("status"));
	}

	@Test
	@DisplayName("尚未到预热展示时间是未开始")
	void notStarted_displayInFuture() {
		Map<String, Object> row = rewrite("active", NOW + 100, NOW + 1000, NOW + 5000, null, null);
		assertEquals("not_started", row.get("status"));
	}

	@Test
	@DisplayName("无改写命中时保持库状态")
	void noRewrite_keepsDbStatus() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("status", "active");
		ActivityListDisplayStatusQuery.applyDisplayStatusRewrite(
				row, "active", NOW, NOW, NOW + 5000, null, null, NOW);
		assertEquals("active", row.get("status"));
		assertNull(row.get("status_desc"));
	}

	private static Map<String, Object> rewrite(
			String dbStatus,
			long display,
			long employeeBegin,
			long employeeEnd,
			Long relativeBegin,
			Long relativeEnd) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("status", dbStatus);
		ActivityListDisplayStatusQuery.applyDisplayStatusRewrite(
				row, dbStatus, display, employeeBegin, employeeEnd, relativeBegin, relativeEnd, NOW);
		return row;
	}
}
