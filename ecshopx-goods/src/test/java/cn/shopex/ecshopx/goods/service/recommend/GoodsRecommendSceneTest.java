package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class GoodsRecommendSceneTest {

	@Test
	void parse_acceptsKnownScenes() {
		assertEquals(GoodsRecommendScene.DETAIL, GoodsRecommendScene.parse("detail"));
		assertEquals(GoodsRecommendScene.CHECKOUT, GoodsRecommendScene.parse("CHECKOUT"));
	}

	@Test
	void parse_unknownReturnsNull() {
		assertNull(GoodsRecommendScene.parse("unknown"));
		assertNull(GoodsRecommendScene.parse(""));
	}
}
