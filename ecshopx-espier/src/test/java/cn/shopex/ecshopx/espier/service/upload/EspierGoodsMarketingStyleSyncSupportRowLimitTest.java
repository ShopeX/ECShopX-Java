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

package cn.shopex.ecshopx.espier.service.upload;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.service.upload.EspierGoodsMarketingStyleSyncSupport.PriceMode;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

/** ECX-10949：营销样式同步超过 500 行时沿用原有拒绝文案。 */
class EspierGoodsMarketingStyleSyncSupportRowLimitTest {

	@Test
	void moreThan500DataRows_usesExistingMessage() throws Exception {
		MultipartFile file = mock(MultipartFile.class);
		when(file.getBytes()).thenReturn(buildXlsx(501));
		UploadHeaderTitle title = new UploadHeaderTitle(Map.of("SKU", "item_bn"), Map.of());

		assertThatThrownBy(
						() ->
								EspierGoodsMarketingStyleSyncSupport.parseSync(
										file, title, PriceMode.MARKETING, 1L, 0L, null))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("每次最多上传500个商品...请减少后再提交");
	}

	private static byte[] buildXlsx(int dataRows) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			var sheet = wb.createSheet();
			var header = sheet.createRow(0);
			header.createCell(0).setCellValue("SKU");
			for (int i = 0; i < dataRows; i++) {
				var row = sheet.createRow(i + 1);
				row.createCell(0).setCellValue("bn" + i);
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			return bos.toByteArray();
		}
	}
}
