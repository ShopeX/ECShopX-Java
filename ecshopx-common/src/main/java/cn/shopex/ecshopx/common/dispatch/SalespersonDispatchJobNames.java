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

package cn.shopex.ecshopx.common.dispatch;

public final class SalespersonDispatchJobNames {

	public static final String SALESPERSON_TASK_JOB =
			"job:45:SalespersonBundle\\Jobs\\SalespersonTask";

	public static final String SALESPERSON_RELATIONSHIP_CONTINUITY_JOB =
			"job:46:SalespersonBundle\\Jobs\\SalespersonRelationshipContinuity";

	public static final String SALESPERSON_ITEMS_SHELVES_JOB =
			"job:138:SalespersonBundle\\Jobs\\SalespersonItemsShelvesJob";

	/**
	 * Readable alias for the package-promotion <strong>update</strong> trigger row in the async job
	 * inventory ({@code job:139}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}—same bus
	 * message name and single handler registration as the create trigger ({@code job:138}).
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB139_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the package-promotion <strong>cancel</strong> trigger row in the async job
	 * inventory ({@code job:140}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}—same bus
	 * message name and single handler registration as the create trigger ({@code job:138}).
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB140_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the marketing-activity <strong>create</strong> trigger row in the async job
	 * inventory ({@code job:141}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}—same bus
	 * message name and single handler registration as the other trigger rows.
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB141_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the marketing-activity <strong>update</strong> trigger row in the async job
	 * inventory ({@code job:142}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}—same bus
	 * message name and single handler registration as the other trigger rows.
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB142_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the marketing-activity <strong>physical delete</strong> (waiting-only) trigger row
	 * in the async job inventory ({@code job:144}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}—same
	 * bus message name and single handler registration as {@code job:138}.
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB144_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the marketing-activity <strong>end</strong> ({@code isEnd}) trigger row in the async
	 * job inventory ({@code job:145}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}—same bus message name
	 * and single handler registration as {@code job:138}.
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB145_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the seckill-activity <strong>create</strong> trigger row in the async job
	 * inventory ({@code job:146}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}.
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB146_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the seckill-activity <strong>update</strong> trigger row in the async job
	 * inventory ({@code job:147}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}.
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB147_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	/**
	 * Readable alias for the seckill-activity <strong>manual end</strong> ({@code updateStatus}) trigger row in the
	 * async job inventory ({@code job:149}); identical to {@link #SALESPERSON_ITEMS_SHELVES_JOB}.
	 */
	public static final String SALESPERSON_ITEMS_SHELVES_JOB_CSV_JOB149_ALIAS = SALESPERSON_ITEMS_SHELVES_JOB;

	private SalespersonDispatchJobNames() {
	}
}
