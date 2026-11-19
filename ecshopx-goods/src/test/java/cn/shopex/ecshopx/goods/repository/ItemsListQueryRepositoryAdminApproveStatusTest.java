package cn.shopex.ecshopx.goods.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemsListQueryRepositoryAdminApproveStatusTest {

	@Test
	void expandAdminApproveStatusAlias_offsale_mapsToInstockAndOfflineSale() {
		assertThat(ItemsListQueryRepository.expandAdminApproveStatusAlias("offsale"))
				.containsExactly("instock", "offline_sale");
		assertThat(ItemsListQueryRepository.expandAdminApproveStatusAlias("OffSale"))
				.containsExactly("instock", "offline_sale");
	}

	@Test
	void expandAdminApproveStatusAlias_onsale_mapsToOnsaleAndOnlyShow() {
		assertThat(ItemsListQueryRepository.expandAdminApproveStatusAlias("onsale"))
				.containsExactly("onsale", "only_show");
	}

	@Test
	void expandAdminApproveStatusAlias_unknown_returnsNull() {
		assertThat(ItemsListQueryRepository.expandAdminApproveStatusAlias("only_show")).isNull();
		assertThat(ItemsListQueryRepository.expandAdminApproveStatusAlias(null)).isNull();
	}
}