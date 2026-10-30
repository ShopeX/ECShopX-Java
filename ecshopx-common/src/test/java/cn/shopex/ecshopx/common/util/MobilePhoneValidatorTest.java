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

package cn.shopex.ecshopx.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MobilePhoneValidatorTest {

	@Test
	@DisplayName("CN 合法号通过")
	void cnMobileOk() {
		assertThat(MobilePhoneValidator.isCnMobile("13800138000")).isTrue();
		assertThat(MobilePhoneValidator.isCnOrEgMobile("13800138000")).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"01012345678", "01112345678", "01212345678", "01512345678"})
	@DisplayName("EG 本地号通过")
	void egLocalOk(String mobile) {
		assertThat(MobilePhoneValidator.isCnOrEgMobile(mobile)).isTrue();
		assertThat(MobilePhoneValidator.isCnMobile(mobile)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"+201012345678", "201012345678", "00201012345678", "+2001012345678"})
	@DisplayName("EG 国际号通过")
	void egIntlOk(String mobile) {
		assertThat(MobilePhoneValidator.isCnOrEgMobile(mobile)).isTrue();
		assertThat(MobilePhoneValidator.isCnMobile(mobile)).isFalse();
	}

	@Test
	@DisplayName("去空格与连字符后再校验")
	void stripsSpacesAndDashes() {
		assertThat(MobilePhoneValidator.isCnMobile("138 0013-8000")).isTrue();
		assertThat(MobilePhoneValidator.isCnOrEgMobile("+20 101-2345678")).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"123", "02012345678", "01312345678", "1380013800", "abc"})
	@DisplayName("非法号拒绝")
	void rejectInvalid(String mobile) {
		assertThat(MobilePhoneValidator.isCnMobile(mobile)).isFalse();
		assertThat(MobilePhoneValidator.isCnOrEgMobile(mobile)).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@DisplayName("空值拒绝")
	void rejectBlank(String mobile) {
		assertThat(MobilePhoneValidator.isCnMobile(mobile)).isFalse();
		assertThat(MobilePhoneValidator.isCnOrEgMobile(mobile)).isFalse();
	}
}
