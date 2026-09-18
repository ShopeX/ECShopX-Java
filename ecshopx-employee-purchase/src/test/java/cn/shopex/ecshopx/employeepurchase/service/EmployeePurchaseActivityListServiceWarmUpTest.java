package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseUserActivitiesQueryMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseVerifiedRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePurchaseActivityListServiceWarmUpTest {

	@Mock private EmployeePurchaseUserActivitiesQueryMapper userActivitiesQueryMapper;
	@Mock private EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	@Mock private PassphraseVerifiedRedisService passphraseVerifiedRedisService;

	private EmployeePurchaseActivityListService service;

	@BeforeEach
	void setUp() {
		service =
				new EmployeePurchaseActivityListService(
						userActivitiesQueryMapper,
						employeePurchaseActivityDataService,
						new ObjectMapper(),
						passphraseVerifiedRedisService);
	}

	@Test
	@DisplayName("C端 status=warm_up,ongoing 返回未开亲友购的预热活动，status=warm_up")
	void warmUpPrepaidActivity_includedWhenFilteringWarmUpAndOngoing() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", 166L);
		row.put("name", "预热内购");
		row.put("status", "active");
		row.put("display_time", now - 3600);
		row.put("employee_begin_time", now + 86400);
		row.put("employee_end_time", now + 172800);
		row.put("relative_begin_time", null);
		row.put("relative_end_time", null);
		row.put("is_passphrase_enabled", 0);
		row.put("purchase_mode", "prepaid_point");

		when(userActivitiesQueryMapper.countUserActivities(
						eq(141L), eq(1001L), anyInt(), eq(121L), isNull(), isNull()))
				.thenReturn(1L);
		when(userActivitiesQueryMapper.selectUserActivities(
						eq(141L),
						eq(1001L),
						anyInt(),
						eq(121L),
						isNull(),
						isNull(),
						eq(0L),
						anyInt()))
				.thenReturn(List.of(row));
		when(passphraseVerifiedRedisService.isVerified(anyLong(), anyLong(), anyLong(), anyLong()))
				.thenReturn(false);

		Map<String, Object> result =
				service.getActivityList(141L, 1001L, null, 121L, null, null, "warm_up,ongoing", 1, 10);

		assertEquals(1L, ((Number) result.get("total_count")).longValue());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		assertEquals(1, list.size());
		assertEquals("warm_up", list.get(0).get("status"));
		assertEquals("预热中", list.get(0).get("status_desc"));
		assertEquals(166L, ((Number) list.get(0).get("id")).longValue());
	}

	@Test
	@DisplayName("MyBatis camelCase 键也能改写成预热中")
	void warmUp_camelCaseMapKeys_stillRewritten() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", 167L);
		row.put("name", "预热内购");
		row.put("status", "active");
		row.put("displayTime", now - 3600);
		row.put("employeeBeginTime", now + 86400);
		row.put("employeeEndTime", now + 172800);
		row.put("relativeBeginTime", 0);
		row.put("relativeEndTime", 0);
		row.put("isPassphraseEnabled", 0);

		when(userActivitiesQueryMapper.countUserActivities(
						eq(141L), eq(1001L), anyInt(), eq(121L), isNull(), isNull()))
				.thenReturn(1L);
		when(userActivitiesQueryMapper.selectUserActivities(
						eq(141L),
						eq(1001L),
						anyInt(),
						eq(121L),
						isNull(),
						isNull(),
						eq(0L),
						anyInt()))
				.thenReturn(List.of(row));
		when(passphraseVerifiedRedisService.isVerified(anyLong(), anyLong(), anyLong(), anyLong()))
				.thenReturn(false);

		Map<String, Object> result =
				service.getActivityList(141L, 1001L, null, 121L, null, null, "warm_up,ongoing", 1, 10);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		assertEquals(1, list.size());
		assertEquals("warm_up", list.get(0).get("status"));
		assertEquals(now - 3600, ((Number) list.get(0).get("display_time")).intValue());
	}
}
