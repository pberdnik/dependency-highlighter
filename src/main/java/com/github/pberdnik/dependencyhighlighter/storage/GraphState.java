package com.github.pberdnik.dependencyhighlighter.storage;

import com.github.pberdnik.dependencyhighlighter.old.file.CodeFile;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Tag("graph")
public class GraphState implements Serializable {
    @Tag("files")
    @XMap()
    @Property(alwaysWrite = true)
    public Map<String, CodeFile> codeFiles;

    GraphState() {
        codeFiles = new HashMap<>();
    }
}
