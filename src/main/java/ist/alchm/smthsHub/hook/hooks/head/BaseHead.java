package ist.alchm.smthsHub.hook.hooks.head;

import com.cryptomorin.xseries.XMaterial;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.hook.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaseHead implements PluginHook, HeadHook {

    private Map<String, ItemStack> cache;

    @Override
    public void onEnable(SmthsHubPlugin plugin) {
        cache = new HashMap<>();
    }

    private static final UUID RANDOM_UUID = UUID.fromString("92864445-51c5-4c3b-9039-517c9927d1b4");

    public ItemStack getHead(String data) {
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        setBase64ToSkullMeta(data, meta);
        head.setItemMeta(meta);
        return head;
    }

    private static PlayerProfile getProfileBase64(String base64) {
        PlayerProfile profile = Bukkit.createPlayerProfile(RANDOM_UUID); // Get a new player profile
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = getUrlFromBase64(base64);
        } catch (MalformedURLException | URISyntaxException exception) {
            throw new RuntimeException("Invalid URL", exception);
        }

        textures.setSkin(urlObject); // Set the skin of the player profile to the URL
        profile.setTextures(textures); // Set the textures back to the profile
        return profile;
    }

    private static URL getUrlFromBase64(String base64) throws MalformedURLException, URISyntaxException {
        String decoded = new String(Base64.getDecoder().decode(base64));
        String urlString = decoded.substring("{\"textures\":{\"SKIN\":{\"url\":\"".length(), decoded.length() - "\"}}}".length());
        return new URI(urlString).toURL();
    }

    private static void setBase64ToSkullMeta(String base64, SkullMeta meta) {
        meta.setOwnerProfile(getProfileBase64(base64));
    }
}
