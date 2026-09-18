package cn.shopex.ecshopx.theme.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PagesTemplateWidgetItemsQueryTest {

	@Test
	void h5Front_parsesEActivityIdAndEnablesStoreOnsaleFilter() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("data_type", "items");
		request.setParameter("data_value[0]", "7670");
		request.setParameter("e_activity_id", "166");
		request.setParameter("num", "4");

		PagesTemplateWidgetItemsQuery q =
				PagesTemplateWidgetItemsQuery.fromH5FrontHttpServletRequest(request);

		assertEquals(166L, q.getEActivityId());
		assertTrue(q.isApplyStoreOnsaleFilter());
	}

	@Test
	void admin_parsesEActivityIdButDoesNotApplyStoreOnsaleFilter() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("data_type", "items");
		request.setParameter("data_value", "7670");
		request.setParameter("e_activity_id", "166");

		PagesTemplateWidgetItemsQuery q = PagesTemplateWidgetItemsQuery.fromHttpServletRequest(request);

		assertEquals(166L, q.getEActivityId());
		assertFalse(q.isApplyStoreOnsaleFilter());
	}

	@Test
	void missingOrInvalidEActivityIdIsZero() {
		MockHttpServletRequest missing = new MockHttpServletRequest();
		missing.setParameter("data_type", "items");
		missing.setParameter("data_value", "1");
		assertEquals(
				0L, PagesTemplateWidgetItemsQuery.fromH5FrontHttpServletRequest(missing).getEActivityId());

		MockHttpServletRequest invalid = new MockHttpServletRequest();
		invalid.setParameter("data_type", "items");
		invalid.setParameter("data_value", "1");
		invalid.setParameter("e_activity_id", "abc");
		assertEquals(
				0L, PagesTemplateWidgetItemsQuery.fromH5FrontHttpServletRequest(invalid).getEActivityId());
	}
}
