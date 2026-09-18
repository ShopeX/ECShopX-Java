/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseDistributorItemsQueryMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseAdminService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseSyncService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsRelCatsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ActivityCreateService {

	private final ActivitiesMapper activitiesMapper;
	private final ActivityEnterprisesMapper activityEnterprisesMapper;
	private final ObjectMapper objectMapper;
	private final EmployeePurchaseActivityItemWriteService employeePurchaseActivityItemWriteService;
	private final ItemsMapper itemsMapper;
	private final ItemsRelCatsMapper itemsRelCatsMapper;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final EmployeePurchaseDistributorItemsQueryMapper employeePurchaseDistributorItemsQueryMapper;
	private final ActivityItemsMapper activityItemsMapper;
	private final ActivityPassphraseSyncService activityPassphraseSyncService;
	private final ActivityPassphraseAdminService activityPassphraseAdminService;
	private final EnterpriseConfigService enterpriseConfigService;
	private final EmployeePurchaseActivityItemsCategoryRedisService
			employeePurchaseActivityItemsCategoryRedisService;

	public ActivityCreateService(
			ActivitiesMapper activitiesMapper,
			ActivityEnterprisesMapper activityEnterprisesMapper,
			ObjectMapper objectMapper,
			EmployeePurchaseActivityItemWriteService employeePurchaseActivityItemWriteService,
			ItemsMapper itemsMapper,
			ItemsRelCatsMapper itemsRelCatsMapper,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			ItemsCategoryRepository itemsCategoryRepository,
			EmployeePurchaseDistributorItemsQueryMapper employeePurchaseDistributorItemsQueryMapper,
			ActivityItemsMapper activityItemsMapper,
			ActivityPassphraseSyncService activityPassphraseSyncService,
			ActivityPassphraseAdminService activityPassphraseAdminService,
			EnterpriseConfigService enterpriseConfigService,
			EmployeePurchaseActivityItemsCategoryRedisService
					employeePurchaseActivityItemsCategoryRedisService) {
		this.activitiesMapper = activitiesMapper;
		this.activityEnterprisesMapper = activityEnterprisesMapper;
		this.objectMapper = objectMapper;
		this.employeePurchaseActivityItemWriteService = employeePurchaseActivityItemWriteService;
		this.itemsMapper = itemsMapper;
		this.itemsRelCatsMapper = itemsRelCatsMapper;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.employeePurchaseDistributorItemsQueryMapper = employeePurchaseDistributorItemsQueryMapper;
		this.activityItemsMapper = activityItemsMapper;
		this.activityPassphraseSyncService = activityPassphraseSyncService;
		this.activityPassphraseAdminService = activityPassphraseAdminService;
		this.enterpriseConfigService = enterpriseConfigService;
		this.employeePurchaseActivityItemsCategoryRedisService =
				employeePurchaseActivityItemsCategoryRedisService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		long distributorIdJwt = readDistributorId(operatorJwt);
		int operatorIdJwt = readOperatorId(operatorJwt);

		LinkedHashMap<String, Object> params = new LinkedHashMap<>(merged);
		params.put("company_id", companyId);

		boolean newContract = hasPurchaseModeInRequest(params);
		String purchaseMode = null;
		List<EnterpriseConfigService.EnterpriseConfigInput> enterpriseConfigs = null;
		boolean passphraseEnabled = false;

		if (newContract) {
			purchaseMode = requirePurchaseMode(params);
			if (PurchaseModeSupport.isPrepaidPoint(purchaseMode)) {
				rejectRelativeConfigForPrepaid(params);
				params.put("if_relative_join", 0);
				params.put("if_share_limitfee", 0);
			}
		}

		params.put("if_relative_join", normalize01("if_relative_join", params));
		params.put("if_share_limitfee", normalize01("if_share_limitfee", params));
		params.put("is_discount_description_enabled", normalize01("is_discount_description_enabled", params));

		int ifRelativeJoin = intFrom01(params.get("if_relative_join"));
		int ifShareLimitfee = intFrom01(params.get("if_share_limitfee"));

		if (newContract) {
			validateFieldsNewContract(params, ifRelativeJoin, ifShareLimitfee, purchaseMode);
			passphraseEnabled =
					Boolean.TRUE.equals(
							ActivityPassphraseSyncService.parseEnabled(params.get("is_passphrase_enabled")));
			List<Long> enterpriseIdsForConfig = parseEnterpriseIdList(params.get("enterprise_id"));
			enterpriseConfigs =
					enterpriseConfigService.parseAndValidate(
							params.get("enterprise_configs"), enterpriseIdsForConfig, passphraseEnabled);
		} else {
			validateFields(params, ifRelativeJoin, ifShareLimitfee);
		}

		List<Long> enterpriseIds = parseEnterpriseIdList(params.get("enterprise_id"));
		long pagesTemplateId = requireLong(params, "pages_template_id", "请选择活动首页关联模版");
		int displayTime = requireInt(params, "display_time", "请选择活动预热时间");
		int employeeBeginTime = requireInt(params, "employee_begin_time", "请选择员工购买开始时间");
		int employeeEndTime = requireInt(params, "employee_end_time", "请选择员工购买结束时间");
		int employeeLimitfee =
				newContract
						? readIntOrDefault(params.get("employee_limitfee"), 0)
						: requireInt(params, "employee_limitfee", "请输入员工可使用额度");
		int minimumAmount = requireInt(params, "minimum_amount", "请填写订单最低金额");
		int closeModifyHours = requireInt(
				params, "close_modify_hours_after_activity", "请填写活动结束后多少小时内可以修改收货地址");

		int inviteLimit = 0;
		Integer relativeBeginTime = null;
		Integer relativeEndTime = null;
		int relativeLimitfee = 0;

		if (ifRelativeJoin == 1) {
			inviteLimit = requireInt(params, "invite_limit", "请输入员工可邀请亲友人数上限");
			relativeBeginTime = requireInt(params, "relative_begin_time", "请选择亲友购买开始时间");
			relativeEndTime = requireInt(params, "relative_end_time", "请选择亲友购买结束时间");
			if (ifShareLimitfee == 0) {
				relativeLimitfee = requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
			}
		} else if (PurchaseModeSupport.isPrepaidPoint(purchaseMode)) {
			relativeLimitfee = 0;
		} else if (ifShareLimitfee == 0) {
			relativeLimitfee = requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
		}

		if (displayTime > employeeBeginTime) {
			throw new ResourceException("预热时间不能晚于员工开始购买时间");
		}
		if (ifRelativeJoin == 1 && relativeBeginTime != null && displayTime > relativeBeginTime) {
			throw new ResourceException("预热时间不能晚于家属开始购买时间");
		}

		params.put("distributor_id", distributorIdJwt);
		params.put("operator_id", operatorIdJwt);

		String name = requireString(params, "name", "请输入活动名称");
		String title = requireString(params, "title", "请输入活动标题");
		String pic = requireString(params, "pic", "请上传活动图片");
		String sharePic = requireString(params, "share_pic", "请上传活动分享图片");
		String listPic = requireString(params, "list_pic", "请上传活动列表海报");

		Object priceRaw = params.get("price_display_config");
		JsonNode priceParsed = parsePriceDisplayConfigNode(priceRaw);
		String priceDisplayConfigDb = null;
		if (priceParsed != null && !priceParsed.isNull()) {
			try {
				priceDisplayConfigDb = objectMapper.writeValueAsString(priceParsed);
			} catch (JsonProcessingException e) {
				priceDisplayConfigDb = null;
			}
		}

		Object discObj = params.get("discount_description");
		String discountDescription = discObj == null ? "" : discObj.toString();

		boolean ifRelBool = ifRelativeJoin == 1;
		boolean ifShareBool = ifShareLimitfee == 1;
		boolean isDiscountDescEnabled = intFrom01(params.get("is_discount_description_enabled")) == 1;

		int now = (int) (System.currentTimeMillis() / 1000);
		Activities entity = new Activities();
		entity.setCompanyId(companyId);
		entity.setDistributorId(toIntBounded(distributorIdJwt));
		entity.setOperatorId(operatorIdJwt);
		entity.setName(name);
		entity.setTitle(title);
		entity.setPagesTemplateId(pagesTemplateId);
		entity.setPic(pic);
		entity.setSharePic(sharePic);
		entity.setListPic(listPic);
		entity.setEnterpriseId(enterpriseIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
		entity.setDisplayTime(displayTime);
		entity.setEmployeeBeginTime(employeeBeginTime);
		entity.setEmployeeEndTime(employeeEndTime);
		entity.setEmployeeLimitfee(employeeLimitfee);
		entity.setIfRelativeJoin(ifRelBool);
		if (ifRelBool) {
			entity.setInviteLimit(inviteLimit);
			entity.setRelativeBeginTime(relativeBeginTime);
			entity.setRelativeEndTime(relativeEndTime);
			entity.setIfShareLimitfee(ifShareBool);
			entity.setRelativeLimitfee(relativeLimitfee);
		} else {
			entity.setInviteLimit(0);
			entity.setRelativeBeginTime(null);
			entity.setRelativeEndTime(null);
			entity.setIfShareLimitfee(false);
			entity.setRelativeLimitfee(relativeLimitfee);
		}
		entity.setMinimumAmount(minimumAmount);
		entity.setCloseModifyHoursAfterActivity(closeModifyHours);
		entity.setStatus("active");
		entity.setIfShareStore(false);
		entity.setPriceDisplayConfig(priceDisplayConfigDb);
		entity.setIsDiscountDescriptionEnabled(isDiscountDescEnabled);
		entity.setDiscountDescription(discountDescription);
		if (newContract) {
			entity.setPurchaseMode(purchaseMode);
		}
		entity.setCreated(now);
		entity.setUpdated(now);

		activitiesMapper.insert(entity);
		Long activityId = entity.getId();

		if (newContract) {
			enterpriseConfigService.saveOnCreate(
					companyId, activityId, enterpriseIds, enterpriseConfigs, passphraseEnabled);
		} else {
			for (Long eid : enterpriseIds) {
				ActivityEnterprises row = new ActivityEnterprises();
				row.setActivityId(activityId);
				row.setEnterpriseId(eid);
				row.setCompanyId(companyId);
				activityEnterprisesMapper.insert(row);
			}
			activityPassphraseSyncService.applyOnCreate(
					companyId, activityId, new HashSet<>(enterpriseIds), params);
		}

		Activities fresh =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId));
		return toResponseMap(fresh != null ? fresh : entity, enterpriseIds);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateActivity(
			String activityId, Map<String, Object> merged, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		long distributorIdJwt = readDistributorId(operatorJwt);
		int operatorIdJwt = readOperatorId(operatorJwt);

		LinkedHashMap<String, Object> params = new LinkedHashMap<>(merged);

		long id = parsePathActivityIdForUpdate(activityId);
		Activities row = activitiesMapper.selectOne(
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, id));
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}

		boolean dbNewContract = PurchaseModeSupport.isNewContractActivity(row);
		boolean requestHasNewFields =
				hasPurchaseModeInRequest(params) || params.containsKey("enterprise_configs");
		if (!dbNewContract && requestHasNewFields) {
			throw new ResourceException("历史活动不支持修改购买方式与企业额度配置，请新建活动");
		}

		if (dbNewContract) {
			if (PurchaseModeSupport.isPrepaidPoint(row.getPurchaseMode())) {
				rejectRelativeConfigForPrepaid(params);
				params.put("if_relative_join", 0);
				params.put("if_share_limitfee", 0);
			}
			if (hasPurchaseModeInRequest(params)) {
				String reqMode = requirePurchaseMode(params);
				if (!reqMode.equals(row.getPurchaseMode())) {
					throw new ResourceException("购买方式与企业额度配置创建后不可修改");
				}
			}
		}

		params.put("if_relative_join", normalizeStringOnlyTrueOrOne01("if_relative_join", params));
		int ifRelativeJoinAfterNorm = intFrom01(params.get("if_relative_join"));
		if (ifRelativeJoinAfterNorm == 1) {
			if (!params.containsKey("if_share_limitfee") || params.get("if_share_limitfee") == null) {
				throw new BadRequestException("请选择亲友是否共享员工额度");
			}
		}
		params.put("if_share_limitfee", normalizeStringOnlyTrueOrOne01("if_share_limitfee", params));
		params.put("is_discount_description_enabled", normalize01("is_discount_description_enabled", params));

		int ifRelativeJoin = intFrom01(params.get("if_relative_join"));
		int ifShareLimitfee = intFrom01(params.get("if_share_limitfee"));

		if (dbNewContract) {
			validateUpdateActivityParamsNewContract(
					params, ifRelativeJoin, ifShareLimitfee, row.getPurchaseMode());
		} else {
			validateUpdateActivityParams(params, ifRelativeJoin, ifShareLimitfee);
		}

		int displayTime = requireInt(params, "display_time", "请选择活动预热时间");
		int employeeBeginTime = requireInt(params, "employee_begin_time", "请选择员工购买开始时间");
		if (displayTime > employeeBeginTime) {
			throw new ResourceException("预热时间不能晚于员工开始购买时间");
		}
		if (ifRelativeJoin != 0) {
			int relativeBeginTime = requireInt(params, "relative_begin_time", "请选择亲友购买开始时间");
			if (displayTime > relativeBeginTime) {
				throw new ResourceException("预热时间不能晚于家属开始购买时间");
			}
		}

		params.put("distributor_id", distributorIdJwt);
		params.put("operator_id", operatorIdJwt);

		Object priceRaw = params.get("price_display_config");
		JsonNode priceNode = parsePriceDisplayConfigNode(priceRaw);
		if (priceNode == null || priceNode.isNull()) {
			params.remove("price_display_config");
		} else {
			try {
				params.put("price_display_config", objectMapper.writeValueAsString(priceNode));
			} catch (JsonProcessingException e) {
				params.remove("price_display_config");
			}
		}

		List<Long> enterpriseIds = parseEnterpriseIdList(params.get("enterprise_id"));
		List<EnterpriseConfigService.EnterpriseConfigInput> requestConfigs = List.of();
		boolean passphraseEnabled = false;

		if (dbNewContract) {
			passphraseEnabled = Boolean.TRUE.equals(row.getIsPassphraseEnabled());
			if (params.containsKey("is_passphrase_enabled")) {
				Boolean reqEnabled =
						ActivityPassphraseSyncService.parseEnabled(params.get("is_passphrase_enabled"));
				if (reqEnabled != null && reqEnabled != passphraseEnabled) {
					throw new ResourceException("购买方式与企业额度配置创建后不可修改");
				}
			}
			requestConfigs =
					enterpriseConfigService.assertConfigsUnchanged(
							companyId, id, enterpriseIds, params.get("enterprise_configs"), passphraseEnabled);
		}

		String enterpriseIdStr = enterpriseIds.stream().map(String::valueOf).collect(Collectors.joining(","));
		int employeeEndTime = requireInt(params, "employee_end_time", "请选择员工购买结束时间");
		int employeeLimitfee =
				dbNewContract
						? (row.getEmployeeLimitfee() == null ? 0 : row.getEmployeeLimitfee())
						: requireInt(params, "employee_limitfee", "请输入员工可使用额度");
		int minimumAmount = requireInt(params, "minimum_amount", "请填写订单最低金额");
		int closeModifyHoursAfterActivity = requireInt(
				params,
				"close_modify_hours_after_activity",
				"请填写活动结束后多少小时内可以修改收货地址");
		int now = (int) (System.currentTimeMillis() / 1000);

		var u = Wrappers.<Activities>lambdaUpdate()
				.eq(Activities::getCompanyId, companyId)
				.eq(Activities::getId, id)
				.set(Activities::getDistributorId, toIntBounded(distributorIdJwt))
				.set(Activities::getOperatorId, operatorIdJwt)
				.set(Activities::getName, requireString(params, "name", "请输入活动名称"))
				.set(Activities::getTitle, requireString(params, "title", "请输入活动标题"))
				.set(Activities::getPagesTemplateId, requireLong(params, "pages_template_id", "请选择活动首页关联模版"))
				.set(Activities::getPic, requireString(params, "pic", "请上传活动图片"))
				.set(Activities::getSharePic, requireString(params, "share_pic", "请上传活动分享图片"))
				.set(Activities::getListPic, requireString(params, "list_pic", "请上传活动列表海报"))
				.set(Activities::getEnterpriseId, enterpriseIdStr)
				.set(Activities::getDisplayTime, displayTime)
				.set(Activities::getEmployeeBeginTime, employeeBeginTime)
				.set(Activities::getEmployeeEndTime, employeeEndTime)
				.set(Activities::getEmployeeLimitfee, employeeLimitfee)
				.set(Activities::getIfRelativeJoin, ifRelativeJoin == 1)
				.set(Activities::getIfShareLimitfee, ifShareLimitfee == 1)
				.set(Activities::getMinimumAmount, minimumAmount)
				.set(Activities::getCloseModifyHoursAfterActivity, closeModifyHoursAfterActivity)
				.set(
						Activities::getIsDiscountDescriptionEnabled,
						intFrom01(params.get("is_discount_description_enabled")) == 1)
				.set(Activities::getUpdated, now);
		if (paramKeyPresent(params, "invite_limit")) {
			u.set(
					Activities::getInviteLimit,
					requireInt(params, "invite_limit", "请输入员工可邀请亲友人数上限"));
		}
		if (paramKeyPresent(params, "relative_begin_time")) {
			u.set(
					Activities::getRelativeBeginTime,
					requireInt(params, "relative_begin_time", "请选择亲友购买开始时间"));
		}
		if (paramKeyPresent(params, "relative_end_time")) {
			u.set(
					Activities::getRelativeEndTime,
					requireInt(params, "relative_end_time", "请选择亲友购买结束时间"));
		}
		if (paramKeyPresent(params, "relative_limitfee")) {
			u.set(
					Activities::getRelativeLimitfee,
					requireInt(params, "relative_limitfee", "请填写亲友可使用额度"));
		}
		if (paramKeyPresent(params, "price_display_config")) {
			u.set(Activities::getPriceDisplayConfig, params.get("price_display_config").toString());
		}
		if (paramKeyPresent(params, "discount_description")) {
			u.set(Activities::getDiscountDescription, params.get("discount_description").toString());
		}
		activitiesMapper.update(null, u);

		if (dbNewContract) {
			enterpriseConfigService.applyParticipateQuotaOnUpdate(
					companyId, id, requestConfigs, passphraseEnabled);
		} else {
			activityEnterprisesMapper.delete(
					Wrappers.<ActivityEnterprises>lambdaQuery()
							.eq(ActivityEnterprises::getActivityId, id)
							.eq(ActivityEnterprises::getCompanyId, companyId));
			for (Long eid : enterpriseIds) {
				ActivityEnterprises entRow = new ActivityEnterprises();
				entRow.setActivityId(id);
				entRow.setEnterpriseId(eid);
				entRow.setCompanyId(companyId);
				activityEnterprisesMapper.insert(entRow);
			}
			activityPassphraseSyncService.applyOnUpdate(
					companyId, id, new HashSet<>(enterpriseIds), params);
		}

		Activities fresh = activitiesMapper.selectOne(
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, id));
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toResponseMap(fresh, enterpriseIds);
	}

	public Map<String, Object> addActivityItems(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		return addActivityItems(merged, operatorJwt, null);
	}

	public Map<String, Object> addActivityItems(
			Map<String, Object> merged,
			Map<String, Object> operatorJwt,
			Integer activityStoreOverride) {
		long jwtDistributorId = readDistributorId(operatorJwt);
		long activityPk = parseRequiredActivityPk(merged);
		long companyId = readCompanyId(operatorJwt);

		long distributorInput = parseDistributorInputLenient(merged.get("distributor_id"));
		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
		Object opType = operatorJwt.get("operator_type");
		String opTypeStr = opType == null ? "" : opType.toString();
		Long paramsDistributorId = null;
		if ("standard".equals(productModel) && "distributor".equals(opTypeStr) && distributorInput > 0) {
			paramsDistributorId = distributorInput;
		}

		Integer distributorSelf = null;
		if (paramsDistributorId != null && paramsDistributorId > 0 && "standard".equals(productModel)) {
			distributorSelf = employeePurchaseDistributorItemsQueryMapper.selectDistributorSelf(companyId, paramsDistributorId);
		}

		if (branchKeyTruthy(merged, "item_id")) {
			List<Long> itemIds = normalizeItemIdList(merged.get("item_id"));
			if (!itemIds.isEmpty()) {
				employeePurchaseActivityItemWriteService.writeOneBatch(
						activityPk, companyId, itemIds, jwtDistributorId, activityStoreOverride);
			}
		}

		if (branchKeyTruthy(merged, "main_cat_id")) {
			List<Long> mainCatRoots = normalizeLongIdList(merged.get("main_cat_id"));
			if (!mainCatRoots.isEmpty()) {
				List<String> mainCatKeys = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, mainCatRoots);
				if (!mainCatKeys.isEmpty()) {
					LambdaQueryWrapper<Items> itemWrapper = Wrappers.lambdaQuery();
					itemWrapper.eq(Items::getCompanyId, companyId).orderByAsc(Items::getItemId);
					boolean skipMainCatBranch = false;
					if (paramsDistributorId != null && paramsDistributorId > 0 && "standard".equals(productModel)) {
						Long totalCount = employeePurchaseDistributorItemsQueryMapper.countItemsForDistributorByItemCategories(
								companyId, paramsDistributorId, distributorSelf, mainCatKeys);
						long tc = totalCount == null ? 0L : totalCount;
						if (tc == 0) {
							skipMainCatBranch = true;
						} else {
							List<Long> allowedItemIds = employeePurchaseDistributorItemsQueryMapper.selectItemIdsForDistributorByItemCategories(
									companyId, paramsDistributorId, distributorSelf, mainCatKeys);
							if (allowedItemIds == null || allowedItemIds.isEmpty()) {
								skipMainCatBranch = true;
							} else {
								itemWrapper.in(Items::getItemId, allowedItemIds);
							}
						}
					} else {
						itemWrapper.in(Items::getItemCategory, mainCatKeys);
					}
					if (!skipMainCatBranch) {
						int pageNum = 1;
						int pageSize = 500;
						while (true) {
							Page<Items> page = new Page<>(pageNum, pageSize);
							itemsMapper.selectPage(page, itemWrapper);
							List<Items> records = page.getRecords();
							if (records.isEmpty()) {
								break;
							}
							List<Long> pageItemIds = records.stream().map(Items::getItemId).filter(id -> id != null).toList();
							employeePurchaseActivityItemWriteService.writeOneBatch(
									activityPk, companyId, pageItemIds, jwtDistributorId, activityStoreOverride);
							if (records.size() < pageSize) {
								break;
							}
							pageNum++;
						}
					}
				}
			}
		}

		if (branchKeyTruthy(merged, "cat_id")) {
			List<Long> catRoots = normalizeLongIdList(merged.get("cat_id"));
			if (!catRoots.isEmpty()) {
				List<Long> catIdsForRelCats = expandCategoryIdsTwoPass(companyId, catRoots);
				if (!catIdsForRelCats.isEmpty()) {
					int pageNum = 1;
					int pageSize = 200;
					while (true) {
						Page<ItemsRelCats> relPage = new Page<>(pageNum, pageSize);
						LambdaQueryWrapper<ItemsRelCats> relW = Wrappers.lambdaQuery();
						relW.eq(ItemsRelCats::getCompanyId, companyId)
								.in(ItemsRelCats::getCategoryId, catIdsForRelCats)
								.orderByAsc(ItemsRelCats::getItemId);
						itemsRelCatsMapper.selectPage(relPage, relW);
						List<ItemsRelCats> relRows = relPage.getRecords();
						if (relRows.isEmpty()) {
							break;
						}
						List<Long> relPageItemIds = relRows.stream()
								.map(ItemsRelCats::getItemId)
								.filter(id -> id != null)
								.toList();
						if (paramsDistributorId != null && paramsDistributorId > 0 && "standard".equals(productModel)) {
							Long c = employeePurchaseDistributorItemsQueryMapper.countItemsForDistributorByItemIds(
									companyId, paramsDistributorId, distributorSelf, relPageItemIds);
							if (c == null || c == 0L) {
								break;
							}
						}
						LambdaQueryWrapper<Items> skuW = Wrappers.lambdaQuery();
						skuW.eq(Items::getCompanyId, companyId).in(Items::getDefaultItemId, relPageItemIds);
						List<Items> skuRows = itemsMapper.selectList(skuW);
						if (skuRows.isEmpty()) {
							break;
						}
						List<Long> skuIds = skuRows.stream().map(Items::getItemId).filter(id -> id != null).toList();
						employeePurchaseActivityItemWriteService.writeOneBatch(
								activityPk, companyId, skuIds, jwtDistributorId, activityStoreOverride);
						if (relRows.size() < pageSize) {
							break;
						}
						pageNum++;
					}
				}
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", true);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> selectActivitySpecItems(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		readDistributorId(operatorJwt);
		long activityId = parseRequiredActivityPk(merged);
		long goodsId = parseRequiredGoodsId(merged);

		if (!branchKeyTruthy(merged, "item_id")) {
			throw new BadRequestException("规格ID必填");
		}
		List<Long> requestedItemIds = normalizeItemIdList(merged.get("item_id"));
		if (requestedItemIds.isEmpty()) {
			throw new ResourceException("请选择活动商品");
		}

		List<ActivityItems> existingRows = activityItemsMapper.selectList(
				Wrappers.<ActivityItems>lambdaQuery()
						.eq(ActivityItems::getCompanyId, companyId)
						.eq(ActivityItems::getActivityId, activityId)
						.eq(ActivityItems::getGoodsId, goodsId)
						.select(ActivityItems::getItemId));

		LinkedHashSet<Long> existingIds = existingRows.stream()
				.map(ActivityItems::getItemId)
				.filter(id -> id != null)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		LinkedHashSet<Long> requestedSet = new LinkedHashSet<>(requestedItemIds);
		existingIds.removeAll(requestedSet);

		if (!existingIds.isEmpty()) {
			activityItemsMapper.delete(
					Wrappers.<ActivityItems>lambdaQuery()
							.eq(ActivityItems::getCompanyId, companyId)
							.eq(ActivityItems::getActivityId, activityId)
							.eq(ActivityItems::getGoodsId, goodsId)
							.in(ActivityItems::getItemId, existingIds));
		}

		LinkedHashMap<String, Object> addOnly = new LinkedHashMap<>();
		addOnly.put("activity_id", String.valueOf(activityId));
		addOnly.put("item_id", requestedItemIds);
		return addActivityItems(addOnly, operatorJwt, Integer.valueOf(0));
	}

	public Map<String, Object> updateActivityItems(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		long activityId = parseRequiredActivityPk(merged);
		long jwtDistributorId = readDistributorId(operatorJwt);

		Object rawItemId = merged.get("item_id");
		if (rawItemId == null) {
			throw new BadRequestException("商品ID必填");
		}
		List<Long> itemIds = normalizeItemIdList(rawItemId);
		if (itemIds.isEmpty()) {
			throw new BadRequestException("商品ID必填");
		}

		Integer shelfStatusPatch = null;
		if (merged.containsKey("shelf_status")) {
			Object rawShelf = merged.get("shelf_status");
			if (rawShelf != null && StringUtils.hasText(rawShelf.toString().trim())) {
				int shelfStatus = parseIntForActivityItemPatch(rawShelf, "上下架状态");
				if (shelfStatus != 0 && shelfStatus != 1) {
					throw new BadRequestException("上下架状态无效");
				}
				shelfStatusPatch = shelfStatus;
			}
		}

		boolean allSpec = isTruthyAllFlag(merged.get("all"));
		var w = Wrappers.<ActivityItems>lambdaUpdate()
				.eq(ActivityItems::getCompanyId, companyId)
				.eq(ActivityItems::getActivityId, activityId);

		if (allSpec && shelfStatusPatch != null) {
			ActivityItems exist = activityItemsMapper.selectOne(
					Wrappers.<ActivityItems>lambdaQuery()
							.eq(ActivityItems::getCompanyId, companyId)
							.eq(ActivityItems::getActivityId, activityId)
							.in(ActivityItems::getItemId, itemIds)
							.last("LIMIT 1"));
			if (exist == null || exist.getGoodsId() == null) {
				throw new ResourceException("活动商品不存在");
			}
			w.eq(ActivityItems::getGoodsId, exist.getGoodsId());
		} else {
			w.in(ActivityItems::getItemId, itemIds);
		}

		boolean anyPatch = false;
		if (branchKeyTruthy(merged, "activity_price")) {
			w.set(ActivityItems::getActivityPrice, parseIntForActivityItemPatch(merged.get("activity_price"), "活动价格"));
			anyPatch = true;
		}
		if (branchKeyTruthy(merged, "activity_store")) {
			w.set(ActivityItems::getActivityStore, parseIntForActivityItemPatch(merged.get("activity_store"), "活动库存"));
			anyPatch = true;
		}
		if (branchKeyTruthy(merged, "limit_fee")) {
			w.set(ActivityItems::getLimitFee, parseIntForActivityItemPatch(merged.get("limit_fee"), "每人限额"));
			anyPatch = true;
		}
		if (branchKeyTruthy(merged, "limit_num")) {
			w.set(ActivityItems::getLimitNum, parseIntForActivityItemPatch(merged.get("limit_num"), "限购数量"));
			anyPatch = true;
		}
		if (branchKeyTruthy(merged, "sort")) {
			w.set(ActivityItems::getSort, parseIntForActivityItemPatch(merged.get("sort"), "排序"));
			anyPatch = true;
		}
		if (shelfStatusPatch != null) {
			w.set(ActivityItems::getShelfStatus, shelfStatusPatch);
			anyPatch = true;
		}
		if (!anyPatch) {
			throw new BadRequestException("更新内容不能为空");
		}

		activityItemsMapper.update(null, w);

		if (shelfStatusPatch != null) {
			employeePurchaseActivityItemsCategoryRedisService.store(companyId, activityId, jwtDistributorId);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", true);
		return out;
	}

	public Map<String, Object> activeActivity(String activityId, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		Long id = parsePathActivityIdOrNotFound(activityId);
		var q = Wrappers.<Activities>lambdaQuery()
				.eq(Activities::getCompanyId, companyId)
				.eq(Activities::getId, id);
		Activities row = activitiesMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("活动不存在");
		}
		if (!"pending".equals(row.getStatus())) {
			throw new ResourceException("只能开始暂停中的活动");
		}
		// 只用 lambdaUpdate.set：禁止 new Activities() 局部 update，否则默认 false 会覆盖 is_passphrase_enabled 等字段
		int affected =
				activitiesMapper.update(
						null,
						Wrappers.<Activities>lambdaUpdate()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, id)
								.set(Activities::getStatus, "active"));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", affected);
		return out;
	}

	public Map<String, Object> aheadActivity(String activityId, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		Long id = parsePathActivityIdOrNotFound(activityId);
		var q = Wrappers.<Activities>lambdaQuery()
				.eq(Activities::getCompanyId, companyId)
				.eq(Activities::getId, id);
		Activities row = activitiesMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("活动不存在");
		}
		if (!"active".equals(row.getStatus())) {
			throw new ResourceException("只能提前开始有效的活动");
		}
		int now = (int) (System.currentTimeMillis() / 1000);
		Integer displayTime = row.getDisplayTime();
		Integer employeeBeginTime = row.getEmployeeBeginTime();
		if (displayTime == null || employeeBeginTime == null) {
			throw new ResourceException("只能提前开始预热中的活动");
		}
		if (displayTime > now || employeeBeginTime < now) {
			throw new ResourceException("只能提前开始预热中的活动");
		}
		int affected =
				activitiesMapper.update(
						null,
						Wrappers.<Activities>lambdaUpdate()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, id)
								.set(Activities::getEmployeeBeginTime, now));
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", affected);
		return out;
	}

	public Map<String, Object> seIfShareStore(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);

		Object rawActivityId = merged.get("activity_id");
		if (rawActivityId == null) {
			throw new BadRequestException("活动ID必填");
		}
		String activityIdStr;
		if (rawActivityId instanceof String s) {
			activityIdStr = s.trim();
		} else {
			activityIdStr = rawActivityId.toString().trim();
		}
		if (activityIdStr.isEmpty()) {
			throw new BadRequestException("活动ID必填");
		}

		int share01 = parseIfShareStore01(merged.get("if_share_store"));

		long activityPk;
		try {
			activityPk = Long.parseLong(activityIdStr);
		} catch (NumberFormatException e) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("status", 0);
			return out;
		}

		boolean targetShare = share01 == 1;
		var u = Wrappers.<Activities>lambdaUpdate()
				.eq(Activities::getCompanyId, companyId)
				.eq(Activities::getId, activityPk);
		if (targetShare) {
			u.and(w -> w.isNull(Activities::getIfShareStore).or().eq(Activities::getIfShareStore, false));
		} else {
			u.and(w -> w.isNull(Activities::getIfShareStore).or().eq(Activities::getIfShareStore, true));
		}
		u.set(Activities::getIfShareStore, targetShare);
		int affected = activitiesMapper.update(null, u);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", affected);
		return out;
	}

	public Map<String, Object> cancelActivity(String activityId, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		Long id = parsePathActivityIdOrNotFound(activityId);
		var q = Wrappers.<Activities>lambdaQuery()
				.eq(Activities::getCompanyId, companyId)
				.eq(Activities::getId, id);
		Activities row = activitiesMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("活动不存在");
		}
		int now = (int) (System.currentTimeMillis() / 1000);
		Integer displayTime = row.getDisplayTime();
		int displayTimeForCompare = (displayTime == null) ? 0 : displayTime.intValue();
		if (displayTimeForCompare < now) {
			throw new ResourceException("只能取消未开始的活动");
		}
		int affected =
				activitiesMapper.update(
						null,
						Wrappers.<Activities>lambdaUpdate()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, id)
								.set(Activities::getStatus, "cancel"));
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", affected);
		return out;
	}

	public Map<String, Object> suspendActivity(String activityId, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		Long id = parsePathActivityIdOrNotFound(activityId);
		Activities row = activitiesMapper.selectOne(
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, id));
		if (row == null) {
			throw new ResourceException("活动不存在");
		}
		if (!"active".equals(row.getStatus())) {
			throw new ResourceException("只能暂停进行中的活动");
		}
		int now = (int) (System.currentTimeMillis() / 1000);
		int eb = row.getEmployeeBeginTime() == null ? 0 : row.getEmployeeBeginTime().intValue();
		int rb = row.getRelativeBeginTime() == null ? 0 : row.getRelativeBeginTime().intValue();
		int ee = row.getEmployeeEndTime() == null ? 0 : row.getEmployeeEndTime().intValue();
		int re = row.getRelativeEndTime() == null ? 0 : row.getRelativeEndTime().intValue();
		if ((eb > now && rb > now) || (ee < now && re < now)) {
			throw new ResourceException("只能暂停进行中的活动");
		}
		int affected =
				activitiesMapper.update(
						null,
						Wrappers.<Activities>lambdaUpdate()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, id)
								.set(Activities::getStatus, "pending"));
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", affected);
		return out;
	}

	public Map<String, Object> endActivity(String activityId, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		Long id = parsePathActivityIdOrNotFound(activityId);
		var q = Wrappers.<Activities>lambdaQuery()
				.eq(Activities::getCompanyId, companyId)
				.eq(Activities::getId, id);
		Activities row = activitiesMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("活动不存在");
		}
		int now = (int) (System.currentTimeMillis() / 1000);
		Integer displayTime = row.getDisplayTime();
		if (displayTime != null && displayTime > now) {
			throw new ResourceException("只能结束已开始的活动");
		}
		int affected =
				activitiesMapper.update(
						null,
						Wrappers.<Activities>lambdaUpdate()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, id)
								.ne(Activities::getStatus, "over")
								.set(Activities::getStatus, "over"));
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", affected);
		return out;
	}

	private static long parseRequiredGoodsId(Map<String, Object> merged) {
		Object raw = merged.get("goods_id");
		if (raw == null) {
			throw new BadRequestException("商品ID必填");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("商品ID必填");
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("商品ID必填");
			}
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			String t = raw.toString().trim();
			if (t.isEmpty()) {
				throw new BadRequestException("商品ID必填");
			}
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品ID必填");
		}
	}

	private static int parseIntForActivityItemPatch(Object raw, String label) {
		if (raw instanceof Number n) {
			return toIntBounded(n.longValue());
		}
		if (raw instanceof String s) {
			String t = s.trim();
			try {
				return toIntBounded(Long.parseLong(t));
			} catch (NumberFormatException e) {
				throw new BadRequestException(label + "格式错误");
			}
		}
		try {
			return toIntBounded(Long.parseLong(raw.toString().trim()));
		} catch (NumberFormatException e) {
			throw new BadRequestException(label + "格式错误");
		}
	}

	private static long parseRequiredActivityPk(Map<String, Object> merged) {
		Object raw = merged.get("activity_id");
		if (raw == null) {
			throw new BadRequestException("活动ID必填");
		}
		String activityIdStr;
		if (raw instanceof String s) {
			activityIdStr = s.trim();
		} else {
			activityIdStr = raw.toString().trim();
		}
		if (activityIdStr.isEmpty()) {
			throw new BadRequestException("活动ID必填");
		}
		try {
			return Long.parseLong(activityIdStr.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID必填");
		}
	}

	private static long parseDistributorInputLenient(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean branchKeyTruthy(Map<String, Object> merged, String key) {
		return merged.containsKey(key) && isTruthyBranchValue(merged.get(key));
	}

	private static boolean isTruthyAllFlag(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		String t = raw.toString().trim();
		return "1".equals(t) || "true".equalsIgnoreCase(t);
	}

	private static boolean isTruthyBranchValue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof CharSequence s) {
			String t = s.toString().trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		if (v instanceof java.util.Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return true;
	}

	private static List<Long> normalizeItemIdList(Object raw) {
		List<Long> base = normalizeLongIdList(raw);
		List<Long> out = new ArrayList<>();
		for (Long v : base) {
			if (v != null && v != 0L) {
				out.add(v);
			}
		}
		return out;
	}

	private static List<Long> normalizeLongIdList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				addLongsFromScalar(o, out);
			}
			return out;
		}
		List<Long> out = new ArrayList<>();
		addLongsFromScalar(raw, out);
		return out;
	}

	private static void addLongsFromScalar(Object o, List<Long> out) {
		if (o instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return;
			}
			if (t.contains(",")) {
				for (String part : t.split(",")) {
					String p = part.trim();
					if (p.isEmpty()) {
						continue;
					}
					try {
						out.add(Long.parseLong(p));
					} catch (NumberFormatException ignored) {
					}
				}
				return;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
			}
			return;
		}
		if (o instanceof Number n) {
			out.add(n.longValue());
			return;
		}
		try {
			out.add(Long.parseLong(o.toString().trim()));
		} catch (NumberFormatException ignored) {
		}
	}

	private List<Long> expandCategoryIdsTwoPass(long companyId, List<Long> roots) {
		List<Long> batch1 = itemsCategoryRepository.listCategoryIdsByParentIds(companyId, roots);
		LinkedHashSet<Long> catIds = new LinkedHashSet<>(roots);
		if (batch1.isEmpty()) {
			return new ArrayList<>(catIds);
		}
		catIds.addAll(batch1);
		List<Long> batch2 = itemsCategoryRepository.listCategoryIdsByParentIds(companyId, batch1);
		if (!batch2.isEmpty()) {
			catIds.addAll(batch2);
		}
		return new ArrayList<>(catIds);
	}

	private static int parseIfShareStore01(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择是否共享库存");
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v == 0 || v == 1) {
				return v;
			}
			throw new BadRequestException("请选择是否共享库存");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("请选择是否共享库存");
			}
			if ("0".equals(t)) {
				return 0;
			}
			if ("1".equals(t)) {
				return 1;
			}
			if ("true".equalsIgnoreCase(t)) {
				return 1;
			}
			if ("false".equalsIgnoreCase(t)) {
				return 0;
			}
			throw new BadRequestException("请选择是否共享库存");
		}
		String t = raw.toString().trim();
		if (t.isEmpty()) {
			throw new BadRequestException("请选择是否共享库存");
		}
		if ("0".equals(t)) {
			return 0;
		}
		if ("1".equals(t)) {
			return 1;
		}
		if ("true".equalsIgnoreCase(t)) {
			return 1;
		}
		if ("false".equalsIgnoreCase(t)) {
			return 0;
		}
		throw new BadRequestException("请选择是否共享库存");
	}

	private Long parsePathActivityIdOrNotFound(String activityId) {
		String t = activityId == null ? "" : activityId.trim();
		if (t.isEmpty()) {
			throw new ResourceException("活动不存在");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("活动不存在");
		}
	}

	private static long parsePathActivityIdForUpdate(String activityId) {
		String t = activityId == null ? "" : activityId.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("活动ID无效");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID无效");
		}
	}

	/** Returns true if {@code params} contains {@code key} and the value is non-null. */
	private static boolean paramKeyPresent(Map<String, Object> params, String key) {
		if (!params.containsKey(key)) {
			return false;
		}
		return params.get(key) != null;
	}

	/**
	 * Update-path helper: returns 1 only when the parameter is the string {@code "1"} or {@code "true"};
	 * missing key, null, or any other value yields 0. Does not use {@link #normalize01}.
	 */
	private static int normalizeStringOnlyTrueOrOne01(String key, Map<String, Object> params) {
		if (!params.containsKey(key) || params.get(key) == null) {
			return 0;
		}
		Object o = params.get(key);
		if (o instanceof String s) {
			if ("1".equals(s)) {
				return 1;
			}
			if ("true".equals(s)) {
				return 1;
			}
		}
		return 0;
	}

	private void validateUpdateActivityParams(
			LinkedHashMap<String, Object> params, int ifRelativeJoin, int ifShareLimitfee) {
		requireString(params, "name", "请输入活动名称");
		requireString(params, "title", "请输入活动标题");
		requireLong(params, "pages_template_id", "请选择活动首页关联模版");
		requireString(params, "pic", "请上传活动图片");
		requireString(params, "share_pic", "请上传活动分享图片");
		requireString(params, "list_pic", "请上传活动列表海报");

		List<Long> enterpriseIds = parseEnterpriseIdList(params.get("enterprise_id"));
		if (enterpriseIds.isEmpty()) {
			throw new BadRequestException("请选择参与企业");
		}

		requireInt(params, "display_time", "请选择活动预热时间");
		requireInt(params, "employee_begin_time", "请选择员工购买开始时间");
		requireInt(params, "employee_end_time", "请选择员工购买结束时间");
		requireInt(params, "employee_limitfee", "请输入员工可使用额度");

		if (ifRelativeJoin == 1) {
			requireInt(params, "invite_limit", "请输入员工可邀请亲友人数上限");
			requireInt(params, "relative_begin_time", "请选择亲友购买开始时间");
			requireInt(params, "relative_end_time", "请选择亲友购买结束时间");
		}

		if (ifShareLimitfee == 0) {
			requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
		}

		requireInt(params, "minimum_amount", "请填写订单最低金额");
		requireInt(
				params, "close_modify_hours_after_activity", "请填写活动结束后多少小时内可以修改收货地址");
	}

	private void validateUpdateActivityParamsNewContract(
			LinkedHashMap<String, Object> params,
			int ifRelativeJoin,
			int ifShareLimitfee,
			String purchaseMode) {
		requireString(params, "name", "请输入活动名称");
		requireString(params, "title", "请输入活动标题");
		requireLong(params, "pages_template_id", "请选择活动首页关联模版");
		requireString(params, "pic", "请上传活动图片");
		requireString(params, "share_pic", "请上传活动分享图片");
		requireString(params, "list_pic", "请上传活动列表海报");

		List<Long> enterpriseIds = parseEnterpriseIdList(params.get("enterprise_id"));
		if (enterpriseIds.isEmpty()) {
			throw new BadRequestException("请选择参与企业");
		}

		requireInt(params, "display_time", "请选择活动预热时间");
		requireInt(params, "employee_begin_time", "请选择员工购买开始时间");
		requireInt(params, "employee_end_time", "请选择员工购买结束时间");

		if (PurchaseModeSupport.isPrepaidPoint(purchaseMode)) {
			if (ifRelativeJoin == 1) {
				throw new BadRequestException("预充点数购买方式不支持亲友购");
			}
		} else if (ifRelativeJoin == 1) {
			requireInt(params, "invite_limit", "请输入员工可邀请亲友人数上限");
			requireInt(params, "relative_begin_time", "请选择亲友购买开始时间");
			requireInt(params, "relative_end_time", "请选择亲友购买结束时间");
			if (ifShareLimitfee == 0) {
				requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
			}
		} else if (ifShareLimitfee == 0) {
			requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
		}

		requireInt(params, "minimum_amount", "请填写订单最低金额");
		requireInt(
				params, "close_modify_hours_after_activity", "请填写活动结束后多少小时内可以修改收货地址");
	}

	private void validateFields(LinkedHashMap<String, Object> params, int ifRelativeJoin, int ifShareLimitfee) {
		requireString(params, "name", "请输入活动名称");
		requireString(params, "title", "请输入活动标题");
		requireLong(params, "pages_template_id", "请选择活动首页关联模版");
		requireString(params, "pic", "请上传活动图片");
		requireString(params, "share_pic", "请上传活动分享图片");
		requireString(params, "list_pic", "请上传活动列表海报");

		List<Long> enterpriseIds = parseEnterpriseIdList(params.get("enterprise_id"));
		if (enterpriseIds.isEmpty()) {
			throw new BadRequestException("请选择参与企业");
		}

		requireInt(params, "display_time", "请选择活动预热时间");
		requireInt(params, "employee_begin_time", "请选择员工购买开始时间");
		requireInt(params, "employee_end_time", "请选择员工购买结束时间");
		requireInt(params, "employee_limitfee", "请输入员工可使用额度");

		if (ifRelativeJoin == 1) {
			requireInt(params, "invite_limit", "请输入员工可邀请亲友人数上限");
			requireInt(params, "relative_begin_time", "请选择亲友购买开始时间");
			requireInt(params, "relative_end_time", "请选择亲友购买结束时间");
		}

		if (ifShareLimitfee == 0) {
			requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
		}

		requireInt(params, "minimum_amount", "请填写订单最低金额");
		requireInt(params, "close_modify_hours_after_activity", "请填写活动结束后多少小时内可以修改收货地址");
		requirePriceDisplayConfigPresent(params);
	}

	private void validateFieldsNewContract(
			LinkedHashMap<String, Object> params,
			int ifRelativeJoin,
			int ifShareLimitfee,
			String purchaseMode) {
		requireString(params, "name", "请输入活动名称");
		requireString(params, "title", "请输入活动标题");
		requireLong(params, "pages_template_id", "请选择活动首页关联模版");
		requireString(params, "pic", "请上传活动图片");
		requireString(params, "share_pic", "请上传活动分享图片");
		requireString(params, "list_pic", "请上传活动列表海报");

		List<Long> enterpriseIds = parseEnterpriseIdList(params.get("enterprise_id"));
		if (enterpriseIds.isEmpty()) {
			throw new BadRequestException("请选择参与企业");
		}
		if (!params.containsKey("enterprise_configs") || params.get("enterprise_configs") == null) {
			throw new BadRequestException("请配置企业额度信息");
		}

		requireInt(params, "display_time", "请选择活动预热时间");
		requireInt(params, "employee_begin_time", "请选择员工购买开始时间");
		requireInt(params, "employee_end_time", "请选择员工购买结束时间");

		if (PurchaseModeSupport.isPrepaidPoint(purchaseMode)) {
			if (ifRelativeJoin == 1) {
				throw new BadRequestException("预充点数购买方式不支持亲友购");
			}
		} else if (ifRelativeJoin == 1) {
			requireInt(params, "invite_limit", "请输入员工可邀请亲友人数上限");
			requireInt(params, "relative_begin_time", "请选择亲友购买开始时间");
			requireInt(params, "relative_end_time", "请选择亲友购买结束时间");
			if (ifShareLimitfee == 0) {
				requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
			}
		} else if (ifShareLimitfee == 0) {
			requireInt(params, "relative_limitfee", "请填写亲友可使用额度");
		}

		requireInt(params, "minimum_amount", "请填写订单最低金额");
		requireInt(params, "close_modify_hours_after_activity", "请填写活动结束后多少小时内可以修改收货地址");
		requirePriceDisplayConfigPresent(params);
	}

	private static boolean hasPurchaseModeInRequest(Map<String, Object> params) {
		if (!params.containsKey("purchase_mode") || params.get("purchase_mode") == null) {
			return false;
		}
		return StringUtils.hasText(PurchaseModeSupport.normalize(params.get("purchase_mode")));
	}

	private static String requirePurchaseMode(Map<String, Object> params) {
		String mode = PurchaseModeSupport.normalize(params.get("purchase_mode"));
		if (!PurchaseModeSupport.isValid(mode)) {
			throw new BadRequestException("购买方式仅支持 cash 或 prepaid_point");
		}
		return mode;
	}

	private static void rejectRelativeConfigForPrepaid(Map<String, Object> params) {
		if (truthyRelativeFlag(params.get("if_relative_join"))) {
			throw new BadRequestException("预充点数购买方式不支持亲友购");
		}
		if (paramKeyPresent(params, "invite_limit") && isNonZeroNumber(params.get("invite_limit"))) {
			throw new BadRequestException("预充点数购买方式不支持亲友购");
		}
		if (paramKeyPresent(params, "relative_begin_time")
				&& isNonZeroNumber(params.get("relative_begin_time"))) {
			throw new BadRequestException("预充点数购买方式不支持亲友购");
		}
		if (paramKeyPresent(params, "relative_end_time")
				&& isNonZeroNumber(params.get("relative_end_time"))) {
			throw new BadRequestException("预充点数购买方式不支持亲友购");
		}
		if (paramKeyPresent(params, "relative_limitfee")
				&& isNonZeroNumber(params.get("relative_limitfee"))) {
			throw new BadRequestException("预充点数购买方式不支持亲友购");
		}
	}

	private static boolean truthyRelativeFlag(Object o) {
		if (o == null) {
			return false;
		}
		if (Boolean.TRUE.equals(o)) {
			return true;
		}
		if (o instanceof Number n) {
			return n.intValue() == 1;
		}
		String s = o.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static boolean isNonZeroNumber(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) != 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static int readIntOrDefault(Object o, int def) {
		if (o == null) {
			return def;
		}
		if (o instanceof String s && s.trim().isEmpty()) {
			return def;
		}
		try {
			if (o instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int normalize01(String key, Map<String, Object> params) {
		if (!params.containsKey(key) || params.get(key) == null) {
			return 0;
		}
		Object o = params.get(key);
		if (Boolean.TRUE.equals(o)) {
			return 1;
		}
		if (o instanceof Number n && n.intValue() == 1) {
			return 1;
		}
		if (o instanceof String s) {
			if ("1".equals(s)) {
				return 1;
			}
			if ("true".equals(s)) {
				return 1;
			}
		}
		return 0;
	}

	private static int intFrom01(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private static List<Long> parseEnterpriseIdList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String s = o.toString().trim();
				if (s.isEmpty()) {
					continue;
				}
				try {
					if (o instanceof Number n) {
						out.add(n.longValue());
					} else {
						out.add(Long.parseLong(s));
					}
				} catch (NumberFormatException e) {
					throw new BadRequestException("请选择参与企业");
				}
			}
			return out;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return List.of();
		}
		try {
			if (raw instanceof Number n) {
				return List.of(n.longValue());
			}
			return List.of(Long.parseLong(s));
		} catch (NumberFormatException e) {
			throw new BadRequestException("请选择参与企业");
		}
	}

	private static String requireString(Map<String, Object> p, String key, String err) {
		Object o = p.get(key);
		if (o == null) {
			throw new BadRequestException(err);
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException(err);
		}
		return s;
	}

	private static int requireInt(Map<String, Object> p, String key, String err) {
		Object o = p.get(key);
		if (o == null) {
			throw new BadRequestException(err);
		}
		if (o instanceof String s && s.trim().isEmpty()) {
			throw new BadRequestException(err);
		}
		try {
			if (o instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(err);
		}
	}

	private static long requireLong(Map<String, Object> p, String key, String err) {
		Object o = p.get(key);
		if (o == null) {
			throw new BadRequestException(err);
		}
		if (o instanceof String s && s.trim().isEmpty()) {
			throw new BadRequestException(err);
		}
		try {
			if (o instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(err);
		}
	}

	private static void requirePriceDisplayConfigPresent(Map<String, Object> p) {
		Object o = p.get("price_display_config");
		if (o == null) {
			throw new BadRequestException("请设置活动价格展示");
		}
		if (o instanceof String s) {
			if (s.trim().isEmpty()) {
				throw new BadRequestException("请设置活动价格展示");
			}
			return;
		}
		if (o instanceof Map<?, ?> || o instanceof List<?>) {
			return;
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new BadRequestException("请设置活动价格展示");
		}
	}

	private JsonNode parsePriceDisplayConfigNode(Object raw) {
		if (raw == null) {
			return null;
		}
		try {
			if (raw instanceof String s) {
				String t = s.trim();
				if (t.isEmpty()) {
					return null;
				}
				return objectMapper.readTree(t);
			}
			if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
				return objectMapper.valueToTree(raw);
			}
			return objectMapper.readTree(raw.toString());
		} catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> toResponseMap(Activities e, List<Long> enterpriseIdsResponse) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("operator_id", e.getOperatorId());
		m.put("name", e.getName());
		m.put("title", e.getTitle());
		m.put("pages_template_id", e.getPagesTemplateId());
		m.put("pic", e.getPic());
		m.put("share_pic", e.getSharePic());
		m.put("list_pic", e.getListPic());
		m.put("enterprise_id", new ArrayList<>(enterpriseIdsResponse));
		m.put("display_time", e.getDisplayTime());
		m.put("employee_begin_time", e.getEmployeeBeginTime());
		m.put("employee_end_time", e.getEmployeeEndTime());

		boolean newContract = PurchaseModeSupport.isNewContractActivity(e);
		if (newContract) {
			m.put("purchase_mode", e.getPurchaseMode());
			m.put("purchase_mode_desc", PurchaseModeSupport.desc(e.getPurchaseMode()));
		} else {
			m.put("employee_limitfee", e.getEmployeeLimitfee());
		}

		boolean ifRel = Boolean.TRUE.equals(e.getIfRelativeJoin());
		m.put("if_relative_join", ifRel);
		if (ifRel) {
			m.put("invite_limit", e.getInviteLimit());
			m.put("relative_begin_time", e.getRelativeBeginTime());
			m.put("relative_end_time", e.getRelativeEndTime());
			m.put("if_share_limitfee", e.getIfShareLimitfee());
		} else {
			m.put("invite_limit", e.getInviteLimit() != null ? e.getInviteLimit() : 0);
			m.put("relative_begin_time", null);
			m.put("relative_end_time", null);
			m.put(
					"if_share_limitfee",
					Boolean.TRUE.equals(e.getIfShareLimitfee()) ? 1 : 0);
		}
		m.put("relative_limitfee", e.getRelativeLimitfee());
		m.put("minimum_amount", e.getMinimumAmount());
		m.put("close_modify_hours_after_activity", e.getCloseModifyHoursAfterActivity());
		m.put("status", e.getStatus());
		m.put("if_share_store", e.getIfShareStore());
		m.put("price_display_config", parsePriceConfigForResponse(e.getPriceDisplayConfig()));
		m.put("is_discount_description_enabled", e.getIsDiscountDescriptionEnabled());
		m.put("discount_description", e.getDiscountDescription());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		if (newContract) {
			boolean passphraseEnabled = Boolean.TRUE.equals(e.getIsPassphraseEnabled());
			m.put("is_passphrase_enabled", passphraseEnabled ? "true" : "false");
			m.put(
					"enterprise_configs",
					enterpriseConfigService.buildResponseList(
							e.getCompanyId() == null ? 0L : e.getCompanyId(),
							e.getId() == null ? 0L : e.getId(),
							passphraseEnabled));
		} else {
			activityPassphraseAdminService.appendAdminPassphraseFields(e, m);
		}
		return m;
	}

	private Object parsePriceConfigForResponse(String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(json, Object.class);
		} catch (Exception e) {
			return null;
		}
	}

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(co);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	private static long toLong(Object co) {
		if (co instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(co.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long readDistributorId(Map<String, Object> operatorJwt) {
		Object v = operatorJwt.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}

	private static int readOperatorId(Map<String, Object> operatorJwt) {
		Object o = operatorJwt.get("operator_id");
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int toIntBounded(long v) {
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}
}
