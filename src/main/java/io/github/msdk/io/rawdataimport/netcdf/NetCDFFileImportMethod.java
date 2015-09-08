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

package io.github.msdk.io.rawdataimport.netcdf;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.datapointstore.DataPointStore;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.msspectra.MsSpectrumDataPointList;
import io.github.msdk.datamodel.msspectra.MsSpectrumType;
import io.github.msdk.datamodel.rawdata.ChromatographyInfo;
import io.github.msdk.datamodel.rawdata.MsFunction;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.datamodel.rawdata.RawDataFileType;
import io.github.msdk.datamodel.rawdata.SeparationType;
import io.github.msdk.io.spectrumtypedetection.SpectrumTypeDetectionMethod;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NetCDFFileImportMethod implements MSDKMethod<RawDataFile> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private NetcdfFile inputFile;

    private int parsedScans;
    private int totalScans = 0, numberOfGoodScans, scanNum = 0;

    private int scanStartPositions[];
    private Hashtable<Integer, Double> scansRetentionTimes;

    private final @Nonnull File sourceFile;
    private final @Nonnull RawDataFileType fileType = RawDataFileType.NETCDF;
    private final @Nonnull DataPointStore dataStore;

    private RawDataFile newRawFile;
    private boolean canceled = false;

    private Variable massValueVariable, intensityValueVariable;

    // Some software produces netcdf files with a scale factor such as 0.05
    private double massValueScaleFactor = 1;
    private double intensityValueScaleFactor = 1;

    private final @Nonnull MsSpectrumDataPointList dataPoints = MSDKObjectBuilder
            .getMsSpectrumDataPointList();

    public NetCDFFileImportMethod(@Nonnull File sourceFile,
            @Nonnull DataPointStore dataStore) {
        this.sourceFile = sourceFile;
        this.dataStore = dataStore;
    }

    @Override
    public RawDataFile execute() throws MSDKException {

        logger.info("Started parsing file " + sourceFile);

        @Nonnull
        String fileName = sourceFile.getName();
        newRawFile = MSDKObjectBuilder.getRawDataFile(fileName, sourceFile,
                fileType, dataStore);

        try {

            // Open NetCDF-file
            inputFile = NetcdfFile.open(sourceFile.getPath());

            // Read NetCDF variables
            readVariables();

            // Parse scans
            MsScan buildingScan;
            while ((buildingScan = readNextScan()) != null) {

                // Check if cancel is requested
                if (canceled) {
                    return null;
                }
                newRawFile.addScan(buildingScan);
                parsedScans++;

            }

            // Close file
            inputFile.close();

        } catch (Throwable e) {
            throw new MSDKException(e);
        }

        logger.info("Finished parsing " + sourceFile + ", parsed " + parsedScans
                + " scans");

        return newRawFile;

    }

    private void readVariables() throws IOException {

        /*
         * DEBUG: dump all variables for (Variable v : inputFile.getVariables())
         * { System.out.println("variable " + v.getShortName()); }
         */

        // Find mass_values and intensity_values variables
        massValueVariable = inputFile.findVariable("mass_values");
        if (massValueVariable == null) {
            logger.error("Could not find variable mass_values");
            throw (new IOException("Could not find variable mass_values"));
        }
        assert(massValueVariable.getRank() == 1);

        Attribute massScaleFacAttr = massValueVariable
                .findAttribute("scale_factor");
        if (massScaleFacAttr != null) {
            massValueScaleFactor = massScaleFacAttr.getNumericValue()
                    .doubleValue();
        }

        intensityValueVariable = inputFile.findVariable("intensity_values");
        if (intensityValueVariable == null) {
            logger.error("Could not find variable intensity_values");
            throw (new IOException("Could not find variable intensity_values"));
        }
        assert(intensityValueVariable.getRank() == 1);

        Attribute intScaleFacAttr = intensityValueVariable
                .findAttribute("scale_factor");
        if (intScaleFacAttr != null) {
            intensityValueScaleFactor = intScaleFacAttr.getNumericValue()
                    .doubleValue();
        }

        // Read number of scans
        Variable scanIndexVariable = inputFile.findVariable("scan_index");
        if (scanIndexVariable == null) {
            throw (new IOException(
                    "Could not find variable scan_index from file "
                            + sourceFile));
        }
        totalScans = scanIndexVariable.getShape()[0];

        // Read scan start positions
        // Extra element is required, because element totalScans+1 is used to
        // find the stop position for last scan
        int[] scanStartPositions = new int[totalScans + 1];

        Array scanIndexArray = scanIndexVariable.read();
        IndexIterator scanIndexIterator = scanIndexArray.getIndexIterator();
        int ind = 0;
        while (scanIndexIterator.hasNext()) {
            scanStartPositions[ind] = ((Integer) scanIndexIterator.next());
            ind++;
        }
             

        // Calc stop position for the last scan
        // This defines the end index of the last scan
        scanStartPositions[totalScans] = (int) massValueVariable.getSize();

        // Start scan RT
        float retentionTimes[] = new float[totalScans];
        Variable scanTimeVariable = inputFile
                .findVariable("scan_acquisition_time");
        if (scanTimeVariable == null) {
            throw (new IOException(
                    "Could not find variable scan_acquisition_time from file "
                            + sourceFile));
        }
        Array scanTimeArray = null;
        scanTimeArray = scanTimeVariable.read();
        IndexIterator scanTimeIterator = scanTimeArray.getIndexIterator();
        ind = 0;
        while (scanTimeIterator.hasNext()) {
            if (scanTimeVariable.getDataType()
                    .getPrimitiveClassType() == float.class) {
                retentionTimes[ind] = (Float) (scanTimeIterator.next());
            }
            if (scanTimeVariable.getDataType()
                    .getPrimitiveClassType() == double.class) {
                retentionTimes[ind] = ((Double) scanTimeIterator.next()).floatValue();
            }
            ind++;
        }
        // End scan RT

        // Cleanup
        scanTimeIterator = null;
        scanTimeArray = null;
        scanTimeVariable = null;

        // Fix problems caused by new QStar data converter
        // assume scan is missing when scan_index[i]<0
        // for these scans, fix variables:
        // - scan_acquisition_time: interpolate/extrapolate using times of
        // present scans
        // - scan_index: fill with following good value

        // Calculate number of good scans
        numberOfGoodScans = 0;
        for (int i = 0; i < totalScans; i++) {
            if (scanStartPositions[i] >= 0) {
                numberOfGoodScans++;
            }
        }

        // Is there need to fix something?
        if (numberOfGoodScans < totalScans) {

            // Fix scan_acquisition_time
            // - calculate average delta time between present scans
            double sumDelta = 0;
            int n = 0;
            for (int i = 0; i < totalScans; i++) {
                // Is this a present scan?
                if (scanStartPositions[i] >= 0) {
                    // Yes, find next present scan
                    for (int j = i + 1; j < totalScans; j++) {
                        if (scanStartPositions[j] >= 0) {
                            sumDelta += (retentionTimes[j] - retentionTimes[i])
                                    / ((double) (j - i));
                            n++;
                            break;
                        }
                    }
                }
            }
            double avgDelta = sumDelta / (double) n;
            // - fill missing scan times using nearest good scan and avgDelta
            for (int i = 0; i < totalScans; i++) {
                // Is this a missing scan?
                if (scanStartPositions[i] < 0) {
                    // Yes, find nearest present scan
                    int nearestI = Integer.MAX_VALUE;
                    for (int j = 1; 1 < 2; j++) {
                        if ((i + j) < totalScans) {
                            if (scanStartPositions[i + j] >= 0) {
                                nearestI = i + j;
                                break;
                            }
                        }
                        if ((i - j) >= 0) {
                            if (scanStartPositions[i - j] >= 0) {
                                nearestI = i + j;
                                break;
                            }
                        }

                        // Out of bounds?
                        if (((i + j) >= totalScans) && ((i - j) < 0)) {
                            break;
                        }
                    }

                    if (nearestI != Integer.MAX_VALUE) {

                        retentionTimes[i] = (float) (retentionTimes[nearestI]
                                + (i - nearestI) * avgDelta);

                    } else {
                        if (i > 0) {
                            retentionTimes[i] = retentionTimes[i - 1];
                        } else {
                            retentionTimes[i] = 0;
                        }
                        logger.error(
                                "ERROR: Could not fix incorrect QStar scan times.");
                    }
                }
            }

            // Fix scanStartPositions by filling gaps with next good value
            for (int i = 0; i < totalScans; i++) {
                if (scanStartPositions[i] < 0) {
                    for (int j = i + 1; j < (totalScans + 1); j++) {
                        if (scanStartPositions[j] >= 0) {
                            scanStartPositions[i] = scanStartPositions[j];
                            break;
                        }
                    }
                }
            }
        }

    }

    /**
     * Reads one scan from the file. Requires that general information has
     * already been read.
     * 
     * @throws MSDKException
     */
    private MsScan readNextScan() throws IOException, MSDKException {

        // Set scan number
        scanNum++;

        // Set a simple MS function, always MS level 1 for netCDF data
        MsFunction msFunction = MSDKObjectBuilder.getMsFunction(1);

        MsScan scan = MSDKObjectBuilder.getMsScan(dataStore, scanNum,
                msFunction);

        // End of file
        if (startAndLength == null) {
            System.out.println("scan #" + scanNum);
            return null;
        }

        // Get retention time of the scan
        Double retentionTime = scansRetentionTimes.get(scanNum);
        if (retentionTime == null) {
            logger.error("Could not find retention time for scan " + scanNum);
            throw (new MSDKException(
                    "Could not find retention time for scan " + scanNum));
        }

        // Read mass and intensity values
        final int scanStartPosition[] = { scanStartPositions[i] };
        final int scanLength[] = {  scanStartPositions[i + 1]
                - scanStartPositions[i] };
        Array massValueArray;
        Array intensityValueArray;
            massValueArray = massValueVariable.read(scanStartPosition,
                    scanLength);
            intensityValueArray = intensityValueVariable.read(scanStartPosition,
                    scanLength);
        
        Index massValuesIndex = massValueArray.getIndex();
        Index intensityValuesIndex = intensityValueArray.getIndex();

        int arrayLength = massValueArray.getShape()[0];

        dataPoints.clear();
        dataPoints.allocate(arrayLength);
        double mzValues[] = dataPoints.getMzBuffer();
        float intValues[] = dataPoints.getIntensityBuffer();

        for (int j = 0; j < arrayLength; j++) {
            Index massIndex0 = massValuesIndex.set0(j);
            Index intensityIndex0 = intensityValuesIndex.set0(j);

            mzValues[j] = massValueArray.getDouble(massIndex0)
                    * massValueScaleFactor;
            intValues[j] = (float) (intensityValueArray
                    .getDouble(intensityIndex0) * intensityValueScaleFactor);
        }

        // Store the data points
        scan.setDataPoints(dataPoints);

        // Auto-detect whether this scan is centroided
        SpectrumTypeDetectionMethod detector = new SpectrumTypeDetectionMethod(
                scan);
        MsSpectrumType spectrumType = detector.execute();
        scan.setSpectrumType(spectrumType);

        // TODO set correct separation type from global netCDF file attributes
        ChromatographyInfo chromData = MSDKObjectBuilder
                .getChromatographyInfo1D(SeparationType.UNKNOWN,
                        retentionTime.floatValue());
        scan.setChromatographyInfo(chromData);
        
        return scan;

    }

    private void extractDataPoints(MsSpectrumDataPointList dataPoints) {

    }

    @Override
    @Nullable
    public RawDataFile getResult() {
        return newRawFile;
    }

    @Override
    public Float getFinishedPercentage() {
        return totalScans == 0 ? null : (float) parsedScans / totalScans;
    }

    @Override
    public void cancel() {
        this.canceled = true;
    }

}