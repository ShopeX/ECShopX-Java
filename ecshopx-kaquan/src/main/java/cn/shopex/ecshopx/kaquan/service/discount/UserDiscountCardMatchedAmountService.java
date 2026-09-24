package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountCardMatchedAmountService {

	private final OperatorCartSkuLoadFacade operatorCartSkuLoadFacade;

	public UserDiscountCardMatchedAmountService(OperatorCartSkuLoadFacade operatorCartSkuLoadFacade) {
		this.operatorCartSkuLoadFacade = operatorCartSkuLoadFacade;
	}

	public Map<Long, CouponCartItemScope> loadScopes(long companyId, Collection<Long> itemIds) {
		if (companyId <= 0L || itemIds == null || itemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, CouponCartItemScope> loaded = operatorCartSkuLoadFacade.loadCouponCartItemScopes(companyId, itemIds);
		return loaded == null ? Map.of() : loaded;
	}
}
