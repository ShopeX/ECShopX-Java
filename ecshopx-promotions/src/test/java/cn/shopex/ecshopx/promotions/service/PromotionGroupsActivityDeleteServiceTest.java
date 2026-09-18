package cn.shopex.ecshopx.promotions.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

class PromotionGroupsActivityDeleteServiceTest {

	@Test
	void deletePromotionGroupsActivity_afterApplyDeleteWrites_invokesEnqueueGroupSalespersonItemsShelvesOnce() {
		MessageSource messageSource = Mockito.mock(MessageSource.class);
		PromotionGroupsActivityDeleteWritesService writes =
				Mockito.mock(PromotionGroupsActivityDeleteWritesService.class);
		MarketingActivityPostCommitJobsService marketing =
				Mockito.mock(MarketingActivityPostCommitJobsService.class);

		long companyId = 100L;
		long groupsActivityId = 2001L;
		when(writes.applyDeleteWrites(eq(companyId), eq(groupsActivityId), any(Locale.class)))
				.thenReturn(groupsActivityId);

		PromotionGroupsActivityDeleteService service =
				new PromotionGroupsActivityDeleteService(messageSource, writes, marketing);

		service.deletePromotionGroupsActivity(companyId, String.valueOf(groupsActivityId));

		InOrder order = inOrder(writes, marketing);
		order.verify(writes).applyDeleteWrites(eq(companyId), eq(groupsActivityId), any(Locale.class));
		order.verify(marketing).enqueueGroupSalespersonItemsShelves(companyId, groupsActivityId);
		verify(marketing, Mockito.times(1)).enqueueGroupSalespersonItemsShelves(companyId, groupsActivityId);
	}
}
