package cn.shopex.ecshopx.reservation.api.front.v1;

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
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import cn.shopex.ecshopx.reservation.service.ReservationAsyncNotifier;
import cn.shopex.ecshopx.reservation.service.ReservationCheckService;
import cn.shopex.ecshopx.reservation.service.ReservationCreateService;
import cn.shopex.ecshopx.reservation.service.ReservationDateDayQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationSettingQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappGetRecordCountService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappLimitCheckService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappRecordListQueryService;
import cn.shopex.ecshopx.reservation.service.ReservationWxappTimelistQueryService;
import cn.shopex.ecshopx.reservation.service.WxappCanReservationRightsParamValidator;
import cn.shopex.ecshopx.reservation.service.WxappCanReservationRightsQueryService;
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
 * HTTP slice for wxapp {@code POST /api/v1/wxapp/reservation}: controller builds post data, runs limit check,
 * delegates to {@link ReservationCreateService#createReservation}; on successful insert the finish dispatch publisher
 * must be invoked exactly once.
 */
class ReservationFrontV1ReservationControllerCreateReservationFinishPublishWebMvcTest {

	private static final String WXAPP_CREATE_RESERVATION_PATH = "/api/v1/wxapp/reservation";

	@Test
	void post_apiV1WxappReservation_createReservation_verifiesFinishDispatchPublisherInvokedOnce() throws Exception {
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

		ReservationWxappLimitCheckService limitCheck = mock(ReservationWxappLimitCheckService.class);
		doNothing().when(limitCheck).checkLimit(any());

		ReservationController controller =
				new ReservationController(
						reservationCreateService,
						limitCheck,
						mock(WxappCanReservationRightsParamValidator.class),
						mock(WxappCanReservationRightsQueryService.class),
						mock(ReservationDateDayQueryService.class),
						mock(ReservationWxappGetRecordCountService.class),
						mock(ReservationWxappRecordListQueryService.class),
						mock(ReservationWxappTimelistQueryService.class));

		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver =
				new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 10L);
		auth.put("username", "member-a");
		auth.put("mobile", "13800000000");

		mockMvc
				.perform(
						post(WXAPP_CREATE_RESERVATION_PATH)
								.requestAttr(WxappMemberAuthAttributes.REQUEST_ATTR, auth)
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"{\"shopId\":20,\"shopName\":\"门店\",\"beginTime\":\"10:00\","
												+ "\"dateDay\":\"2026-05-10\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(true));

		verify(finishPublisher, times(1)).publish(any());
	}

	@Test
	void post_apiV1WxappReservation_createReservation_verifiesFinishPublishPayloadIncludesWxappIdentityFromAuth()
			throws Exception {
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

		ReservationWxappLimitCheckService limitCheck = mock(ReservationWxappLimitCheckService.class);
		doNothing().when(limitCheck).checkLimit(any());

		ReservationController controller =
				new ReservationController(
						reservationCreateService,
						limitCheck,
						mock(WxappCanReservationRightsParamValidator.class),
						mock(WxappCanReservationRightsQueryService.class),
						mock(ReservationDateDayQueryService.class),
						mock(ReservationWxappGetRecordCountService.class),
						mock(ReservationWxappRecordListQueryService.class),
						mock(ReservationWxappTimelistQueryService.class));

		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver =
				new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		String expectedOpenId = "wx-open-entry02-test";
		String expectedWxappAppId = "wxapp-appid-entry02";
		long expectedUserId = 424242L;

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 10L);
		auth.put("username", "member-a");
		auth.put("mobile", "13800000000");
		auth.put("user_id", expectedUserId);
		auth.put("open_id", expectedOpenId);
		auth.put("wxapp_appid", expectedWxappAppId);

		mockMvc
				.perform(
						post(WXAPP_CREATE_RESERVATION_PATH)
								.requestAttr(WxappMemberAuthAttributes.REQUEST_ATTR, auth)
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"{\"shopId\":20,\"shopName\":\"门店\",\"beginTime\":\"10:00\","
												+ "\"dateDay\":\"2026-05-10\"}"))
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

		assertEquals(expectedOpenId, postdata.get("open_id"));
		assertEquals(expectedWxappAppId, postdata.get("wxapp_appid"));
		assertEquals(expectedUserId, ((Number) postdata.get("user_id")).longValue());
	}
}
