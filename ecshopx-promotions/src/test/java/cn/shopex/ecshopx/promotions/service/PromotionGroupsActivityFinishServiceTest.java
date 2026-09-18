package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

class PromotionGroupsActivityFinishServiceTest {

	@Test
	void finishPromotionGroupsActivity_afterApplyFinishWrites_invokesScheduleAutoCancelGroupOrdersOnce() {
		MessageSource messageSource = Mockito.mock(MessageSource.class);
		PromotionGroupsActivityFinishWritesService writes =
				Mockito.mock(PromotionGroupsActivityFinishWritesService.class);
		PromotionGroupsTeamAutoCancelService autoCancel =
				Mockito.mock(PromotionGroupsTeamAutoCancelService.class);
		MarketingActivityPostCommitJobsService marketing =
				Mockito.mock(MarketingActivityPostCommitJobsService.class);

		long companyId = 100L;
		long groupsActivityId = 2001L;
		Map<String, Object> finishPayload = Map.of("status", "finished");
		when(writes.applyFinishWrites(eq(companyId), eq(groupsActivityId), any(Locale.class)))
				.thenReturn(finishPayload);

		PromotionGroupsActivityFinishService service = new PromotionGroupsActivityFinishService(
				messageSource, writes, autoCancel, marketing);

		Map<String, Object> result = service.finishPromotionGroupsActivity(companyId, String.valueOf(groupsActivityId));

		assertSame(finishPayload, result);
		InOrder order = inOrder(writes, autoCancel, marketing);
		order.verify(writes).applyFinishWrites(eq(companyId), eq(groupsActivityId), any(Locale.class));
		order.verify(autoCancel).scheduleAutoCancelGroupOrders();
		order.verify(marketing).enqueueGroupSalespersonItemsShelves(companyId, groupsActivityId);
		verify(autoCancel, Mockito.times(1)).scheduleAutoCancelGroupOrders();
	}
}
