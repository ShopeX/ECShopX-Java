package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
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
	void resolveDistributorIdForRecommendMerge_standardRequiresPositive() {
		assertFalse(
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge("standard", null).isOk());
		assertEquals(
				GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED,
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge("standard", null)
						.errorCode());
		assertEquals(
				GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID,
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge("standard", 0).errorCode());
		assertEquals(
				1001L,
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge("standard", 1001)
						.distributorId());
	}

	@Test
	void parseRecommendItemLines_null_returnsEmpty() {
		assertEquals(List.of(), GoodsRecommendRequestParser.parseRecommendItemLines(null));
	}

	@Test
	void parseRecommendItemLines_objectsOnly() {
		List<GoodsRecommendRequestParser.RecommendAddLine> lines =
				GoodsRecommendRequestParser.parseRecommendItemLines(
						List.of(Map.of("item_id", 5, "num", 1), Map.of("item_id", 8, "num", 2)));
		assertEquals(2, lines.size());
		assertEquals(5L, lines.get(0).itemId());
		assertEquals(1, lines.get(0).num());
		assertEquals(8L, lines.get(1).itemId());
		assertEquals(2, lines.get(1).num());
	}

	@Test
	void parseRecommendItemLines_sameItemId_accumulates() {
		List<GoodsRecommendRequestParser.RecommendAddLine> lines =
				GoodsRecommendRequestParser.parseRecommendItemLines(
						List.of(Map.of("item_id", 5, "num", 1), Map.of("item_id", 5, "num", 3)));
		assertEquals(1, lines.size());
		assertEquals(5L, lines.get(0).itemId());
		assertEquals(4, lines.get(0).num());
	}

	@Test
	void parseRecommendItemLines_bareId_throws() {
		assertThrows(
				IllegalArgumentException.class,
				() -> GoodsRecommendRequestParser.parseRecommendItemLines(List.of(5)));
	}

	@Test
	void parseRecommendItemLines_missingNum_throws() {
		assertThrows(
				IllegalArgumentException.class,
				() -> GoodsRecommendRequestParser.parseRecommendItemLines(List.of(Map.of("item_id", 5))));
	}

	@Test
	void parseRecommendItemLines_scalar_throws() {
		assertThrows(
				IllegalArgumentException.class, () -> GoodsRecommendRequestParser.parseRecommendItemLines(5));
	}

	@Test
	void parseRecommendItemLines_jsonString_parsesArray() {
		List<GoodsRecommendRequestParser.RecommendAddLine> lines =
				GoodsRecommendRequestParser.parseRecommendItemLines("[{\"item_id\":6729,\"num\":1}]");
		assertEquals(1, lines.size());
		assertEquals(6729L, lines.get(0).itemId());
		assertEquals(1, lines.get(0).num());
	}

	@Test
	void parseRecommendItemLines_emptyJsonString_returnsEmpty() {
		assertEquals(List.of(), GoodsRecommendRequestParser.parseRecommendItemLines("[]"));
		assertEquals(List.of(), GoodsRecommendRequestParser.parseRecommendItemLines(""));
	}

	@Test
	void parseRecommendItemLines_invalidJsonString_throws() {
		assertThrows(
				IllegalArgumentException.class,
				() -> GoodsRecommendRequestParser.parseRecommendItemLines("{\"item_id\":6729}"));
	}

	@Test
	void resolveDistributorIdForRecommendMerge_platformOptionalDefaultsZero() {
		assertTrue(
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge("platform", null).isOk());
		assertEquals(
				0L,
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge("platform", null)
						.distributorId());
		assertEquals(
				GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID,
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge("platform", 1001)
						.errorCode());
	}
}
