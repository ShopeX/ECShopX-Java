package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.domain.Cart;
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityItemsAggregate;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.CartMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityItemsAggregateMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePurchaseCartItemLimitServiceTest {

	@Mock CartMapper cartMapper;
	@Mock ActivityItemsMapper activityItemsMapper;
	@Mock MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper;

	@InjectMocks EmployeePurchaseCartItemLimitService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ActivityItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Cart.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), MemberActivityItemsAggregate.class);
	}

	@Test
	void assertFastBuy_whenNumExceedsLimit_throwsNumMessage() {
		ActivityItems ai = activityItem(1L, 100, 1, 0);
		when(activityItemsMapper.selectOne(any())).thenReturn(ai);
		when(memberActivityItemsAggregateMapper.selectList(any())).thenReturn(List.of());

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() -> service.assertFastBuyIntent(1L, 10L, 20L, 30L, 1L, 2));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	@Test
	void assertFastBuy_whenFeeExceedsLimit_throwsFeeMessage() {
		ActivityItems ai = activityItem(1L, 200, 10, 100);
		when(activityItemsMapper.selectOne(any())).thenReturn(ai);
		when(memberActivityItemsAggregateMapper.selectList(any())).thenReturn(List.of());

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() -> service.assertFastBuyIntent(1L, 10L, 20L, 30L, 1L, 1));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_FEE, ex.getMessage());
	}

	@Test
	void assertDbCart_whenUnchecked_skipsValidation() {
		assertDoesNotThrow(() -> service.assertDbCartIntent(1L, 10L, 20L, 30L, 1L, 99, false));
	}

	@Test
	void assertDbCart_whenCheckedNumExceeds_throwsNumMessage() {
		ActivityItems ai = activityItem(5L, 100, 1, 0);
		when(cartMapper.selectList(any())).thenReturn(List.of());
		when(activityItemsMapper.selectList(any())).thenReturn(List.of(ai));
		when(memberActivityItemsAggregateMapper.selectList(any())).thenReturn(List.of());

		ResourceException ex =
				assertThrows(
						ResourceException.class, () -> service.assertDbCartIntent(1L, 10L, 20L, 30L, 5L, 2, true));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	@Test
	void assertDbCart_includesExistingCheckedLinesAndHistory() {
		Cart existing = new Cart();
		existing.setCartId(8L);
		existing.setItemId(5L);
		existing.setNum(1L);
		existing.setIsChecked(true);
		ActivityItems ai = activityItem(5L, 100, 3, 0);
		MemberActivityItemsAggregate agg = new MemberActivityItemsAggregate();
		agg.setItemId(5L);
		agg.setAggregateNum(2);
		agg.setAggregateFee(0);
		when(cartMapper.selectList(any())).thenReturn(List.of(existing));
		when(activityItemsMapper.selectList(any())).thenReturn(List.of(ai));
		when(memberActivityItemsAggregateMapper.selectList(any())).thenReturn(List.of(agg));

		ResourceException ex =
				assertThrows(
						ResourceException.class, () -> service.assertDbCartIntent(1L, 10L, 20L, 30L, 5L, 2, true));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	private static ActivityItems activityItem(long itemId, int price, int limitNum, int limitFee) {
		ActivityItems ai = new ActivityItems();
		ai.setItemId(itemId);
		ai.setActivityPrice(price);
		ai.setLimitNum(limitNum);
		ai.setLimitFee(limitFee);
		ai.setShelfStatus(1);
		return ai;
	}
}
