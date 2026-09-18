package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePurchaseActivityItemsPriceOverlayServiceTest {

	@Mock ActivityItemsMapper activityItemsMapper;

	@InjectMocks EmployeePurchaseActivityItemsPriceOverlayService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), ActivityItems.class);
	}

	@Test
	void overlay_skipsWhenActivityIdZero() {
		List<Map<String, Object>> rows = List.of(row(10L, 100));
		service.overlayActivityPrice(rows, 141L, 0L);
		verify(activityItemsMapper, never()).selectList(any());
		assertEquals(100, rows.get(0).get("activity_price"));
	}

	@Test
	void overlay_writesPriceAndStoreForMatchingOnShelfItem() {
		ActivityItems ai = new ActivityItems();
		ai.setItemId(10L);
		ai.setActivityPrice(2100);
		ai.setActivityStore(8);
		when(activityItemsMapper.selectList(any())).thenReturn(List.of(ai));

		Map<String, Object> matched = row(10L, 9900);
		Map<String, Object> other = row(11L, 8800);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(matched);
		rows.add(other);

		service.overlayActivityPrice(rows, 141L, 166L);

		assertEquals(2100, matched.get("activity_price"));
		assertEquals(8, matched.get("activity_store"));
		assertEquals(8800, other.get("activity_price"));
		assertFalse(other.containsKey("activity_store"));
	}

	@Test
	void overlay_nullPriceBecomesZero() {
		ActivityItems ai = new ActivityItems();
		ai.setItemId(10L);
		ai.setActivityPrice(null);
		ai.setActivityStore(null);
		when(activityItemsMapper.selectList(any())).thenReturn(List.of(ai));

		Map<String, Object> matched = row(10L, 9900);
		service.overlayActivityPrice(List.of(matched), 141L, 166L);

		assertEquals(0, matched.get("activity_price"));
		assertEquals(0, matched.get("activity_store"));
	}

	private static Map<String, Object> row(long itemId, int existingPrice) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", itemId);
		m.put("activity_price", existingPrice);
		return m;
	}
}
