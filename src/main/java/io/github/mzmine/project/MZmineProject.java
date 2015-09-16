/*
 * Copyright 2006-2015 The MZmine 3 Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 3; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import io.github.msdk.datamodel.featuretables.FeatureTable;
import io.github.msdk.datamodel.featuretables.Sample;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.mzmine.gui.mainwindow.RawDataTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Simple implementation of the MZmineProject interface.
 */
public class MZmineProject {

    private static final Image rawDataFilesIcon = new Image("icons/xicicon.png");
    private static final Image peakListsIcon = new Image("icons/peaklistsicon.png");
    private static final Image groupIcon = new Image("icons/groupicon.png");
    private static final Image fileIcon = new Image("icons/fileicon.png");
    private static final Image peakListIcon = new Image(
            "icons/peaklisticon_single.png");

    private @Nullable File projectFile;

    private final TreeItem<RawDataTreeItem> rawDataRootItem;

    private final List<FeatureTable> featureTables = new ArrayList<>();

    public MZmineProject() {
        rawDataRootItem = new TreeItem<>(new RawDataTreeItem());
        rawDataRootItem.setGraphic(new ImageView(rawDataFilesIcon));
        rawDataRootItem.setExpanded(true);
    }

    @Nullable
    public File getProjectFile() {
        return projectFile;
    }

    public void setProjectFile(@Nullable File projectFile) {
        this.projectFile = projectFile;
    }

    public TreeItem<RawDataTreeItem> getRawDataRootItem() {
        return rawDataRootItem;
    }

    @SuppressWarnings("null")

    @Nonnull
    public List<Sample> getSamples() {
        final ArrayList<Sample> allSamples = new ArrayList<>();
        synchronized (featureTables) {
            for (FeatureTable peakList : featureTables) {
                for (Sample s : peakList.getSamples()) {
                    if (!allSamples.contains(s))
                        allSamples.add(s);
                }
            }
        }
        return ImmutableList.copyOf(allSamples);
    }

    public void addFile(final RawDataFile rawDataFile) {
        RawDataTreeItem wrap = new RawDataTreeItem(rawDataFile);
        TreeItem<RawDataTreeItem> df1 = new TreeItem<>(wrap);
        df1.setGraphic(new ImageView(fileIcon));
        rawDataRootItem.getChildren().add(df1);
    }

    public void removeFile(final RawDataFile rawDataFile) {
        for (TreeItem<?> df1 : rawDataRootItem.getChildren()) {
            if (df1.getValue() == rawDataFile) {
                rawDataRootItem.getChildren().remove(df1);
                break;
            }
        }
    }

    @SuppressWarnings("null")
    @Nonnull
    public List<RawDataFile> getRawDataFiles() {
        List<RawDataFile> dataFiles = new ArrayList<>();
        for (TreeItem<?> df1 : rawDataRootItem.getChildren()) {
            if (df1.getValue() instanceof RawDataFile) {
                dataFiles.add((RawDataFile) df1.getValue());
            }
        }
        return ImmutableList.copyOf(dataFiles);
    }

    public void addFeatureTable(FeatureTable featureTable) {
        synchronized (featureTables) {
            featureTables.add(featureTable);
        }
    }

    public void removeFeatureTable(FeatureTable featureTable) {
        synchronized (featureTables) {
            featureTables.remove(featureTable);
        }
    }

    @SuppressWarnings("null")

    @Nonnull
    public List<FeatureTable> getFeatureTables() {
        final List<FeatureTable> snapShot;
        synchronized (featureTables) {
            snapShot = ImmutableList.copyOf(featureTables);
        }
        return snapShot;
    }

}