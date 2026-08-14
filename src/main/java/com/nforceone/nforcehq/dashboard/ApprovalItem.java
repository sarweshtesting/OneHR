package com.nforceone.nforcehq.dashboard;

/** type is one of: LEAVE, REGULARIZATION, MISMATCH. id is a UUID string for LEAVE/
 * REGULARIZATION, or the userId::workDate pseudo-id for MISMATCH. */
public record ApprovalItem(String type, String id, String userName, String title, String subtitle) {
}
