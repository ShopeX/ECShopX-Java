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

package cn.shopex.ecshopx.openapi.web;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiAssignMemberToSalespersonFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderWriteoffV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiNotifyBecomeFriendFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradesFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagLibraryPushFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagRelationPushFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberListCustomerUnionidsFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiKaquanV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiNotImplementedException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {
	"cn.shopex.ecshopx.openapi.thirdapi.v1",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.orders",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.member",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.items",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.distributor",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan"
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OpenapiExceptionHandler {

	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<OpenapiEnvelope> handleResource(ResourceException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(OpenapiErrorCode.BUSINESS_ERROR, ex.getMessage(), null));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<OpenapiEnvelope> handleUnauthorized(UnauthorizedException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(OpenapiErrorCode.SIGN_ERROR, ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiNotImplementedException.class)
	public ResponseEntity<OpenapiEnvelope> handleNotImplemented(OpenapiNotImplementedException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(OpenapiErrorCode.NOT_IMPLEMENTED, ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiLegacyZeroCodeFailException.class)
	public ResponseEntity<OpenapiEnvelope> handleLegacyZeroCodeFail(OpenapiLegacyZeroCodeFailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(OpenapiErrorCode.SUCCESS, ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiMemberListCustomerUnionidsFailException.class)
	public ResponseEntity<OpenapiEnvelope> handleCustomerUnionidsFail(
			OpenapiMemberListCustomerUnionidsFailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(OpenapiErrorCode.SYSTEM_ERROR, ex.getMessage(), List.of()));
	}

	@ExceptionHandler(OpenapiAssignMemberToSalespersonFailException.class)
	public ResponseEntity<OpenapiEnvelope> handleAssignMemberToSalespersonFail(
			OpenapiAssignMemberToSalespersonFailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiAftersalesV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleAftersalesV2Fail(
			OpenapiAftersalesV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiOrderDeliverV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleOrderDeliverV2Fail(
			OpenapiOrderDeliverV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiOrderCancelV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleOrderCancelV2Fail(
			OpenapiOrderCancelV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiOrderWriteoffV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleOrderWriteoffV2Fail(
			OpenapiOrderWriteoffV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiNotifyBecomeFriendFailException.class)
	public ResponseEntity<OpenapiEnvelope> handleNotifyBecomeFriendFail(
			OpenapiNotifyBecomeFriendFailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiMemberCardGradesFailException.class)
	public ResponseEntity<OpenapiEnvelope> handleMemberCardGradesFail(
			OpenapiMemberCardGradesFailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(OpenapiErrorCode.SYSTEM_ERROR, ex.getMessage(), List.of()));
	}

	@ExceptionHandler(OpenapiMemberTagLibraryPushFailException.class)
	public ResponseEntity<OpenapiEnvelope> handleMemberTagLibraryPushFail(
			OpenapiMemberTagLibraryPushFailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(OpenapiErrorCode.SYSTEM_ERROR, ex.getMessage(), List.of()));
	}

	@ExceptionHandler(OpenapiMemberTagRelationPushFailException.class)
	public ResponseEntity<OpenapiEnvelope> handleMemberTagRelationPushFail(
			OpenapiMemberTagRelationPushFailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(OpenapiMemberTagCategoryV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleMemberTagCategoryV2Fail(
			OpenapiMemberTagCategoryV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiMemberTagV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleMemberTagV2Fail(
			OpenapiMemberTagV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiMemberV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleMemberV2Fail(OpenapiMemberV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiDistributorV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleDistributorV2Fail(
			OpenapiDistributorV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiMemberRechargeV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleMemberRechargeV2Fail(
			OpenapiMemberRechargeV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiItemsV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleItemsV2Fail(OpenapiItemsV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}

	@ExceptionHandler(OpenapiKaquanV2FailException.class)
	public ResponseEntity<OpenapiEnvelope> handleKaquanV2Fail(OpenapiKaquanV2FailException ex) {
		return ResponseEntity.ok(
				OpenapiEnvelope.fail(ex.getOpenapiCode(), ex.getMessage(), null));
	}
}
