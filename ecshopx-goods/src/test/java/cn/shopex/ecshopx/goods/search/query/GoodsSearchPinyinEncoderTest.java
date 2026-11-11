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

package cn.shopex.ecshopx.goods.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoodsSearchPinyinEncoderTest {

	@Test
	void fullPinyin_andFirstLetters_forChineseName() {
		assertThat(GoodsSearchPinyinEncoder.fullPinyin("山岳户外")).isEqualTo("shanyuehuwai");
		assertThat(GoodsSearchPinyinEncoder.firstLetters("山岳户外")).isEqualTo("syhw");
	}

	@Test
	void keepsAsciiLettersAndDigits() {
		assertThat(GoodsSearchPinyinEncoder.fullPinyin("GTX悠途")).isEqualTo("gtxyoutu");
		assertThat(GoodsSearchPinyinEncoder.firstLetters("GTX悠途")).isEqualTo("gtxyt");
	}

	@Test
	void normalizeQuery_stripsSpacesAndNonAlnum() {
		assertThat(GoodsSearchPinyinEncoder.normalizeQuery(" Shan Yue ")).isEqualTo("shanyue");
		assertThat(GoodsSearchPinyinEncoder.normalizeQuery("SY-HW")).isEqualTo("syhw");
	}

	@Test
	void emptyInput_returnsEmpty() {
		assertThat(GoodsSearchPinyinEncoder.fullPinyin(null)).isEmpty();
		assertThat(GoodsSearchPinyinEncoder.firstLetters("")).isEmpty();
		assertThat(GoodsSearchPinyinEncoder.normalizeQuery("   ")).isEmpty();
	}
}
