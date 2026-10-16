package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountNewGiftCardUpdateService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivitySaveRules;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberSignInCouponCandidateService {

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;

	public MemberSignInCouponCandidateService(
			DiscountCardsMapper discountCardsMapper,
			UserDiscountReceiveCardService userDiscountReceiveCardService) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
	}

	public Map<String, Object> listCandidates(
			long companyId, String keyword, String pageRaw, String pageSizeRaw) {
		long nowSec = Instant.now().getEpochSecond();
		int page = MemberSigninActivitySaveRules.parsePage(pageRaw);
		int pageSize = MemberSigninActivitySaveRules.parsePageSize(pageSizeRaw);

		LambdaQueryWrapper<DiscountCards> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(DiscountCards::getCompanyId, companyId);
		wrapper.eq(DiscountCards::getKqStatus, DiscountNewGiftCardUpdateService.STATUS_NORMAL);
		wrapper.and(
				w ->
						w.isNull(DiscountCards::getEndDate)
								.or()
								.eq(DiscountCards::getEndDate, 0)
								.or()
								.gt(DiscountCards::getEndDate, (int) nowSec));
		if (StringUtils.hasText(keyword)) {
			String escaped = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
			wrapper.apply("title LIKE CONCAT('%',{0},'%') ESCAPE '\\\\'", escaped);
		}
		wrapper.orderByDesc(DiscountCards::getCardId);

		Page<DiscountCards> pageReq = new Page<>(page, pageSize);
		Page<DiscountCards> pageResult = discountCardsMapper.selectPage(pageReq, wrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		for (DiscountCards card : pageResult.getRecords()) {
			int quantity = card.getQuantity() == null ? 0 : card.getQuantity();
			int issued = userDiscountReceiveCardService.countIssuedForCard(companyId, card.getCardId());
			int stock = Math.max(0, quantity - issued);
			if (stock <= 0) {
				continue;
			}
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("id", card.getCardId());
			item.put("title", card.getTitle());
			item.put("stock", stock);
			item.put("valid_begin", card.getBeginDate());
			item.put("valid_end", card.getEndDate());
			list.add(item);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", list.size());
		out.put("page", page);
		out.put("page_size", pageSize);
		return out;
	}
}
