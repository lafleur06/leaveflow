package com.batuhanturnay.leaveflow.leave;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface LeaveRequestEventRepository extends JpaRepository<LeaveRequestEvent, Long> {

	@EntityGraph(attributePaths = "actor")
	List<LeaveRequestEvent> findByLeaveRequestIdOrderByOccurredAtAscIdAsc(Long leaveRequestId);

}
