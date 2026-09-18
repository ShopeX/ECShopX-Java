package cn.shopex.ecshopx.common.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.OrderTypeSlugMapper.Resolved;
import org.junit.jupiter.api.Test;

class OrderTypeSlugMapperTest {

	@Test
	void resolve_normal() {
		assertResolved("normal", "normal", "normal");
	}

	@Test
	void resolve_service() {
		assertResolved("service", "service", "normal");
	}

	@Test
	void resolve_bargain() {
		assertResolved("bargain", "bargain", "bargain");
	}

	@Test
	void resolve_normalBargain() {
		assertResolved("normal_bargain", "normal", "bargain");
	}

	@Test
	void resolve_normalSeckill() {
		assertResolved("normal_seckill", "normal", "seckill");
	}

	@Test
	void resolve_serviceSeckill() {
		assertResolved("service_seckill", "service", "seckill");
	}

	@Test
	void resolve_normalDrug() {
		assertResolved("normal_drug", "normal", "drug");
	}

	@Test
	void resolve_normalShopguide() {
		assertResolved("normal_shopguide", "normal", "shopguide");
	}

	@Test
	void resolve_normalPointsmall() {
		assertResolved("normal_pointsmall", "normal", "pointsmall");
	}

	@Test
	void resolve_normalExcard() {
		assertResolved("normal_excard", "normal", "excard");
	}

	@Test
	void resolve_normalCommunity() {
		assertResolved("normal_community", "normal", "community");
	}

	@Test
	void resolve_normalShopadmin() {
		assertResolved("normal_shopadmin", "normal", "shopadmin");
	}

	@Test
	void resolve_normalEmployeePurchase() {
		assertResolved("normal_employee_purchase", "normal", "employee_purchase");
	}

	@Test
	void resolve_normalGroups() {
		assertResolved("normal_groups", "normal", "groups");
	}

	@Test
	void resolve_serviceGroups() {
		assertResolved("service_groups", "service", "groups");
	}

	@Test
	void resolve_groupsAlias() {
		assertResolved("groups", "service", "groups");
	}

	@Test
	void resolve_isCaseAndWhitespaceInsensitive() {
		Resolved r = OrderTypeSlugMapper.resolve("  Normal_Seckill  ");
		assertThat(r.orderType()).isEqualTo("normal");
		assertThat(r.orderClass()).isEqualTo("seckill");
	}

	@Test
	void resolve_unknownSlug_throws() {
		assertThatThrownBy(() -> OrderTypeSlugMapper.resolve("unknown_xxx"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无此类型订单！");
	}

	@Test
	void resolve_blankSlug_throws() {
		assertThatThrownBy(() -> OrderTypeSlugMapper.resolve(""))
				.isInstanceOf(ResourceException.class);
		assertThatThrownBy(() -> OrderTypeSlugMapper.resolve("   "))
				.isInstanceOf(ResourceException.class);
	}

	@Test
	void resolve_nullSlug_throws() {
		assertThatThrownBy(() -> OrderTypeSlugMapper.resolve(null))
				.isInstanceOf(ResourceException.class);
	}

	private static void assertResolved(String slug, String expectedType, String expectedClass) {
		Resolved r = OrderTypeSlugMapper.resolve(slug);
		assertThat(r.orderType()).isEqualTo(expectedType);
		assertThat(r.orderClass()).isEqualTo(expectedClass);
	}
}
