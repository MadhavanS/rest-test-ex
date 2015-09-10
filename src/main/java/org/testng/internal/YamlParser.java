package org.testng.internal;

import org.testng.TestNGException;
import org.testng.xml.IFileParser;
import org.testng.xml.XmlSuite;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class YamlParser implements IFileParser<XmlSuite> {

    @Override
    public XmlSuite parse(String filePath, InputStream is, boolean loadClasses) throws TestNGException {
        try {
            // use YamlBridge to adapt to modern snakeyaml APIs
            return YamlBridge.parse(filePath, is);
        } catch (FileNotFoundException e) {
            throw new TestNGException(e);
        }
    }

}