package cn.shopex.ecshopx.wechat.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.wechat.openapi.OpenapiWxappShareIdRedisService;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class WxappLegacyQrcodePngServiceTest {

	@Mock
	private WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;

	@Mock
	private OpenapiWxappShareIdRedisService openapiWxappShareIdRedisService;

	@Mock
	private WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	@InjectMocks
	private WxappLegacyQrcodePngService service;

	@Test
	void employeePurchaseShareLand_storesSceneParamsAndUsesShareIdScene() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wechatAuth/wxapp/qrcode.png");
		request.addParameter("company_id", "38");
		request.addParameter("cid", "38");
		request.addParameter("temp_name", "yykweishop");
		request.addParameter("page", "pages/share-land");
		request.addParameter("id", "156");
		request.addParameter("enterprise_id", "128");
		request.addParameter("appid", "wx1e25e45145b70faa");
		request.addParameter("from_scene", "poster_purchase_auth");
		request.addParameter("ppe", "1");

		byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47};
		when(openapiWxappShareIdRedisService.getShareId(eq(38L), any())).thenReturn("abc123");
		when(wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(
						"wx1e25e45145b70faa", "share_id=abc123", "pages/share-land"))
				.thenReturn(png);

		byte[] result = service.generateQrcodePng(request);

		assertArrayEquals(png, result);

		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(openapiWxappShareIdRedisService).getShareId(eq(38L), paramsCaptor.capture());
		LinkedHashMap<String, Object> stored = new LinkedHashMap<>(paramsCaptor.getValue());
		org.junit.jupiter.api.Assertions.assertEquals("38", stored.get("cid"));
		org.junit.jupiter.api.Assertions.assertEquals("156", stored.get("id"));
		org.junit.jupiter.api.Assertions.assertEquals("128", stored.get("enterprise_id"));
		org.junit.jupiter.api.Assertions.assertEquals("poster_purchase_auth", stored.get("from_scene"));
		org.junit.jupiter.api.Assertions.assertEquals("1", stored.get("ppe"));
		org.junit.jupiter.api.Assertions.assertFalse(stored.containsKey("page"));
		org.junit.jupiter.api.Assertions.assertFalse(stored.containsKey("company_id"));
	}

	@Test
	void resolvesAppidFromTemplateWhenMissing() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wechatAuth/wxapp/qrcode.png");
		request.addParameter("company_id", "38");
		request.addParameter("temp_name", "yykweishop");
		request.addParameter("page", "pages/index");

		when(weappAuthorizerAppidRepository.findAuthorizerAppid(38L, "yykweishop"))
				.thenReturn(java.util.Optional.of("wxResolved"));
		when(openapiWxappShareIdRedisService.getShareId(eq(38L), any())).thenReturn("sid1");
		when(wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes("wxResolved", "share_id=sid1", "pages/index"))
				.thenReturn(new byte[] {1});

		service.generateQrcodePng(request);

		verify(weappAuthorizerAppidRepository).findAuthorizerAppid(38L, "yykweishop");
	}
}
