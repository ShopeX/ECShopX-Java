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

package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.BargainPromotionMultiLangWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * ECX-11445: create/update must persist request {@code min_price} (yuan → fen), not hardcoded 100.
 */
@ExtendWith(MockitoExtension.class)
class BargainPromotionCreateServiceMinPriceTest {

	@Mock private MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	@Mock private MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	@Mock private BargainPromotionsMapper bargainPromotionsMapper;
	@Mock private UserBargainsMapper userBargainsMapper;
	@Mock private BargainPromotionMultiLangWriteService bargainPromotionMultiLangWriteService;
	@Mock private MessageSource messageSource;

	private BargainPromotionCreateService service;

	@BeforeEach
	void setUp() {
		LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
		service =
				new BargainPromotionCreateService(
						marketingActivityCrossPromotionGuardService,
						marketingActivityCatalogAccess,
						bargainPromotionsMapper,
						userBargainsMapper,
						bargainPromotionMultiLangWriteService,
						messageSource,
						new ObjectMapper());
		lenient()
				.when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		lenient().when(marketingActivityCatalogAccess.anyGiftItem(anyLong(), any())).thenReturn(false);
	}

	@Test
	@DisplayName("createBargain persists min_price 5 yuan as 500 fen")
	void createBargain_readsMinPriceFromRequest() {
		doAnswer(
						inv -> {
							BargainPromotions row = inv.getArgument(0);
							row.setBargainId(77L);
							return 1;
						})
				.when(bargainPromotionsMapper)
				.insert(any(BargainPromotions.class));

		Map<String, Object> params = validCreateParams();
		params.put("min_price", "5");

		service.createBargain(params, "zh-CN");

		ArgumentCaptor<BargainPromotions> captor = ArgumentCaptor.forClass(BargainPromotions.class);
		verify(bargainPromotionsMapper).insert(captor.capture());
		assertThat(captor.getValue().getMinPrice()).isEqualTo(500);
	}

	@Test
	@DisplayName("createBargain rejects min_price greater than price")
	void createBargain_minPriceAbovePrice_throws() {
		Map<String, Object> params = validCreateParams();
		params.put("price", "10");
		params.put("mkt_price", "100");
		params.put("min_price", "20");

		assertThatThrownBy(() -> service.createBargain(params, "zh-CN"))
				.isInstanceOf(BadRequestException.class);
	}

	private static Map<String, Object> validCreateParams() {
		long now = Instant.now().getEpochSecond();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 141L);
		params.put("item_id", "1001");
		params.put("title", "砍价");
		params.put("ad_pic", "http://a/p.png");
		params.put("item_name", "sku");
		params.put("item_pics", "http://a/i.png");
		params.put("price", "10");
		params.put("mkt_price", "100");
		params.put("min_price", "5");
		params.put("limit_num", 1);
		params.put("bargain_rules", "规则");
		params.put("share_msg", "分享");
		params.put("people_range", Map.of("min", 1, "max", 3));
		params.put("help_pics", List.of("http://a/h.png"));
		params.put("begin_time", now + 60);
		params.put("end_time", now + 3600);
		return params;
	}
}
