/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Crypto Morin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ist.alchm.smthsHub.utility.reflection;

import ist.alchm.smthsHub.SmthsHubPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.logging.Level;

/*
 * References
 *
 * * * GitHub: https://github.com/CryptoMorin/XSeries/blob/master/Titles.java
 * * XSeries: https://www.spigotmc.org/threads/378136/
 * PacketPlayOutTitle: https://wiki.vg/Protocol#Title
 *
 */

/**
 * A reflection API for titles in Minecraft.
 * Fully optimized - Supports 1.8.8+ and above.
 * Requires ReflectionUtils.
 * Messages are not colorized by default.
 * <p>
 * Titles are text messages that appear in the
 * middle of the players screen: <a href="https://minecraft.gamepedia.com/Commands/title">...</a>
 * PacketPlayOutTitle: <a href="https://wiki.vg/Protocol#Title">...</a>
 *
 * @author Crypto Morin
 * @version 1.0.0
 * @see ReflectionUtils
 */
public class Titles {

    private static final JavaPlugin PLUGIN = JavaPlugin.getProvidingPlugin(SmthsHubPlugin.class);

    private static final Object TIMES;
    private static final Object TITLE;
    private static final Object SUBTITLE;
    private static final Object CLEAR;

    private static final MethodHandle PACKET;
    private static final MethodHandle CHAT_COMPONENT_TEXT;

    static {
        Class<?> chatComponentText = ReflectionUtils.getNMSClass("ChatComponentText");
        Class<?> packet = ReflectionUtils.getNMSClass("PacketPlayOutTitle");
        assert packet != null;
        Class<?> titleTypes = packet.getDeclaredClasses()[0];
        MethodHandle packetCtor = null;
        MethodHandle chatComp = null;

        Object times = null;
        Object title = null;
        Object subtitle = null;
        Object clear = null;

        for (Object type : titleTypes.getEnumConstants()) {
            switch (type.toString()) {
                case "TIMES" -> times = type;
                case "TITLE" -> title = type;
                case "SUBTITLE" -> subtitle = type;
                case "CLEAR" -> clear = type;
            }
        }

        try {
            assert chatComponentText != null;
            chatComp = MethodHandles.lookup().findConstructor(chatComponentText,
                    MethodType.methodType(void.class, String.class));

            packetCtor = MethodHandles.lookup().findConstructor(packet,
                    MethodType.methodType(void.class, titleTypes,
                            ReflectionUtils.getNMSClass("IChatBaseComponent"),
                            int.class, int.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            PLUGIN.getLogger().log(Level.SEVERE, "Failed to initialize Titles packet constructors", e);
        }

        TITLE = title;
        SUBTITLE = subtitle;
        TIMES = times;
        CLEAR = clear;

        PACKET = packetCtor;
        CHAT_COMPONENT_TEXT = chatComp;
    }

    public static void sendTitle(Player player,
                                 int fadeIn, int stay, int fadeOut,
                                 String title, String subtitle) {
        Objects.requireNonNull(player, "Cannot send title to null player");
        if (title == null && subtitle == null) return;

        try {
            Object timesPacket = PACKET.invoke(TIMES, CHAT_COMPONENT_TEXT.invoke(title), fadeIn, stay, fadeOut);
            ReflectionUtils.sendPacket(player, timesPacket);

            if (title != null) {
                Object titlePacket = PACKET.invoke(TITLE, CHAT_COMPONENT_TEXT.invoke(title), fadeIn, stay, fadeOut);
                ReflectionUtils.sendPacket(player, titlePacket);
            }
            if (subtitle != null) {
                Object subtitlePacket = PACKET.invoke(SUBTITLE, CHAT_COMPONENT_TEXT.invoke(subtitle), fadeIn, stay, fadeOut);
                ReflectionUtils.sendPacket(player, subtitlePacket);
            }
        } catch (Throwable throwable) {
            PLUGIN.getLogger().log(Level.SEVERE, "Failed to send title to player " + player.getName(), throwable);
        }
    }

    public static void clearTitle(Player player) {
        Objects.requireNonNull(player, "Cannot clear title from null player");
        Object clearPacket = null;

        try {
            clearPacket = PACKET.invoke(CLEAR, null, -1, -1, -1);
        } catch (Throwable throwable) {
            PLUGIN.getLogger().log(Level.SEVERE, "Failed to create clear title packet for player " + player.getName(), throwable);
        }

        ReflectionUtils.sendPacket(player, clearPacket);
    }
}
