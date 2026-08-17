package com.nforceone.nforcehq.finance;

import java.time.Instant;
import java.util.UUID;

public record FinanceChatMessageView(UUID id, UUID userId, String userName, String message, Instant createdAt) {
}
