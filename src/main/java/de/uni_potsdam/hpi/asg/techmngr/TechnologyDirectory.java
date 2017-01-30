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

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.technology.Balsa;
import de.uni_potsdam.hpi.asg.common.technology.Genlib;
import de.uni_potsdam.hpi.asg.common.technology.SyncTool;
import de.uni_potsdam.hpi.asg.common.technology.Technology;

public class TechnologyDirectory {

    private File                      dir;
    private BiMap<String, Technology> techs;

    private TechnologyDirectory(File dir, BiMap<String, Technology> techs) {
        this.dir = dir;
        this.techs = techs;
    }

    public static TechnologyDirectory create(String dir) {
        return create(FileHelper.getInstance().replaceBasedir(dir));
    }

    public static TechnologyDirectory create(File dir) {
        if(!dir.exists()) {
            dir.mkdirs();
        }
        if(!dir.isDirectory()) {
            return null;
        }
        BiMap<String, Technology> techs = TechnologyDirectory.readTechnologies(dir);

        return new TechnologyDirectory(dir, techs);
    }

    private static BiMap<String, Technology> readTechnologies(File dir) {
        BiMap<String, Technology> techs = HashBiMap.create();
        for(File f : dir.listFiles()) {
            Technology t = Technology.readInSilent(f);
            if(t != null) {
                techs.put(t.getName(), t);
            }
        }
        return techs;
    }

    public Technology createTechnology(Window parent, String name, String balsafolder, String style, String genlibfile, String searchPaths, String libraries, List<String> postCompileCmds, List<String> verilogIncludes) {
        Balsa balsa = new Balsa(style, name);
        File sourcedir = new File(balsafolder);
        File targetdir = new File(getBalsaTechDir(), name);
        targetdir.mkdirs();
        try {
            FileUtils.copyDirectory(sourcedir, targetdir);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(parent, "Error while copying balsa technology directory", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Genlib genlib = new Genlib(name + TechMngrMain.genlibfileExtension);
        File sourcefile = new File(genlibfile);
        File targetfile = new File(dir, name + TechMngrMain.genlibfileExtension);
        try {
            FileUtils.copyFile(sourcefile, targetfile);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(parent, "Error while copying genlib file", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        SyncTool synctool = new SyncTool(searchPaths, libraries, postCompileCmds, verilogIncludes);

        Technology tech = new Technology(name, balsa, genlib, synctool);
        if(!Technology.writeOut(tech, new File(dir, name + TechMngrMain.techfileExtension))) {
            JOptionPane.showMessageDialog(parent, "Error while creating technology file", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        this.techs.put(name, tech);

        return tech;
    }

    public Technology importTechnology(Window parent, Technology srcTech, File srcDir) {
        String name = srcTech.getName();

        if(techs.containsKey(name)) {
            return null;
        }

        File balsaSourceFolder = new File(srcDir, srcTech.getBalsa().getTech());
        String balsafolder = balsaSourceFolder.getAbsolutePath();
        String style = srcTech.getBalsa().getStyle();

        String genlibfile = srcTech.getGenLib();

        String searchPaths = srcTech.getSynctool().getSearchPaths();
        String libraries = srcTech.getSynctool().getLibraries();
        List<String> postCompileCmds = srcTech.getSynctool().getPostCompileCmds();
        List<String> verilogIncludes = srcTech.getSynctool().getVerilogIncludes();

        return createTechnology(parent, name, balsafolder, style, genlibfile, searchPaths, libraries, postCompileCmds, verilogIncludes);
    }

    public void deleteTechnology(Window parent, String name) {
        if(!techs.containsKey(name)) {
            return;
        }

        File balsadir = new File(getBalsaTechDir(), name);
        try {
            FileUtils.deleteDirectory(balsadir);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(parent, "Failed to remove Balsa technology folder", "Error", JOptionPane.ERROR_MESSAGE);
        }

        File genlibfile = new File(dir, name + TechMngrMain.genlibfileExtension);
        if(!genlibfile.delete()) {
            JOptionPane.showMessageDialog(parent, "Failed to remove Genlib file", "Error", JOptionPane.ERROR_MESSAGE);
        }

        File techfile = new File(dir, name + TechMngrMain.techfileExtension);
        if(!techfile.delete()) {
            JOptionPane.showMessageDialog(parent, "Failed to remove technology file", "Error", JOptionPane.ERROR_MESSAGE);
        }

        techs.remove(techs.get(name));
    }

    private File getBalsaTechDir() {
        return FileHelper.getInstance().replaceBasedir(TechMngrMain.balsatechdir);
    }

    public Set<Technology> getTechs() {
        return techs.values();
    }
}