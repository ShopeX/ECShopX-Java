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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class ExchangeCardBarcodeImageService {

	public byte[] barcodePngBytes(String content) {
		try {
			Map<EncodeHintType, Object> hints = new HashMap<>();
			hints.put(EncodeHintType.MARGIN, 0);
			BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.CODE_93, 360, 70, hints);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			return baos.toByteArray();
		} catch (Exception e) {
			throw new ResourceException("兑换券信息生成失败");
		}
	}

	public byte[] qrcodePngBytes(String content) {
		try {
			Map<EncodeHintType, Object> hints = new HashMap<>();
			hints.put(EncodeHintType.MARGIN, 0);
			BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 120, 120, hints);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			return baos.toByteArray();
		} catch (Exception e) {
			throw new ResourceException("兑换券信息生成失败");
		}
	}

	/** SWEEP 场景条码，尺寸与 {@link #barcodePngBytes} 一致。 */
	public byte[] sweepBarcodePngBytes(String content) {
		return barcodePngBytes(content);
	}

	/** SWEEP 场景二维码，尺寸与 {@link #qrcodePngBytes} 一致。 */
	public byte[] sweepQrcodePngBytes(String content) {
		return qrcodePngBytes(content);
	}
}
