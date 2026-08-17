package com.nforceone.nforcehq.leave;

import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveAttachmentService leaveAttachmentService;

    @GetMapping("/api/leave-types")
    public List<LeaveTypeSummary> leaveTypes(@AuthenticationPrincipal JwtPrincipal principal) {
        return leaveService.listTypes(principal);
    }

    @GetMapping("/api/leave/balances/me")
    public List<LeaveBalanceView> myBalances(@AuthenticationPrincipal JwtPrincipal principal) {
        return leaveService.balances(principal);
    }

    @PostMapping("/api/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestView apply(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody ApplyLeaveRequest request) {
        return leaveService.apply(principal, request);
    }

    @GetMapping("/api/me/requests/history")
    public List<RequestHistoryItem> history(@AuthenticationPrincipal JwtPrincipal principal) {
        return leaveService.history(principal);
    }

    @GetMapping("/api/leave/team-calendar")
    public List<TeamCalendarEntry> teamCalendar(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "30") int range) {
        return leaveService.teamCalendar(principal, range);
    }

    @PostMapping("/api/leave-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public LeaveRequestView approveLeave(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return leaveService.approve(principal, id);
    }

    @PostMapping("/api/leave-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public LeaveRequestView rejectLeave(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return leaveService.reject(principal, id);
    }

    @GetMapping("/api/leave-requests/{id}/attachments")
    public List<LeaveAttachmentView> listAttachments(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return leaveAttachmentService.list(principal, id);
    }

    @PostMapping("/api/leave-requests/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveAttachmentView uploadAttachment(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return leaveAttachmentService.upload(principal, id, file);
    }

    @GetMapping("/api/leave-requests/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        LeaveAttachment attachment = leaveAttachmentService.download(principal, id, attachmentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(attachment.getFileName()).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(attachment.getFileData());
    }

    @DeleteMapping("/api/leave-requests/{id}/attachments/{attachmentId}")
    public void deleteAttachment(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        leaveAttachmentService.delete(principal, id, attachmentId);
    }
}
