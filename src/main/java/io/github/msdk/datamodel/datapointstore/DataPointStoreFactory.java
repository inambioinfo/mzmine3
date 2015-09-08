/* 
 * (C) Copyright 2015 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */

package io.github.msdk.datamodel.datapointstore;

import javax.annotation.Nonnull;

import io.github.msdk.MSDKException;

/**
 * Data store provider
 */
public class DataPointStoreFactory {

    private static final @Nonnull MemoryDataPointStore memoryDataStore = new MemoryDataPointStore();

    public static final @Nonnull DataPointStore getMemoryDataStore() {
        return memoryDataStore;
    }

    public static final @Nonnull DataPointStore getTmpFileDataPointStore()
            throws MSDKException {
        return new TmpFileDataPointStore();
    }

    public static final @Nonnull DataPointStore getNFSDBDataPointStore()
            throws MSDKException {
        return new NFSDBDataPointStore();
    }

}