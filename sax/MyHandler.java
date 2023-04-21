package sax;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.*;
import java.io.*;

public class MyHandler extends DefaultHandler {

    static final String outputEncoding = "UTF-8";

    private Hashtable<String, Integer> tags;

    public void startDocument() throws SAXException {
        tags = new Hashtable<String, Integer>();
    }

    public void startElement(String namespaceURI,
                            String localName,
                            String qName, 
                            Attributes atts)
        throws SAXException {

        String key = localName;
        Object value = tags.get(key);

        if (value == null) {
            tags.put(key, 1);
        } 
        else {
            int count = ((Integer)value).intValue();
            count++;
            tags.put(key, count);
        }
    }

    public void endDocument() throws SAXException {
        Enumeration e = tags.keys();
        while (e.hasMoreElements()) {
            String tag = (String)e.nextElement();
            int count = ((Integer)tags.get(tag)).intValue();
            System.out.println("Local Name \"" + tag + "\" occurs " 
                               + count + " times");
        }    
    }
}