package sax;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import sax.MyParser.Parameters.ErrorTypes;

import java.util.*;


public class MyHandlerForErrors extends DefaultHandler {

    static final String outputEncoding = "UTF-8";
    private int lastIdSeen;
    private ErrorTypes lastErrorType;
    protected HashMap<Integer, ErrorTypes> errorTypesById;
    private Cases currentCase = Cases.Other;

    private static enum Cases {
        Id,
        ErrorType,
        Other
    };

    public void startDocument() throws SAXException {
        System.out.println("Starting the parsing of the document");
        errorTypesById = new HashMap<>();
    }



    public void startElement(String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {

            switch (localName) {
                case "modif_id" -> currentCase = Cases.Id;
                case "label" -> currentCase = Cases.ErrorType;
                default -> currentCase = Cases.Other;
            }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String value = new String(ch, start, length);
        switch (currentCase) {
            case Id:
                lastIdSeen = Integer.parseInt(value);
                currentCase = Cases.Other;
                break;
            case ErrorType:
                lastErrorType = MyParser.Parameters.ErrorTypesValues.get(value);
                currentCase = Cases.Other;
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(localName == "annotation") {
            errorTypesById.put(lastIdSeen, lastErrorType);
        }
    }

    public void endDocument() throws SAXException {
        System.out.println("End of the parsing of the document");
        System.out.println(errorTypesById.size() + " errors annotations found.");
    }
}