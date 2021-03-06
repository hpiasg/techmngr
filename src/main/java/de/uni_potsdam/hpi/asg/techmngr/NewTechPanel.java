package de.uni_potsdam.hpi.asg.techmngr;

/*
 * Copyright (C) 2017 Norman Kluge
 * 
 * This file is part of ASGtechmngr.
 * 
 * ASGtechmngr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ASGtechmngr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ASGtechmngr.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.gui.AbstractMainPanel;
import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel;
import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel.AbstractBooleanParam;
import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel.AbstractTextParam;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.common.technology.TechnologyDirectory;

public class NewTechPanel extends AbstractMainPanel {
    private static final long   serialVersionUID = 7635453181517878899L;
    private static final Logger logger           = LogManager.getLogger();

    private Window              parent;
    private Technology          tech;
    private TechnologyDirectory techDir;

    //@formatter:off
    public enum TextParam implements AbstractTextParam {
        /*edit*/ name, balsafolder, genlibfile, liberty, addInfo, searchpath, libraries, layouttcl
    }

    public enum BooleanParam implements AbstractBooleanParam {
    }
    //@formatter:on

    public NewTechPanel(Window parent, TechnologyDirectory techDir) {
        this.parent = parent;
        this.tech = null;
        this.techDir = techDir;

        constructEditPanel();
    }

    private void constructEditPanel() {
        PropertiesPanel editPanel = new PropertiesPanel(parent);
        this.add(editPanel);
        GridBagLayout gbl_editpanel = new GridBagLayout();
        gbl_editpanel.columnWidths = new int[]{170, 300, 0, 0, 40, 0};
        gbl_editpanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_editpanel.rowHeights = new int[]{15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 0};
        gbl_editpanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        editPanel.setLayout(gbl_editpanel);

        editPanel.addTextEntry(0, TextParam.name, "Name", "");
        editPanel.addTextEntry(1, TextParam.balsafolder, "Balsa technology directory", "", true, JFileChooser.DIRECTORIES_ONLY, false, true, "Choose the directory which contains the startup.scm");
        editPanel.addTextEntry(2, TextParam.genlibfile, "Genlib file", "", true, JFileChooser.FILES_ONLY, false);
        editPanel.addTextEntry(3, TextParam.liberty, "Liberty file", "", true, JFileChooser.FILES_ONLY, false);
        editPanel.addTextEntry(4, TextParam.addInfo, "Additional info file", "", true, JFileChooser.FILES_ONLY, false);

        editPanel.addTextEntry(6, TextParam.searchpath, "Search path", "", false, null, false, true, "While using Design Compiler this value is appended to 'search_path'");
        editPanel.addTextEntry(7, TextParam.libraries, "Libraries", "", false, null, false, true, "While using Design Compiler 'link_library' and 'target_library' are set to this value\n(Thus you can define multiple libraries by separating them with a space character)");
        editPanel.addTextEntry(8, TextParam.layouttcl, "TCL file for layouting", "", false, null, false);
        addButtons(editPanel);

        getDataFromPanel(editPanel);
    }

    private void addButtons(PropertiesPanel editPanel) {
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        GridBagConstraints gbc_btnpanel = new GridBagConstraints();
        gbc_btnpanel.insets = new Insets(15, 0, 5, 0);
        gbc_btnpanel.anchor = GridBagConstraints.LINE_START;
        gbc_btnpanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnpanel.gridx = 0;
        gbc_btnpanel.gridwidth = 5;
        gbc_btnpanel.gridy = 9;
        editPanel.add(btnPanel, gbc_btnpanel);

        JButton saveButton = new JButton("Save & close");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(checkInputDataValidity()) {
                    if(createTechnology()) {
                        logger.info("Technology " + textfields.get(TextParam.name).getText() + " created successfully");
                        dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
                    }
                }
            }
        });
        btnPanel.add(saveButton);
    }

    private boolean createTechnology() {
        String name = textfields.get(TextParam.name).getText();
        File balsafolder = new File(textfields.get(TextParam.balsafolder).getText());
        File genlibfile = new File(textfields.get(TextParam.genlibfile).getText());
        File libertylibfile = new File(textfields.get(TextParam.liberty).getText());
        File addInfoFile = new File(textfields.get(TextParam.addInfo).getText());

        String searchPaths = textfields.get(TextParam.searchpath).getText();
        String libraries = textfields.get(TextParam.libraries).getText();
        List<String> postCompileCmds = new ArrayList<>(); // aka not yet implemented
        List<String> verilogIncludes = new ArrayList<>(); // aka not yet implemented

        String layouttcl = textfields.get(TextParam.layouttcl).getText();

        Technology tech = techDir.createTechnology(name, balsafolder, "resyn", genlibfile, searchPaths, libraries, postCompileCmds, verilogIncludes, layouttcl, libertylibfile, addInfoFile);
        if(tech == null) {
            return false;
        }
        this.tech = tech;

        return true;
    }

    private boolean checkInputDataValidity() {
        if(!checkNameValidity()) {
            return false;
        }
        if(!checkBalsaFolderValidity()) {
            return false;
        }
        if(!checkGenlibFileValidity()) {
            return false;
        }
        if(!checkLibertyFileValidity()) {
            return false;
        }
        if(!checkAddInfoFileValidity()) {
            return false;
        }
        return true;
    }

    private boolean checkNameValidity() {
        String name = textfields.get(TextParam.name).getText();
        if(name.equals("")) {
            logger.error("Name cannot be empty");
            return false;
        }
        if(!StringUtils.isAlphanumeric(name)) {
            logger.error("Name must be alphanumeric");
            return false;
        }

        File f = new File(CommonConstants.DEF_TECH_DIR_FILE, name + CommonConstants.XMLTECH_FILE_EXTENSION);
        if(f.exists()) {
            logger.error("Technology " + name + " already exists. Delete it first");
            return false;
        }
        return true;
    }

    private boolean checkBalsaFolderValidity() {
        String balsafolder = textfields.get(TextParam.balsafolder).getText();
        File f = new File(balsafolder);
        if(!f.exists()) {
            logger.error("Balsa technology folder does not exists");
            return false;
        }
        if(!f.isDirectory()) {
            logger.error("Balsa technology folder should be a directory. It is not.");
            return false;
        }
        if(!Arrays.asList(f.list()).contains("startup.scm")) {
            logger.error("Balsa technology folder does not contain a startup.scm");
            return false;
        }
        return true;
    }

    private boolean checkGenlibFileValidity() {
        String genlibfile = textfields.get(TextParam.genlibfile).getText();
        File f = new File(genlibfile);
        if(!f.exists()) {
            logger.error("Genlib file does not exists");
            return false;
        }
        return true;
    }

    private boolean checkLibertyFileValidity() {
        String libertyfile = textfields.get(TextParam.liberty).getText();
        File f = new File(libertyfile);
        if(!f.exists()) {
            logger.error("Liberty file does not exists");
            return false;
        }
        return true;
    }

    private boolean checkAddInfoFileValidity() {
        String addInfofile = textfields.get(TextParam.addInfo).getText();
        File f = new File(addInfofile);
        if(!f.exists()) {
            logger.error("AddInfo file does not exists");
            return false;
        }
        return true;
    }

    public Technology getTech() {
        return tech;
    }
}
