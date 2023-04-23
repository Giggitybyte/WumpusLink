package me.thegiggitybyte.wumpuslink.utils;

import java.net.URL;
import java.util.UUID;

public class Utils {
    public static URL getMinecraftPlayerHeadUrl(UUID playerUuid) {
        return createUrl("https://crafatar.com/renders/head/" + playerUuid + "?default=mhf_Steve&overlay");
    }

    public static String getMinecraftPlayerRender(UUID playerUuid) {
        return "https://crafatar.com/renders/body/" + playerUuid;
    }

    public static URL createUrl(String string) {
        try {
            return new URL(string); // Checked exceptions are for clowns.
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
