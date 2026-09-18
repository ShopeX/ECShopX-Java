package cn.shopex.ecshopx.promotions.integration.order;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.RefundByOrderUpdateOrderStatusJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RefundByOrderUpdateOrderStatusPromotionGroupTeamOrderFailHandlerTest {

	@Test
	void onFailedTeamMember_publishesJobWithOrderCompanyAndOrderType() {
		RefundByOrderUpdateOrderStatusJobDispatchPublisher publisher =
				Mockito.mock(RefundByOrderUpdateOrderStatusJobDispatchPublisher.class);
		RefundByOrderUpdateOrderStatusPromotionGroupTeamOrderFailHandler handler =
				new RefundByOrderUpdateOrderStatusPromotionGroupTeamOrderFailHandler(publisher);

		PromotionGroupsTeam team = new PromotionGroupsTeam();
		team.setCompanyId(100L);
		team.setGroupGoodsType("other");

		PromotionGroupsTeamMember member = new PromotionGroupsTeamMember();
		member.setOrderId("555");
		member.setCompanyId(0L);
		member.setGroupGoodsType("services");

		handler.onFailedTeamMember(team, member);

		verify(publisher).publish(eq(555L), eq(100L), eq("service_groups"));

		PromotionGroupsTeamMember member2 = new PromotionGroupsTeamMember();
		member2.setOrderId("556");
		member2.setCompanyId(88L);
		member2.setGroupGoodsType("physical");

		handler.onFailedTeamMember(team, member2);

		verify(publisher).publish(eq(556L), eq(88L), eq("normal_groups"));
	}
}
