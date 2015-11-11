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

package io.github.mzmine.parameters.parametertypes;

import org.controlsfx.control.PropertySheet;

import io.github.mzmine.parameters.ParameterEditor;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

/**
 * This parameter stores double values
 */
public class IntegerEditor extends BorderPane
        implements ParameterEditor<Integer> {

    private final TextField integerField;

    public IntegerEditor(PropertySheet.Item parameter) {
        if (!(parameter instanceof IntegerParameter))
            throw new IllegalArgumentException();

        this.integerField = new TextField();
        setCenter(integerField);
    }

    @Override
    public Node getEditor() {
        return this;
    }

    @Override
    public Integer getValue() {
        String stringValue = integerField.getText();
        try {
            Integer integerValue = Integer.parseInt(stringValue);
            return integerValue;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void setValue(Integer value) {
        if (value != null) {
            String stringValue = String.valueOf(value);
            integerField.setText(stringValue);
        }
    }

    @Override
    public Control getMainControl() {
        return integerField;
    }

}
