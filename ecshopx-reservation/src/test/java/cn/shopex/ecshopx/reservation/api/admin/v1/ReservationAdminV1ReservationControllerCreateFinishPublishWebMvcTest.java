package cn.shopex.ecshopx.reservation.api.admin.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.common.dispatch.ReservationExpireCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ReservationFinishDispatchPublisher;
import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationRoutePermissionPort;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import cn.shopex.ecshopx.reservation.service.ReservationAsyncNotifier;
import cn.shopex.ecshopx.reservation.service.ReservationCheckService;
import cn.shopex.ecshopx.reservation.service.ReservationCreateService;
import cn.shopex.ecshopx.reservation.service.ReservationEveryDayTimePeriodParamValidator;
import cn.shopex.ecshopx.reservation.service.ReservationEveryDayTimePeriodService;
import cn.shopex.ecshopx.reservation.service.ReservationListParamValidator;
import cn.shopex.ecshopx.reservation.service.ReservationListQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationSettingQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP slice for admin {@code POST /api/v1/reservation}: controller validates input, delegates to
 * {@link ReservationCreateService#createReservation}; on successful insert the finish dispatch publisher must be
 * invoked exactly once.
 */
class ReservationAdminV1ReservationControllerCreateFinishPublishWebMvcTest {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final String ADMIN_CREATE_RESERVATION_PATH = "/api/v1/reservation";

	@Test
	void post_apiV1Reservation_create_verifiesFinishDispatchPublisherInvokedOnce() throws Exception {
		ReservationSettingQueryService settingQuery = mock(ReservationSettingQueryService.class);
		ReservationShopDetailPort shopDetailPort = mock(ReservationShopDetailPort.class);
		ReservationCheckService checkService = mock(ReservationCheckService.class);
		ReservationRecordMapper recordMapper = mock(ReservationRecordMapper.class);
		ReservationFinishDispatchPublisher finishPublisher = mock(ReservationFinishDispatchPublisher.class);
		ReservationAsyncNotifier asyncNotifier = mock(ReservationAsyncNotifier.class);
		ReservationExpireCancelDispatchPublisher expireCancelPublisher =
				mock(ReservationExpireCancelDispatchPublisher.class);

		ReservationSetting setting = new ReservationSetting();
		setting.setCompanyId(10L);
		setting.setReservationMode(0);
		setting.setTimeInterval(30);
		setting.setSmsDelay("0");

		when(settingQuery.findByCompanyId(10L)).thenReturn(Optional.of(setting));
		when(shopDetailPort.getShopsDetail(anyLong(), anyLong())).thenReturn(Collections.emptyMap());
		when(recordMapper.insert(any(ReservationRecord.class))).thenReturn(1);
		doNothing().when(asyncNotifier).afterReservationCreated(anyLong(), any(), any());
		doNothing().when(expireCancelPublisher).publishExpireCancelAfterCreate(anyLong(), any());

		ReservationCreateService reservationCreateService = new ReservationCreateService(
				settingQuery,
				shopDetailPort,
				checkService,
				recordMapper,
				finishPublisher,
				asyncNotifier,
				expireCancelPublisher);

		ReservationRoutePermissionPort routePermission = mock(ReservationRoutePermissionPort.class);
		doNothing().when(routePermission).assertReservationCreateAllowed(any());

		ShopOperatorCompanyActivation companyActivation = mock(ShopOperatorCompanyActivation.class);
		doNothing().when(companyActivation).assertShopOperatorCompanyActive(anyLong());

		ShopOperatorLogsWrite logsWrite = mock(ShopOperatorLogsWrite.class);
		doNothing().when(logsWrite).addLogs(any());

		ObjectMapper objectMapper = new ObjectMapper();

		ReservationController controller =
				new ReservationController(
						routePermission,
						companyActivation,
						reservationCreateService,
						mock(ReservationListParamValidator.class),
						mock(ReservationListQueryService.class),
						logsWrite,
						objectMapper,
						mock(ReservationEveryDayTimePeriodParamValidator.class),
						mock(ReservationEveryDayTimePeriodService.class));

		FlexibleBodyMethodArgumentResolver flexibleResolver =
				new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		Map<String, Object> operator = new LinkedHashMap<>();
		operator.put("company_id", 10L);
		operator.put("operator_id", 1L);

		mockMvc
				.perform(
						post(ADMIN_CREATE_RESERVATION_PATH)
								.requestAttr(OPERATOR_JWT_USER_DATA, operator)
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"{\"shopId\":20,\"resourceLevelId\":5,\"dateDay\":\"2026-05-10\","
												+ "\"beginTime\":\"10:00\",\"instead\":\"system\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(true));

		verify(finishPublisher, times(1)).publish(any());
	}

	@Test
	void post_apiV1Reservation_create_verifiesFinishPublishPostdataMatchesAdminApiShape() throws Exception {
		long jwtCompanyId = 10L;
		long expectedUserId = 424242L;

		ReservationSettingQueryService settingQuery = mock(ReservationSettingQueryService.class);
		ReservationShopDetailPort shopDetailPort = mock(ReservationShopDetailPort.class);
		ReservationCheckService checkService = mock(ReservationCheckService.class);
		ReservationRecordMapper recordMapper = mock(ReservationRecordMapper.class);
		ReservationFinishDispatchPublisher finishPublisher = mock(ReservationFinishDispatchPublisher.class);
		ReservationAsyncNotifier asyncNotifier = mock(ReservationAsyncNotifier.class);
		ReservationExpireCancelDispatchPublisher expireCancelPublisher =
				mock(ReservationExpireCancelDispatchPublisher.class);

		ReservationSetting setting = new ReservationSetting();
		setting.setCompanyId(jwtCompanyId);
		setting.setReservationMode(0);
		setting.setTimeInterval(30);
		setting.setSmsDelay("0");

		when(settingQuery.findByCompanyId(jwtCompanyId)).thenReturn(Optional.of(setting));
		when(shopDetailPort.getShopsDetail(anyLong(), anyLong())).thenReturn(Collections.emptyMap());
		when(recordMapper.insert(any(ReservationRecord.class))).thenReturn(1);
		doNothing().when(asyncNotifier).afterReservationCreated(anyLong(), any(), any());
		doNothing().when(expireCancelPublisher).publishExpireCancelAfterCreate(anyLong(), any());

		ReservationCreateService reservationCreateService = new ReservationCreateService(
				settingQuery,
				shopDetailPort,
				checkService,
				recordMapper,
				finishPublisher,
				asyncNotifier,
				expireCancelPublisher);

		ReservationRoutePermissionPort routePermission = mock(ReservationRoutePermissionPort.class);
		doNothing().when(routePermission).assertReservationCreateAllowed(any());

		ShopOperatorCompanyActivation companyActivation = mock(ShopOperatorCompanyActivation.class);
		doNothing().when(companyActivation).assertShopOperatorCompanyActive(anyLong());

		ShopOperatorLogsWrite logsWrite = mock(ShopOperatorLogsWrite.class);
		doNothing().when(logsWrite).addLogs(any());

		ObjectMapper objectMapper = new ObjectMapper();

		ReservationController controller =
				new ReservationController(
						routePermission,
						companyActivation,
						reservationCreateService,
						mock(ReservationListParamValidator.class),
						mock(ReservationListQueryService.class),
						logsWrite,
						objectMapper,
						mock(ReservationEveryDayTimePeriodParamValidator.class),
						mock(ReservationEveryDayTimePeriodService.class));

		FlexibleBodyMethodArgumentResolver flexibleResolver =
				new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		Map<String, Object> operator = new LinkedHashMap<>();
		operator.put("company_id", jwtCompanyId);
		operator.put("operator_id", 1L);

		mockMvc
				.perform(
						post(ADMIN_CREATE_RESERVATION_PATH)
								.requestAttr(OPERATOR_JWT_USER_DATA, operator)
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"{\"shopId\":20,\"resourceLevelId\":5,\"dateDay\":\"2026-05-10\","
												+ "\"beginTime\":\"10:00\",\"instead\":\"system\","
												+ "\"userId\":"
												+ expectedUserId
												+ "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(true));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> entitiesCaptor = ArgumentCaptor.forClass(Map.class);
		verify(finishPublisher, times(1)).publish(entitiesCaptor.capture());
		Map<String, Object> entities = entitiesCaptor.getValue();

		assertTrue(entities.containsKey("postdata"));
		assertTrue(entities.containsKey("result"));
		assertTrue(entities.containsKey("setting_data"));

		assertInstanceOf(Map.class, entities.get("postdata"));
		@SuppressWarnings("unchecked")
		Map<String, Object> postdata = (Map<String, Object>) entities.get("postdata");

		assertEquals(jwtCompanyId, ((Number) postdata.get("company_id")).longValue());
		assertEquals(expectedUserId, ((Number) postdata.get("user_id")).longValue());

		Object openId = postdata.get("open_id");
		assertTrue(openId == null || openId.toString().trim().isEmpty());
		Object wxappAppid = postdata.get("wxapp_appid");
		assertTrue(wxappAppid == null || wxappAppid.toString().trim().isEmpty());
	}
}
