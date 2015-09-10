package org.testng.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.testng.xml.XmlClass;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * {@code YamlBridge} is a slightly altered version of {@link org.testng.internal.Yaml}
 * that is adapted to current versions of snakeyaml.
 */
public class YamlBridge {
    public static XmlSuite parse(String filePath, InputStream is)
            throws FileNotFoundException {
        Constructor constructor = new Constructor(XmlSuite.class);
        {
            TypeDescription suiteDescription = new TypeDescription(XmlSuite.class);
            suiteDescription.putListPropertyType("packages", XmlPackage.class);
            suiteDescription.putListPropertyType("listeners", String.class);
            suiteDescription.putListPropertyType("tests", XmlTest.class);
            suiteDescription.putListPropertyType("method-selectors", XmlMethodSelector.class);
            constructor.addTypeDescription(suiteDescription);
        }

        {
            TypeDescription testDescription = new TypeDescription(XmlTest.class);
            testDescription.putListPropertyType("classes", XmlClass.class);
            testDescription.putMapPropertyType("metaGroups", String.class, List.class);
            testDescription.putListPropertyType("method-selectors", XmlMethodSelector.class);
            constructor.addTypeDescription(testDescription);
        }

        org.yaml.snakeyaml.Yaml y = new org.yaml.snakeyaml.Yaml(constructor);
        if (is == null)
            is = new FileInputStream(new File(filePath));
        XmlSuite result = (XmlSuite) y.load(is);

        result.setFileName(filePath);
        // DEBUG
        // System.out.println("[Yaml] " + result.toXml());

        // Adjust XmlTest parents and indices
        for (XmlTest t : result.getTests()) {
            t.setSuite(result);
            int index = 0;
            for (XmlClass c : t.getClasses()) {
                c.setIndex(index++);
            }
        }

        return result;
    }
}
