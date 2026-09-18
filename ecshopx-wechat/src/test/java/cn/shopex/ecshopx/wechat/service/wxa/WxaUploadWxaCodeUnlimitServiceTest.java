package cn.shopex.ecshopx.wechat.service.wxa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * PHP {@code Wxa::uploadWxaCodeUnlimit} returns {@code ['base64Image' => 'data:image/jpg;base64,' . base64_encode($binary)]};
 * Java must match the same envelope and prefix (see {@link WxaUploadWxaCodeUnlimitService}).
 */
@ExtendWith(MockitoExtension.class)
class WxaUploadWxaCodeUnlimitServiceTest {

	@Mock
	private WechatAuthQueryService wechatAuthQueryService;

	@Mock
	private WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	@InjectMocks
	private WxaUploadWxaCodeUnlimitService service;

	@Test
	void responseMatchesPhpShape_defaultSceneAndPage() {
		byte[] wxBytes = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47};
		when(wechatAuthQueryService.isMiniProgramWxaBoundToCompany(38L, "wxTestApp")).thenReturn(true);
		when(wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes("wxTestApp", "1", "pages/index")).thenReturn(wxBytes);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setQueryString("wxaAppId=wxTestApp");
		request.addParameter("wxaAppId", "wxTestApp");

		Map<String, Object> data = service.uploadWxaCodeUnlimit(38L, "", request);

		String expected =
				"data:image/jpg;base64," + Base64.getEncoder().encodeToString(wxBytes);
		assertEquals(expected, data.get("base64Image"));
		assertEquals(1, data.size());
		verify(wxaUnlimitedQrcodeClient).getUnlimitedCodeBytes("wxTestApp", "1", "pages/index");
	}

	@Test
	void responseMatchesPhpShape_distributorIdMapsToDidInScene() {
		byte[] wxBytes = "PNG".getBytes(StandardCharsets.UTF_8);
		when(wechatAuthQueryService.isMiniProgramWxaBoundToCompany(1L, "wxA")).thenReturn(true);
		when(wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(eq("wxA"), eq("did=294"), eq("pages/index")))
				.thenReturn(wxBytes);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setQueryString("wxaAppId=wxA&distributor_id=294");
		request.addParameter("wxaAppId", "wxA");
		request.addParameter("distributor_id", "294");

		Map<String, Object> data = service.uploadWxaCodeUnlimit(1L, "", request);

		assertEquals(
				"data:image/jpg;base64," + Base64.getEncoder().encodeToString(wxBytes), data.get("base64Image"));
		verify(wxaUnlimitedQrcodeClient).getUnlimitedCodeBytes("wxA", "did=294", "pages/index");
	}
}
