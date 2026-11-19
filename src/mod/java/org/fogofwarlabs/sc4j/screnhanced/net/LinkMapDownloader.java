package org.fogofwarlabs.sc4j.screnhanced.net;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkParticipant;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkTransferService;
import org.fogofwarlabs.sc4j.engine.net.transfer.FileTransferSource;
import org.fogofwarlabs.sc4j.engine.net.transfer.TransferDownload;
import org.fogofwarlabs.sc4j.engine.net.transfer.TransferKey;
import org.fogofwarlabs.sc4j.engine.net.transfer.TransferState;
import org.fogofwarlabs.sc4j.scr.net.beaconlink.BeaconLinkProvider;
import org.fogofwarlabs.sc4j.scr.net.lan.LanMapDownloader;
import org.fogofwarlabs.sc4j.scr.net.lan.LanMapTransfer;
import org.fogofwarlabs.sc4j.scr.net.provider.ProviderPeer;
import org.fogofwarlabs.sc4j.scr.net.provider.ProviderSession;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads the maps of a lobby in a Beacon.Link room through the engine's transfer of payloads ({@link LinkTransferService}) on the room's
 * overlay: every map the local player shares is offered under the key of its size and checksum, and a missing map is requested under that key
 * from its owner. A download that fails, or that the owner does not accept within {@value #ANSWER_MILLIS} ms, goes back to the chunks of
 * message {@code 0x4f}.
 */
final class LinkMapDownloader implements LanMapDownloader {
    /**
     * The time a request may wait for the owner's acceptance, in milliseconds.
     */
    static final long ANSWER_MILLIS = 5000L;

    private static final @NotNull Logger LOGGER = Logger.of(LinkMapDownloader.class);
    private static final @NotNull String KEY_PREFIX = "scr-map ";

    /**
     * One map being downloaded.
     */
    private static final class MapDownload {
        private final LanMapTransfer.@NotNull Transfer transfer;
        private final @NotNull MapTransferSink sink;
        private final @NotNull TransferDownload<LinkParticipant> download;
        private final long startMillis;

        private MapDownload(LanMapTransfer.@NotNull Transfer transfer, @NotNull MapTransferSink sink,
                            @NotNull TransferDownload<LinkParticipant> download, long startMillis) {
            this.transfer    = transfer;
            this.sink        = sink;
            this.download    = download;
            this.startMillis = startMillis;
        }
    }

    /**
     * Creates the key a map is offered under.
     *
     * @param size     the map's size
     * @param checksum the map's checksum
     * @return the created {@link TransferKey} instance
     */
    static @NotNull TransferKey createKey(int size, int checksum) {
        return TransferKey.of(KEY_PREFIX + Integer.toUnsignedString(size) + " " + Integer.toHexString(checksum));
    }


    private final @NotNull BeaconLinkProvider network;
    private final @NotNull ProviderSession session;
    private final @NotNull LanMapTransfer mapTransfer;
    private final @NotNull NetworkClock clock;
    private final @NotNull LinkTransferService service;

    private final @NotNull List<@NotNull FileTransferSource> sources;
    private final @NotNull List<@NotNull MapDownload> downloads;
    private boolean closed;

    /**
     * Creates a {@link LinkMapDownloader} instance on a Beacon.Link network: it opens the transfer port of the network's overlay.
     *
     * @param network     the network
     * @param session     the lobby's provider session
     * @param mapTransfer the lobby's map transfer
     * @param clock       the clock of the transfers
     * @throws IllegalStateException if the transfer port is open already
     */
    LinkMapDownloader(@NotNull BeaconLinkProvider network, @NotNull ProviderSession session, @NotNull LanMapTransfer mapTransfer,
                      @NotNull NetworkClock clock) {
        this.network     = network;
        this.session     = session;
        this.mapTransfer = mapTransfer;
        this.clock       = clock;
        this.service     = new LinkTransferService(network.getBeaconLinkSession().getOverlay(), clock);

        this.sources   = new ArrayList<>();
        this.downloads = new ArrayList<>();
        this.closed    = false;
    }

    /**
     * Offers a shared map under the key of its size and checksum; a map that cannot be read is not offered, and the other players' requests
     * of it are refused.
     *
     * @param map the shared map
     */
    @Override
    public void share(LanMapTransfer.@NotNull SharedFile map) {
        if (this.closed) {
            return;
        }

        try {
            FileTransferSource source = FileTransferSource.open(map.getPath());

            this.sources.add(source);
            this.service.offer(createKey(map.getSize(), map.getChecksum()), source);
        } catch (IOException exception) {
            LOGGER.warning("BEACON.LINK MAP NOT OFFERED: {0}: {1}", map.getPath(), exception.getMessage());
        }
    }

    /**
     * Requests a missing map from its owner, when the owner is a participant of the room.
     *
     * @param transfer the transfer of the map
     * @return {@code true} if the map is requested, {@code false} if the owner is no participant of the room
     */
    @Override
    public boolean download(LanMapTransfer.@NotNull Transfer transfer) {
        ProviderPeer owner = this.session.findPeer(transfer.getOwner());
        InetSocketAddress address = owner == null ? null : owner.getAddress();
        LinkParticipant participant = address == null ? null : this.network.findParticipant(address);

        if (this.closed || participant == null) {
            return false;
        }

        MapTransferSink sink = new MapTransferSink(transfer.getSize());
        TransferKey key = createKey(transfer.getSize(), transfer.getChecksum());

        this.downloads.add(new MapDownload(transfer, sink, this.service.request(participant, key, sink), this.clock.getMillis()));
        LOGGER.info("BEACON.LINK MAP DOWNLOAD: {0} ({1} bytes) from player {2}", transfer.getName(), Integer.toUnsignedString(transfer.getSize()),
                    transfer.getOwner());

        return true;
    }

    /**
     * Runs the transfers, reports the downloads' progress, hands a complete map to the lobby's transfer and a failed one back to the chunks.
     */
    @Override
    public void update() {
        if (this.closed) {
            return;
        }

        try {
            this.service.update();
        } catch (IOException exception) {
            LOGGER.warning("BEACON.LINK MAP TRANSFER STOPPED: {0}", exception.getMessage());
            this.close();

            for (MapDownload map : this.downloads) {
                this.mapTransfer.downloadNatively(map.transfer);
            }

            this.downloads.clear();

            return;
        }

        long nowMillis = this.clock.getMillis();

        for (MapDownload map : List.copyOf(this.downloads)) {
            TransferDownload<LinkParticipant> download = map.download;

            if (download.getState() == TransferState.REQUESTING && nowMillis - map.startMillis >= ANSWER_MILLIS) {
                download.cancel();
            }

            if (download.getState() == TransferState.COMPLETE) {
                this.downloads.remove(map);
                this.mapTransfer.completeDownload(map.transfer, map.sink.getBytes());
            } else if (download.isDone()) {
                LOGGER.info("BEACON.LINK MAP DOWNLOAD: {0} {1} ({2}), downloading it in chunks", map.transfer.getName(), download.getState(),
                            download.getFailure());
                this.downloads.remove(map);
                this.mapTransfer.downloadNatively(map.transfer);
            } else {
                this.mapTransfer.reportProgress(map.transfer, (int) download.getReceivedBytes());
            }
        }
    }

    /**
     * Ends the transfers, closes the transfer port and the shared maps. Calling it again does nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        this.service.close();

        for (FileTransferSource source : this.sources) {
            try {
                source.close();
            } catch (IOException exception) {
                LOGGER.debug("BEACON.LINK MAP NOT CLOSED: {0}", exception.getMessage());
            }
        }

        this.sources.clear();
    }
}
