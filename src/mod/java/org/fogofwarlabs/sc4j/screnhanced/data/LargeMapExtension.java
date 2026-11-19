package org.fogofwarlabs.sc4j.screnhanced.data;

import org.fogofwarlabs.sc4j.scr.data.extension.ScrDataExtension;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchive;

/**
 * Lets the map reader accept a {@code staredit\scenario.chk} of any size an array holds ({@link MpqArchive#MAXIMUM_FILE_SIZE}) instead of the
 * native {@link MpqArchive#NATIVE_MAXIMUM_SCENARIO_SIZE}.
 */
public final class LargeMapExtension implements ScrDataExtension {
    /**
     * @return {@link MpqArchive#MAXIMUM_FILE_SIZE}
     */
    @Override
    public int getMaximumScenarioSize() {
        return MpqArchive.MAXIMUM_FILE_SIZE;
    }
}
