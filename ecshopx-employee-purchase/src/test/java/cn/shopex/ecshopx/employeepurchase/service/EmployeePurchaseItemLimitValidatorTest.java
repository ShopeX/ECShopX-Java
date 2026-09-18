package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator.ItemAggregate;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator.ItemLimit;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator.LimitLine;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmployeePurchaseItemLimitValidatorTest {

	private static ItemLimit activityRow(int limitNum, int limitFee) {
		return new ItemLimit(limitNum, limitFee);
	}

	@Test
	void allowsWithinLimits() {
		assertDoesNotThrow(
				() ->
						EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
								Map.of(1L, activityRow(10, 100000)),
								Map.of(),
								List.of(new LimitLine(1L, 1, 100))));
	}

	@Test
	void quantityOnlyUsesNumMessage() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
										Map.of(1L, activityRow(1, 100000)),
										Map.of(),
										List.of(new LimitLine(1L, 2, 200))));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	@Test
	void feeOnlyUsesFeeMessage() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
										Map.of(1L, activityRow(100, 100)),
										Map.of(),
										List.of(new LimitLine(1L, 1, 200))));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_FEE, ex.getMessage());
	}

	@Test
	void bothViolationsPreferFeeMessage() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
										Map.of(1L, activityRow(1, 100)),
										Map.of(),
										List.of(new LimitLine(1L, 2, 500))));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_FEE, ex.getMessage());
	}

	@Test
	void includesHistoryAggregate() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
										Map.of(1L, activityRow(3, 0)),
										Map.of(1L, new ItemAggregate(2, 0)),
										List.of(new LimitLine(1L, 2, 0))));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	@Test
	void multiSkuSecondLineFailsQuantity() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
										Map.of(1L, activityRow(10, 100000), 2L, activityRow(1, 100000)),
										Map.of(),
										List.of(new LimitLine(1L, 1, 100), new LimitLine(2L, 2, 200))));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	@Test
	void fixedMessagesDoNotContainItemName() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
										Map.of(9L, activityRow(1, 100000)),
										Map.of(),
										List.of(new LimitLine(9L, 5, 500))));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	@Test
	void missingActivityItemThrowsNotInActivity() {
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
										Map.of(), Map.of(), List.of(new LimitLine(1L, 1, 100))));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NOT_IN_ACTIVITY, ex.getMessage());
	}
}
