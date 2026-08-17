package com.nforceone.nforcehq.user;

import java.util.Base64;

final class AvatarUtil {

    private AvatarUtil() {
    }

    static String dataUri(User user) {
        if (user.getAvatarPhoto() == null || user.getAvatarContentType() == null) {
            return null;
        }
        return "data:" + user.getAvatarContentType() + ";base64," + Base64.getEncoder().encodeToString(user.getAvatarPhoto());
    }
}
