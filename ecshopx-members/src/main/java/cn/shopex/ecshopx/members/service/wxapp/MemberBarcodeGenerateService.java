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

package cn.shopex.ecshopx.members.service.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class MemberBarcodeGenerateService {

	private static final int CODE_93_WIDTH = 360;

	private static final int CODE_93_HEIGHT = 70;

	private static final int QR_SIZE = 120;

	private static final String DATA_URL_PREFIX = "data:image/jpg;base64,";

	public Map<String, String> generateBarCode(String userCardCode) {
		if (userCardCode == null) {
			throw new ResourceException("会员码生成失败");
		}
		String prefixed = "MC_" + userCardCode;
		try {
			byte[] barcodeBytes = toBarcodePngBytes(prefixed);
			byte[] qrcodeBytes = toQrcodePngBytes(prefixed);
			Map<String, String> out = new LinkedHashMap<>();
			out.put("barcode_url", DATA_URL_PREFIX + Base64.getEncoder().encodeToString(barcodeBytes));
			out.put("qrcode_url", DATA_URL_PREFIX + Base64.getEncoder().encodeToString(qrcodeBytes));
			return out;
		} catch (Exception e) {
			throw new ResourceException("会员码生成失败");
		}
	}

	public LinkedHashMap<String, String> generateBarcodeAndQrcodeDataUrlsForPlainContent(String plainContent) {
		if (plainContent == null) {
			throw new NullPointerException();
		}
		try {
			byte[] barcodeBytes = toBarcodePngBytes(plainContent);
			byte[] qrcodeBytes = toQrcodePngBytes(plainContent);
			LinkedHashMap<String, String> out = new LinkedHashMap<>();
			out.put("barcode_url", DATA_URL_PREFIX + Base64.getEncoder().encodeToString(barcodeBytes));
			out.put("qrcode_url", DATA_URL_PREFIX + Base64.getEncoder().encodeToString(qrcodeBytes));
			return out;
		} catch (Exception e) {
			throw new IllegalStateException("timescard barcode render failed", e);
		}
	}

	private byte[] toBarcodePngBytes(String content) throws Exception {
		Map<EncodeHintType, Object> hints = new HashMap<>();
		hints.put(EncodeHintType.MARGIN, 0);
		BitMatrix matrix =
				new MultiFormatWriter()
						.encode(content, BarcodeFormat.CODE_93, CODE_93_WIDTH, CODE_93_HEIGHT, hints);
		BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		return baos.toByteArray();
	}

	private byte[] toQrcodePngBytes(String content) throws Exception {
		Map<EncodeHintType, Object> hints = new HashMap<>();
		hints.put(EncodeHintType.MARGIN, 0);
		BitMatrix matrix =
				new MultiFormatWriter()
						.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
		BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		return baos.toByteArray();
	}
}
