package org.fogofwarlabs.sc4j.screnhanced.net;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.net.transfer.TransferSink;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Takes a lobby map into memory ({@link TransferSink}): an array of the map's size, as the lobby's transfer holds a map.
 */
final class MapTransferSink implements TransferSink {
    private final byte @NotNull [] data;
    private int length;

    /**
     * Creates a {@link MapTransferSink} instance of a map's size.
     *
     * @param size the map's size in bytes
     */
    MapTransferSink(int size) {
        this.data   = new byte[size];
        this.length = 0;
    }

    /**
     * @return the bytes held: the map's array when it holds the whole map, else a copy of the bytes held
     */
    byte @NotNull [] getBytes() {
        return this.length == this.data.length ? this.data : Arrays.copyOf(this.data, this.length);
    }

    /**
     * @return the number of bytes held
     */
    @Override
    public long getLength() {
        return this.length;
    }

    /**
     * Reads held bytes into a buffer: as many as the buffer has room for, from its position to its limit.
     *
     * @param offset the offset of the first byte
     * @param target the buffer that receives the bytes
     * @throws IOException if the held bytes end before the buffer is full
     */
    @Override
    public void read(long offset, @NotNull ByteBuffer target) throws IOException {
        if (offset < 0 || offset + target.remaining() > this.length) {
            throw Exceptions.newIOException("The map holds {0} bytes, not {1} from {2}.", this.length, target.remaining(), offset);
        }

        target.put(this.data, (int) offset, target.remaining());
    }

    /**
     * Appends bytes after the held ones.
     *
     * @param data the bytes, read from their position to their limit
     * @throws IOException if the bytes do not fit the map's size
     */
    @Override
    public void write(@NotNull ByteBuffer data) throws IOException {
        int count = data.remaining();

        if (count > this.data.length - this.length) {
            throw Exceptions.newIOException("The map has {0} bytes, {1} more do not fit after {2}.", this.data.length, count, this.length);
        }

        data.get(this.data, this.length, count);
        this.length += count;
    }

    /**
     * Drops every held byte.
     */
    @Override
    public void truncate() {
        this.length = 0;
    }
}
