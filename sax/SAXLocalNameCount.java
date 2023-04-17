package sax;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.*;
import java.io.*;

public class SAXLocalNameCount extends DefaultHandler {

    static final String outputEncoding = "UTF-8";

    private static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    private static void usage() {
        System.out.println("usage: SAXLocalNameCount <file-path>");
    }

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
 

    static public void main(String[] args) throws Exception {
        String filename = null;
    
        if (args.length != 1) {
            usage();
            return;
        } else {
            filename = args[0];
        }

        if (filename == null) {
            usage();
            return;
        }

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setErrorHandler(new MyErrorHandler(System.err));
        xmlReader.setContentHandler(new SAXLocalNameCount());
        xmlReader.parse(convertToFileURL(filename));
    }

    private static class MyErrorHandler implements ErrorHandler {
        private PrintStream out;
    
        MyErrorHandler(PrintStream out) {
            this.out = out;
        }
    
        private String getParseExceptionInfo(SAXParseException spe) {
            String systemId = spe.getSystemId();
    
            if (systemId == null) {
                systemId = "null";
            }
    
            String info = "URI=" + systemId + " Line=" 
                + spe.getLineNumber() + ": " + spe.getMessage();
    
            return info;
        }
    
        public void warning(SAXParseException spe) throws SAXException {
            out.println("Warning: " + getParseExceptionInfo(spe));
        }
            
        public void error(SAXParseException spe) throws SAXException {
            String message = "Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }
    
        public void fatalError(SAXParseException spe) throws SAXException {
            String message = "Fatal Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }
    }
}