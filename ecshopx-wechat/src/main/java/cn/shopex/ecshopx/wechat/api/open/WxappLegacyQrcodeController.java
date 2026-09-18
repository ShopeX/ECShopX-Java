/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.wechat.api.open;

import cn.shopex.ecshopx.wechat.service.WxappLegacyQrcodePngService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 对齐 PHP {@code GET /wechatAuth/wxapp/qrcode.png}（web.php）。 */
@RestController
@RequestMapping("/wechatAuth/wxapp")
public class WxappLegacyQrcodeController {

	private final WxappLegacyQrcodePngService wxappLegacyQrcodePngService;

	public WxappLegacyQrcodeController(WxappLegacyQrcodePngService wxappLegacyQrcodePngService) {
		this.wxappLegacyQrcodePngService = wxappLegacyQrcodePngService;
	}

	@GetMapping(value = "/qrcode.png", name = "获取小程序码PNG", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getQrcodePng(HttpServletRequest request) {
		byte[] png = wxappLegacyQrcodePngService.generateQrcodePng(request);
		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
	}
}
