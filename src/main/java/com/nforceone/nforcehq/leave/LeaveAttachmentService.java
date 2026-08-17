package com.nforceone.nforcehq.leave;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.ApprovalScope;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveAttachmentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final LeaveAttachmentRepository leaveAttachmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    public List<LeaveAttachmentView> list(JwtPrincipal principal, UUID leaveRequestId) {
        LeaveRequest request = requireVisible(principal, leaveRequestId);
        return leaveAttachmentRepository
                .findByOrganizationIdAndLeaveRequestIdOrderByCreatedAtAsc(request.getOrganizationId(), leaveRequestId)
                .stream().map(LeaveAttachmentView::from).toList();
    }

    @Transactional
    public LeaveAttachmentView upload(JwtPrincipal principal, UUID leaveRequestId, MultipartFile file) {
        LeaveRequest request = requireOwner(principal, leaveRequestId);
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment file is empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + file.getContentType());
        }

        LeaveAttachment attachment = new LeaveAttachment();
        attachment.setOrganizationId(request.getOrganizationId());
        attachment.setLeaveRequestId(leaveRequestId);
        attachment.setUploadedBy(principal.userId());
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize((int) file.getSize());
        try {
            attachment.setFileData(file.getBytes());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }
        leaveAttachmentRepository.save(attachment);
        return LeaveAttachmentView.from(attachment);
    }

    public LeaveAttachment download(JwtPrincipal principal, UUID leaveRequestId, UUID attachmentId) {
        LeaveRequest request = requireVisible(principal, leaveRequestId);
        return leaveAttachmentRepository
                .findByIdAndOrganizationIdAndLeaveRequestId(attachmentId, request.getOrganizationId(), leaveRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
    }

    @Transactional
    public void delete(JwtPrincipal principal, UUID leaveRequestId, UUID attachmentId) {
        LeaveRequest request = requireOwner(principal, leaveRequestId);
        LeaveAttachment attachment = leaveAttachmentRepository
                .findByIdAndOrganizationIdAndLeaveRequestId(attachmentId, request.getOrganizationId(), leaveRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
        leaveAttachmentRepository.delete(attachment);
    }

    private LeaveRequest requireOwner(JwtPrincipal principal, UUID leaveRequestId) {
        UUID organizationId = requireOrganization(principal);
        LeaveRequest request = leaveRequestRepository.findByIdAndOrganizationId(leaveRequestId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave request not found"));
        if (!request.getUserId().equals(principal.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage attachments on your own leave requests");
        }
        return request;
    }

    private LeaveRequest requireVisible(JwtPrincipal principal, UUID leaveRequestId) {
        UUID organizationId = requireOrganization(principal);
        LeaveRequest request = leaveRequestRepository.findByIdAndOrganizationId(leaveRequestId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave request not found"));
        if (request.getUserId().equals(principal.userId())) {
            return request;
        }
        List<UUID> scope = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (!scope.contains(request.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This leave request is outside your view");
        }
        return request;
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
