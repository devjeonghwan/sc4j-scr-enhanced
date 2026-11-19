package org.fogofwarlabs.sc4j.screnhanced.net;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.fogofwarlabs.sc4j.scr.net.beaconlink.BeaconLinkProvider;
import org.fogofwarlabs.sc4j.scr.net.extension.ScrNetworkExtension;
import org.fogofwarlabs.sc4j.scr.net.lan.LanClient;
import org.fogofwarlabs.sc4j.scr.net.lan.LanLobby;
import org.fogofwarlabs.sc4j.scr.net.lan.LanMapDownloader;
import org.fogofwarlabs.sc4j.scr.net.provider.ProviderSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Downloads the maps of a lobby in a Beacon.Link room through the engine's transfer of payloads ({@link LinkMapDownloader}). Every player of
 * such a lobby runs SC:R Enhanced: the room list holds the rooms of the mod's own identity, and the host refuses a joiner of another LAN join
 * version ({@link LanLobby#JOIN_VERSION}, SC:R Enhanced's own). A lobby on the local network keeps the chunks of message {@code 0x4f}.
 */
public final class LinkMapExtension implements ScrNetworkExtension {
    private static final @NotNull Logger LOGGER = Logger.of(LinkMapExtension.class);

    /**
     * Creates the map downloader of a lobby in a Beacon.Link room.
     *
     * @param client the client, with its network and session
     * @param lobby  the lobby
     * @return the created {@link LinkMapDownloader} instance, or {@code null} on another network or when the transfer cannot run
     */
    @Override
    public @Nullable LanMapDownloader createMapDownloader(@NotNull LanClient client, @NotNull LanLobby lobby) {
        ProviderSession session = client.getSession();

        if (!(client.getNetwork() instanceof BeaconLinkProvider network) || session == null) {
            return null;
        }

        try {
            return new LinkMapDownloader(network, session, lobby.getMapTransfer(), NetworkClock.SYSTEM);
        } catch (IllegalStateException | LinkageError exception) {
            LOGGER.warning("BEACON.LINK MAP TRANSFER NOT AVAILABLE: {0}", exception.toString());

            return null;
        }
    }
}
