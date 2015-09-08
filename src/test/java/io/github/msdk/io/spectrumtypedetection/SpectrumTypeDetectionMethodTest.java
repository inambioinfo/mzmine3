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

package io.github.msdk.io.spectrumtypedetection;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.msdk.datamodel.datapointstore.DataPointStore;
import io.github.msdk.datamodel.datapointstore.DataPointStoreFactory;
import io.github.msdk.datamodel.msspectra.MsSpectrumType;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.io.rawdataimport.RawDataFileImportMethod;

public class SpectrumTypeDetectionMethodTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String TEST_DATA_PATH = "src/test/resources/spectrumtypedetection";

    /**
     * Test the SpectrumTypeDetectionAlgorithm
     */
    @Test
    public void testSpectrumTypeDetectionAlgorithm() throws Exception {

        File inputFiles[] = new File(TEST_DATA_PATH).listFiles();

        Assert.assertNotNull(inputFiles);
        Assert.assertNotEquals(0, inputFiles.length);

        int filesTested = 0;

        for (File inputFile : inputFiles) {

            MsSpectrumType expectedType;
            if (inputFile.getName().startsWith("centroided"))
                expectedType = MsSpectrumType.CENTROIDED;
            else if (inputFile.getName().startsWith("thresholded"))
                expectedType = MsSpectrumType.THRESHOLDED;
            else if (inputFile.getName().startsWith("profile"))
                expectedType = MsSpectrumType.PROFILE;
            else {
                throw new Exception(
                        "Cannot determine expected spectrum type of file "
                                + inputFile);
            }

            logger.info(
                    "Testing autodetection of centroided/thresholded/profile scans on file "
                            + inputFile.getName());

            // Use a temporary file to store data points
            DataPointStore dataStore = DataPointStoreFactory
                    .getTmpFileDataPointStore();

            RawDataFileImportMethod importer = new RawDataFileImportMethod(
                    inputFile, dataStore);
            RawDataFile rawFile = importer.execute();

            Assert.assertNotNull(rawFile);

            for (MsScan scan : rawFile.getScans()) {
                @SuppressWarnings("null")
                SpectrumTypeDetectionMethod detector = new SpectrumTypeDetectionMethod(
                        scan);
                detector.execute();
                MsSpectrumType detectedType = detector.getResult();
                Assert.assertNotNull(detectedType);

                Assert.assertEquals("Scan type wrongly detected for scan "
                        + scan.getScanNumber() + " in " + rawFile.getName(),
                        expectedType, detectedType);
            }

            // Dispose the data store when done
            rawFile.dispose();

            filesTested++;
        }

        // Make sure we tested 10+ files
        Assert.assertTrue(filesTested > 10);
    }

}