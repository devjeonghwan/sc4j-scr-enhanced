package org.fogofwarlabs.sc4j.screnhanced.net;

import org.fogofwarlabs.sc4j.beaconlink.client.BeaconLinkClient;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.DatagramLink;
import org.fogofwarlabs.sc4j.engine.net.lobby.MapFolders;
import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile;
import org.fogofwarlabs.sc4j.scr.game.frame.GameSpeed;
import org.fogofwarlabs.sc4j.scr.net.beaconlink.BeaconLinkProvider;
import org.fogofwarlabs.sc4j.scr.net.discovery.LanDiscovery;
import org.fogofwarlabs.sc4j.scr.net.discovery.LanRecord;
import org.fogofwarlabs.sc4j.scr.net.lan.*;
import org.fogofwarlabs.sc4j.scr.net.provider.ProviderSession;
import org.fogofwarlabs.sc4j.scr.net.proxy.GameProxySocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Checks the map download of SC:R Enhanced's lobbies ({@link LinkMapExtension}): the lobby has SC:R Enhanced's own LAN join version, a lobby
 * on another network than Beacon.Link keeps the chunks of message {@code 0x4f}, and in a Beacon.Link room on 127.0.0.1 a joiner downloads a
 * large map through the engine's transfer while the lobby turns run (the host sees the joiner's download percent rise) and chat lines sent
 * during the download arrive, with both clients running their network once a frame.
 */
final class LinkMapExtensionTest {
    private static final int ENHANCED_JOIN_VERSION = 0xE005;
    private static final @NotNull String GAME_NAME = "s4jbeaconmap";
    private static final @NotNull String MAP_NAME = "large map.scx";
    private static final int MAP_SIZE = 36_000_000;
    private static final long MAP_SEED = 13L;

    /**
     * The time between two network runs of a client: one frame.
     */
    private static final long FRAME_MILLISECONDS = 16L;
    /**
     * The longest real time of each phase: finding the room, joining it and downloading the map.
     */
    private static final long PHASE_LIMIT_MILLISECONDS = 120_000L;
    /**
     * The time between two chat lines of the joiner during the download, and the longest time a chat line may take to the host.
     */
    private static final long CHAT_INTERVAL_MILLISECONDS = 500L;
    private static final long CHAT_LIMIT_MILLISECONDS = 1000L;
    /**
     * The start of the joiner's chat lines, followed by the line's number.
     */
    private static final @NotNull String CHAT_PREFIX = "chat ";
    private static final int COMPLETE_PERCENT = 100;
    /**
     * The most game packets the joiner may receive during the download: the chunks of message {@code 0x4f} would be more than {@code 70000}.
     */
    private static final int PACKET_LIMIT = 10_000;

    /**
     * The scenario of the hosted game: a 128x128 map of two open slots of selectable race.
     */
    private static final int MAP_DIMENSION = 128;
    private static final int VERSION = 205;
    private static final int SLOTS = 12;
    private static final int OPEN_SLOTS = 2;
    private static final int SELECTABLE_RACE = 5;
    private static final int FORCE_SECTION_SIZE = 20;
    private static final int SECTION_HEADER_SIZE = 8;

    /**
     * Holds a network without games whose game socket's datagrams go nowhere.
     */
    private static final class SilentNetwork implements NetworkProvider {
        @Override
        public @NotNull List<LanDiscovery.Game> getGames() {
            return List.of();
        }

        @Override
        public LanDiscovery.@Nullable Game getConnectedGame() {
            return null;
        }

        @Override
        public ProviderSession.@Nullable JoinState getConnectionFailure() {
            return null;
        }

        @Override
        public boolean isReadyToStart() {
            return true;
        }

        @Override
        public @Nullable String getFailureText() {
            return null;
        }

        @Override
        public void setPresence(@NotNull LanRecord record) {
        }

        @Override
        public void setGame(@NotNull LanRecord record, int port) {
        }

        @Override
        public void withdraw(@NotNull String name) {
        }

        @Override
        public @NotNull GameProxySocket openGameSocket(int port) {
            return GameProxySocket.open(new DatagramLink<>() {
                @Override
                public void send(@NotNull ByteBuffer datagram, @NotNull InetSocketAddress target) {
                }

                @Override
                public @Nullable InetSocketAddress receive(@NotNull ByteBuffer buffer) {
                    return null;
                }

                @Override
                public void close() {
                }
            }, port);
        }

        @Override
        public void connect(LanDiscovery.@NotNull Game game) {
        }

        @Override
        public void update() {
        }

        @Override
        public void close() {
        }
    }

    /**
     * Drives the clients in real time: the protocol clock follows the real time, the nodes and sessions are polled, and every client runs its
     * network once a frame.
     */
    static final class Driver {
        private final @NotNull ClientScenario link;
        private final @NotNull List<@NotNull LanClient> clients;
        private final long realStart;
        private final long protocolStart;

        Driver(@NotNull ClientScenario link, @NotNull List<@NotNull LanClient> clients) {
            this.link          = link;
            this.clients       = clients;
            this.realStart     = System.nanoTime();
            this.protocolStart = link.getClock().getEpochMillis();
        }

        long getElapsedMilliseconds() {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.realStart);
        }

        void runUntil(@NotNull BooleanSupplier condition, @NotNull Runnable frame, @NotNull String description) throws IOException,
                                                                                                                       InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(PHASE_LIMIT_MILLISECONDS);

            while (!condition.getAsBoolean()) {
                Assertions.assertTrue(System.nanoTime() < deadline, () -> "not reached in time: " + description);

                long target = this.protocolStart + this.getElapsedMilliseconds();

                if (target > this.link.getClock().getEpochMillis()) {
                    this.link.getClock().advance(target - this.link.getClock().getEpochMillis());
                }

                this.link.pollAll();

                for (LanClient client : this.clients) {
                    client.update();
                }

                frame.run();
                Thread.sleep(FRAME_MILLISECONDS);
            }
        }
    }

    private static void writeSection(@NotNull ByteArrayOutputStream output, @NotNull String name, byte @NotNull [] data) {
        ByteBuffer header = ByteBuffer.allocate(SECTION_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        header.put(name.getBytes(StandardCharsets.US_ASCII)).putInt(data.length);
        output.writeBytes(header.array());
        output.writeBytes(data);
    }

    /**
     * Creates the scenario of a hosted game whose first slots are open.
     */
    static @NotNull ChkFile createScenario(int openSlots) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] owners = new byte[SLOTS];
        byte[] races = new byte[SLOTS];
        ByteBuffer version = ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer dimensions = ByteBuffer.allocate(Short.BYTES * 2).order(ByteOrder.LITTLE_ENDIAN);

        for (int slot = 0; slot < openSlots; slot++) {
            owners[slot] = (byte) LanLobby.TYPE_OPEN;
            races[slot]  = (byte) SELECTABLE_RACE;
        }

        version.putShort((short) VERSION);
        dimensions.putShort((short) MAP_DIMENSION).putShort((short) MAP_DIMENSION);

        writeSection(output, ChkFile.VERSION_SECTION, version.array());
        writeSection(output, ChkFile.DIMENSIONS_SECTION, dimensions.array());
        writeSection(output, ChkFile.TILESET_SECTION, new byte[Short.BYTES]);
        writeSection(output, ChkFile.OWNER_SECTION, owners);
        writeSection(output, ChkFile.RACE_SECTION, races);
        writeSection(output, ChkFile.FORCE_SECTION, new byte[FORCE_SECTION_SIZE]);

        return ChkFile.parseTolerant(output.toByteArray());
    }

    /**
     * Opens a Beacon.Link session on 127.0.0.1, waits until it registers at the node and opens a LAN client of a map folder on it.
     */
    private static @NotNull LanClient openClient(@NotNull ClientScenario link, @NotNull PeerSocket node, @NotNull ModIdentity identity,
                                                 @NotNull String name, @NotNull Path maps) throws IOException {
        BeaconLinkClient session = link.openClient(identity, List.of(ClientScenario.listed(node)));

        link.await(() -> session.getObservedAddress() != null, "the session registers");

        return LanClient.open(name, new BeaconLinkProvider(session), MapFolders.of(List.of(maps), LanMapTransfer.DOWNLOAD_FOLDER));
    }

    static @NotNull LanLobby requireLobby(@NotNull LanClient client) {
        LanLobby lobby = client.getLobby();

        Assertions.assertNotNull(lobby);

        return lobby;
    }

    @Test
    void lobby_on_another_network_keeps_the_chunks(@TempDir @NotNull Path folder) throws Exception {
        Path mapPath = Files.write(folder.resolve(MAP_NAME), new byte[]{1, 2, 3});

        try (LanClient client = LanClient.open("host", new SilentNetwork(), MapFolders.of(List.of(folder), LanMapTransfer.DOWNLOAD_FOLDER))) {
            client.host(GAME_NAME, LanClient.HostedMap.read(mapPath, createScenario(OPEN_SLOTS), MAP_NAME), LanGameOptions.MELEE, GameSpeed.FASTEST);

            Assertions.assertEquals(ENHANCED_JOIN_VERSION, LanLobby.JOIN_VERSION, "SC:R Enhanced's own LAN join version");
            Assertions.assertNull(requireLobby(client).getMapTransfer().getDownloader());
        }
    }

    @Test
    void joiner_in_a_beacon_link_room_downloads_a_large_map_through_the_transfer(@TempDir @NotNull Path folder) throws Exception {
        byte[] contents = new byte[MAP_SIZE];

        new Random(MAP_SEED).nextBytes(contents);

        Path hostMaps = Files.createDirectories(folder.resolve("host"));
        Path joinerMaps = Files.createDirectories(folder.resolve("joiner"));
        Path mapPath = Files.write(hostMaps.resolve(MAP_NAME), contents);
        List<LanClient> clients = new ArrayList<>();

        try (ClientScenario link = new ClientScenario()) {
            ModIdentity identity = ClientScenario.createIdentity("1.0.0");
            PeerSocket node = link.openNode(ClientScenario.createNodeConfiguration());
            Driver driver = new Driver(link, clients);

            try {
                LanClient host = openClient(link, node, identity, "host", hostMaps);

                clients.add(host);
                host.host(GAME_NAME, LanClient.HostedMap.read(mapPath, createScenario(OPEN_SLOTS), MAP_NAME), LanGameOptions.MELEE,
                          GameSpeed.FASTEST);

                Assertions.assertInstanceOf(LinkMapDownloader.class, requireLobby(host).getMapTransfer().getDownloader());

                LanClient joiner = openClient(link, node, identity, "joiner", joinerMaps);
                LanDiscovery.Game[] listed = new LanDiscovery.Game[1];

                clients.add(joiner);
                driver.runUntil(() -> {
                    for (LanDiscovery.Game game : joiner.getGames()) {
                        if (game.getInformation().getName().equals(GAME_NAME)) {
                            listed[0] = game;
                        }
                    }

                    return listed[0] != null;
                }, () -> {
                }, "the joiner lists the room");
                joiner.join(listed[0]);
                driver.runUntil(() -> {
                    LanLobby lobby = joiner.getLobby();

                    return joiner.getState() == LanClient.State.LOBBY && lobby != null && lobby.getState() >= LanLobby.STATE_LOBBY;
                }, () -> {
                }, "the joiner reaches the lobby");

                LanLobby hostLobby = requireLobby(host);
                LanLobby joinerLobby = requireLobby(joiner);
                ProviderSession joinerSession = joiner.getSession();

                Assertions.assertNotNull(joinerSession);

                int joinerPlayer = joinerSession.getLocalIdentifier();
                int packetsBefore = joiner.getPacketsIn();
                List<Integer> percents = new ArrayList<>();
                List<Long> sentChats = new ArrayList<>();
                List<Long> chatDelays = new ArrayList<>();
                long start = driver.getElapsedMilliseconds();
                BooleanSupplier complete = () -> hostLobby.isMapTransferComplete() && joinerLobby.getMapFile() != null &&
                                                 hostLobby.getDownloadPercent(joinerPlayer) == COMPLETE_PERCENT;

                driver.runUntil(complete, () -> {
                    long now = driver.getElapsedMilliseconds();
                    int percent = hostLobby.getDownloadPercent(joinerPlayer);

                    if (percent >= 0 && (percents.isEmpty() || percents.getLast() != percent)) {
                        percents.add(percent);
                    }

                    if (joinerLobby.getMapFile() == null && now - start >= CHAT_INTERVAL_MILLISECONDS * (sentChats.size() + 1)) {
                        Assertions.assertTrue(joinerLobby.sendChat(CHAT_PREFIX + sentChats.size()));
                        sentChats.add(now);
                    }

                    for (LanLobby.Chat chat = hostLobby.pollChat(); chat != null; chat = hostLobby.pollChat()) {
                        chatDelays.add(now - sentChats.get(Integer.parseInt(chat.getText().substring(CHAT_PREFIX.length()))));
                    }

                    Assertions.assertFalse(hostLobby.isTimedOut() || joinerLobby.isTimedOut(), "the lobby turns go on");
                }, "the joiner downloads the map");

                long elapsed = driver.getElapsedMilliseconds() - start;
                int packets = joiner.getPacketsIn() - packetsBefore;
                Path downloaded = joinerLobby.getMapFile();

                System.out.println("BEACON.LINK MAP DOWNLOAD: " + MAP_SIZE + " bytes in " + elapsed + " ms, joiner game packets in " + packets +
                                   ", chat delays " + chatDelays + ", percents " + percents);

                Assertions.assertNotNull(downloaded);
                Assertions.assertEquals(joinerMaps.resolve(LanMapTransfer.DOWNLOAD_FOLDER).resolve(MAP_NAME), downloaded);
                Assertions.assertArrayEquals(contents, Files.readAllBytes(downloaded));
                Assertions.assertTrue(packets < PACKET_LIMIT, "the map did not come in chunks of message 0x4f: " + packets + " packets");
                Assertions.assertEquals(sentChats.size(), chatDelays.size(), "every chat line arrives");
                Assertions.assertTrue(chatDelays.stream().allMatch(delay -> delay <= CHAT_LIMIT_MILLISECONDS), chatDelays::toString);
                Assertions.assertEquals(COMPLETE_PERCENT, percents.getLast());
            } finally {
                for (LanClient client : clients) {
                    client.close();
                }
            }
        }
    }
}
