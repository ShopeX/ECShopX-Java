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

package cn.shopex.ecshopx.goods.service.espier;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.espier.service.upload.EspierUploadHeaderCatalog;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import java.util.List;
import org.junit.jupiter.api.Test;

class NormalGoodsUploadTemplateDemoRowsTest {

	@Test
	void forHeader_emitsNineRowsMatchingColumnWidth() {
		UploadHeaderTitle title = EspierUploadHeaderCatalog.NORMAL_GOODS();
		List<List<Object>> rows = NormalGoodsUploadTemplateDemoRows.forHeader(title);
		assertThat(rows).hasSize(9);
		int width = title.all().size();
		for (List<Object> row : rows) {
			assertThat(row).hasSize(width);
		}
		assertThat(rows.get(0).get(0)).isEqualTo("HP->上衣->HP");
		assertThat(String.valueOf(rows.get(8).get(1))).isEmpty();
		assertThat(String.valueOf(rows.get(8).get(4))).contains("多规格商品");
	}
}
