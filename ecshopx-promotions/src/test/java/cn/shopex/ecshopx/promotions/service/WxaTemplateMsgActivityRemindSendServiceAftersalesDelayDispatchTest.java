package cn.shopex.ecshopx.promotions.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.promotions.port.WxopenTemplateSendDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.WxaNoticeTemplate;
import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WxaTemplateMsgActivityRemindSendServiceAftersalesDelayDispatchTest {

	@Test
	void whenAftersalesSuccess_andForceFireFalse_andTemplateDelayConfigured_thenDispatchesWxopenJobWithDelay() {
		WxaNoticeTemplateMapper noticeMapper = Mockito.mock(WxaNoticeTemplateMapper.class);
		WeappMapper weappMapper = Mockito.mock(WeappMapper.class);
		WxJavaMaRuntime wxJavaMaRuntime = Mockito.mock(WxJavaMaRuntime.class);
		ObjectMapper objectMapper = new ObjectMapper();
		WxopenTemplateSendDispatchPublisher publisher = Mockito.mock(WxopenTemplateSendDispatchPublisher.class);

		Weapp weapp = new Weapp();
		weapp.setCompanyId(1L);
		weapp.setAuthorizerAppid("wx-app-authorizer");
		weapp.setTemplateName("yykweishop");
		when(weappMapper.selectOne(any())).thenReturn(weapp);

		WxaNoticeTemplate notice = new WxaNoticeTemplate();
		notice.setIsOpen(true);
		notice.setTemplateId("pri-template-1");
		notice.setSendTimeDesc("{\"value\":5}");
		notice.setContent("[]");
		when(noticeMapper.selectOne(any())).thenReturn(notice);

		WxaTemplateMsgActivityRemindSendService service =
				new WxaTemplateMsgActivityRemindSendService(
						noticeMapper, weappMapper, wxJavaMaRuntime, objectMapper, publisher);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("order_id", "1001");
		data.put("refund_fee", "9.99元");
		data.put("remarks", "您的售后已审核成功，请填写回寄物流！");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("scenes_name", "aftersalesSuccess");
		payload.put("appid", "wx-app-authorizer");
		payload.put("openid", "o-open-id");
		payload.put("data", data);

		service.send(payload, false);

		verify(publisher)
				.publish(
						argThat(
								m -> {
									if (!"aftersalesSuccess".equals(m.get("scenes_name"))) {
										return false;
									}
									if (!Long.valueOf(1L).equals(toLong(m.get("company_id")))) {
										return false;
									}
									Object d = m.get("data");
									if (!(d instanceof Map<?, ?> dm)) {
										return false;
									}
									return "1001".equals(dm.get("order_id"))
											&& "9.99元".equals(dm.get("refund_fee"))
											&& "您的售后已审核成功，请填写回寄物流！".equals(dm.get("remarks"));
								}),
						eq(true),
						eq(Duration.ofMinutes(5)));
	}

	private static Long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}
}
