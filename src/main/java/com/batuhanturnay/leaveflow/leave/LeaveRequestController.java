package com.batuhanturnay.leaveflow.leave;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.batuhanturnay.leaveflow.leave.dto.LeaveDecisionRequest;
import com.batuhanturnay.leaveflow.leave.dto.LeaveRequestCreateRequest;
import com.batuhanturnay.leaveflow.leave.dto.LeaveRequestResponse;

@Tag(name = "Leave requests", description = "Submitting, approving, rejecting and cancelling leave")
@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

	private final LeaveRequestService leaveRequestService;

	public LeaveRequestController(LeaveRequestService leaveRequestService) {
		this.leaveRequestService = leaveRequestService;
	}

	/**
	 * A manager's approval queue: {@code GET /api/leave-requests?managerId=1&status=PENDING}
	 */
	@Operation(summary = "Search leave requests (yours and your team's; HR sees all)")
	@GetMapping
	public Page<LeaveRequestResponse> list(@RequestParam(required = false) Long employeeId,
			@RequestParam(required = false) Long managerId, @RequestParam(required = false) LeaveStatus status,
			@RequestParam(required = false) LeaveType type,
			@ParameterObject @PageableDefault(size = 20, sort = "startDate",
					direction = Sort.Direction.DESC) Pageable pageable) {
		return leaveRequestService.search(employeeId, managerId, status, type, pageable);
	}

	@Operation(summary = "Get a leave request")
	@GetMapping("/{id}")
	public LeaveRequestResponse get(@PathVariable Long id) {
		return leaveRequestService.findById(id);
	}

	@Operation(summary = "Submit a leave request for yourself (starts as PENDING)")
	@PostMapping
	public ResponseEntity<LeaveRequestResponse> create(@Valid @RequestBody LeaveRequestCreateRequest request) {
		LeaveRequestResponse created = leaveRequestService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@Operation(summary = "Approve a pending request (direct manager; HR for employees without a manager)")
	@PostMapping("/{id}/approve")
	public LeaveRequestResponse approve(@PathVariable Long id,
			@Valid @RequestBody(required = false) LeaveDecisionRequest decision) {
		return leaveRequestService.approve(id, decision != null ? decision : LeaveDecisionRequest.EMPTY);
	}

	@Operation(summary = "Reject a pending request with a note (direct manager; HR for employees without a manager)")
	@PostMapping("/{id}/reject")
	public LeaveRequestResponse reject(@PathVariable Long id, @Valid @RequestBody LeaveDecisionRequest decision) {
		return leaveRequestService.reject(id, decision);
	}

	@Operation(summary = "Cancel your pending request, or an approved one that has not started yet")
	@PostMapping("/{id}/cancel")
	public LeaveRequestResponse cancel(@PathVariable Long id) {
		return leaveRequestService.cancel(id);
	}

}
