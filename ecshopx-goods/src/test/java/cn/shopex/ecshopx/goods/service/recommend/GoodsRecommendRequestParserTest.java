package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GoodsRecommendRequestParserTest {

	@Test
	void parseItemIds_dedupesAndSkipsInvalid() {
		List<Long> ids = GoodsRecommendRequestParser.parseItemIds(List.of(1, 2, 2, 0, -1));
		assertEquals(List.of(1L, 2L), ids);
	}

	@Test
	void parseItemIds_parsesCommaSeparatedString() {
		assertEquals(List.of(10L, 20L), GoodsRecommendRequestParser.parseItemIds("10,20,20"));
	}

	@Test
	void parseDistributorId_requiredThrowsWhenMissing() {
		assertThrows(IllegalArgumentException.class, () -> GoodsRecommendRequestParser.parseDistributorId(null, true));
	}

	@Test
	void parseItemIds_invalidStringThrows() {
		assertThrows(IllegalArgumentException.class, () -> GoodsRecommendRequestParser.parseItemIds("abc"));
	}

	@Test
	void parseDistributorId_parsesNumber() {
		assertEquals(5L, GoodsRecommendRequestParser.parseDistributorId(5, true));
		assertEquals(0L, GoodsRecommendRequestParser.parseDistributorId(0, false));
	}

	@Test
	void resolveDistributorIdForMatch_standardMissingOrZero_returnsEmpty() {
		assertTrue(GoodsRecommendRequestParser.resolveDistributorIdForMatch("standard", null).isEmpty());
		assertTrue(GoodsRecommendRequestParser.resolveDistributorIdForMatch("standard", 0).isEmpty());
		assertTrue(GoodsRecommendRequestParser.resolveDistributorIdForMatch("standard", "abc").isEmpty());
	}

	@Test
	void resolveDistributorIdForMatch_standardPositive_returnsId() {
		assertEquals(
				1001L,
				GoodsRecommendRequestParser.resolveDistributorIdForMatch("standard", 1001).getAsLong());
	}

	@Test
	void resolveDistributorIdForMatch_platformMissingOrZero_returnsZero() {
		assertEquals(
				0L, GoodsRecommendRequestParser.resolveDistributorIdForMatch("platform", null).getAsLong());
		assertEquals(
				0L, GoodsRecommendRequestParser.resolveDistributorIdForMatch("platform", 0).getAsLong());
	}

	@Test
	void resolveDistributorIdForMatch_platformPositive_returnsEmpty() {
		assertTrue(GoodsRecommendRequestParser.resolveDistributorIdForMatch("platform", 1001).isEmpty());
	}

	@Test
	void resolveDistributorIdForCheckoutAdd_standardRequiresPositive() {
		assertFalse(
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd("standard", null).isOk());
		assertEquals(
				GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED,
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd("standard", null)
						.errorCode());
		assertEquals(
				GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID,
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd("standard", 0).errorCode());
		assertEquals(
				1001L,
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd("standard", 1001)
						.distributorId());
	}

	@Test
	void resolveDistributorIdForCheckoutAdd_platformOptionalDefaultsZero() {
		assertTrue(
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd("platform", null).isOk());
		assertEquals(
				0L,
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd("platform", null)
						.distributorId());
		assertEquals(
				GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID,
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd("platform", 1001)
						.errorCode());
	}
}
