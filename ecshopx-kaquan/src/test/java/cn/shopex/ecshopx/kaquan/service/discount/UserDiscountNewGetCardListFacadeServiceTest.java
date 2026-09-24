package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountNewGetCardListRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDiscountNewGetCardListFacadeServiceTest {

	@Mock
	private UserDiscountValidItemsResolver userDiscountValidItemsResolver;

	@Mock
	private UserDiscountNewCardListService userDiscountNewCardListService;

	@Mock
	private UserDiscountCardValidityEvaluator userDiscountCardValidityEvaluator;

	@Mock
	private UserDiscountNewGetCardListResponseFormatter responseFormatter;

	@Mock
	private CompanyDefaultCurrencyService companyDefaultCurrencyService;

	private UserDiscountNewGetCardListFacadeService facade;

	@BeforeEach
	void setUp() {
		facade = new UserDiscountNewGetCardListFacadeService(
				userDiscountValidItemsResolver,
				userDiscountNewCardListService,
				userDiscountCardValidityEvaluator,
				responseFormatter,
				companyDefaultCurrencyService);
		when(responseFormatter.formatList(any())).thenAnswer(inv -> inv.getArgument(0));
		when(responseFormatter.formatTotalCount(anyLong())).thenReturn("0");
		when(responseFormatter.formatCur(any())).thenReturn(Map.of());
		when(companyDefaultCurrencyService.getCur(anyLong())).thenReturn(null);
		when(companyDefaultCurrencyService.toCurResponseMap(any())).thenReturn(Map.of());
	}

	@Test
	@DisplayName("valid=true 时门槛不满足的指定标签券不出现在列表中")
	void usablePickerHidesInvalidTagCoupon() {
		Map<Long, Map<String, Object>> items = new LinkedHashMap<>();
		items.put(2614L, Map.of("item_id", 2614L, "total_fee", 1390L));
		items.put(2446L, Map.of("item_id", 2446L, "total_fee", 12000L));
		when(userDiscountValidItemsResolver.resolve(anyLong(), anyLong(), any())).thenReturn(items);

		Map<String, Object> card = new HashMap<>();
		card.put("code", "G3T2JDN97Z84");
		card.put("valid", true);
		Map<String, Object> page = new HashMap<>();
		page.put("total_count", 1);
		page.put("list", new ArrayList<>(List.of(card)));
		when(userDiscountNewCardListService.loadPage(any(), anyInt(), anyInt())).thenReturn(page);
		doAnswer(inv -> {
			List<Map<String, Object>> list = inv.getArgument(4);
			list.get(0).put("valid", false);
			return null;
		}).when(userDiscountCardValidityEvaluator).evaluate(anyLong(), anyLong(), any(), any(), any());

		UserDiscountNewGetCardListRequest req = new UserDiscountNewGetCardListRequest(
				null, null, null, null, null, null, 1, 10, "mall", "picker", null, "0",
				"true", "cart", null, null, "0", null, "true");
		Map<String, Object> body = facade.build(1L, 45097L, req);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("list");
		assertThat(list).isEmpty();
		assertThat(body.get("total_count")).isEqualTo("0");
	}
}
