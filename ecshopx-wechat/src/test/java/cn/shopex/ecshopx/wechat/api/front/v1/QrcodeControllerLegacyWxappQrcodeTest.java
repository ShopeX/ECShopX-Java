package cn.shopex.ecshopx.wechat.api.front.v1;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.wechat.service.PcLoginQrcodeService;
import cn.shopex.ecshopx.wechat.service.PcQrcodeService;
import cn.shopex.ecshopx.wechat.service.WxappLegacyQrcodePngService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class QrcodeControllerLegacyWxappQrcodeTest {

	@Mock
	private PcLoginQrcodeService pcLoginQrcodeService;

	@Mock
	private PcQrcodeService pcQrcodeService;

	@Mock
	private WxappLegacyQrcodePngService wxappLegacyQrcodePngService;

	@InjectMocks
	private QrcodeController controller;

	@Test
	void getLegacyWxappQrcodePng_delegatesToLegacyService() {
		MockHttpServletRequest request =
				new MockHttpServletRequest(
						"GET", "/api/v1/h5app/wxapp/wechatAuth/wxapp/qrcode.png");
		request.addParameter("company_id", "1");
		request.addParameter("appid", "wxApp");
		byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47};
		when(wxappLegacyQrcodePngService.generateQrcodePng(request)).thenReturn(png);

		ResponseEntity<byte[]> response = controller.getLegacyWxappQrcodePng(request);

		assertArrayEquals(png, response.getBody());
		org.junit.jupiter.api.Assertions.assertEquals(
				MediaType.IMAGE_PNG, response.getHeaders().getContentType());
		verify(wxappLegacyQrcodePngService).generateQrcodePng(request);
	}
}
