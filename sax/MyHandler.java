package sax;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import sax.MyParser.Parameters;
import sax.MyParser.Parameters.ErrorTypes;

import java.util.*;

public class MyHandler extends DefaultHandler {

    static final String outputEncoding = "UTF-8";
    private Parameters parameters;
    protected ArrayList<Integer> modifsIds;
    private ArrayList<Integer> ignoredModifsIds;
    private HashMap<Integer, ErrorTypes> errorTypesById;

    public MyHandler(Parameters parameters, HashMap<Integer, ErrorTypes> errorTypesById) {
        this.parameters = parameters;
        this.errorTypesById = errorTypesById;
    }

    public void startDocument() throws SAXException {
        System.out.println("Starting the parsing of the document");
        modifsIds = new ArrayList<Integer>();
        ignoredModifsIds = new ArrayList<Integer>();
    }

    public void startElement(String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {

        if(localName == "modif") {
            int id = Integer.parseInt(atts.getValue("id"));
            int userId = Integer.parseInt(atts.getValue("wp_user_id"));
            int nbModifs = Integer.parseInt(atts.getValue("wp_user_num_modif"));
            
            // We ignore excess questions
            if(parameters.getQuestionCount() != -1) {
                if(parameters.getQuestionCount() <= modifsIds.size()) {
                    ignoredModifsIds.add(id);
                    return;
                }
            }   
            // We ignore non-logged user
            if(parameters.isOnlyLoggedUsers() && userId == 0) {
                ignoredModifsIds.add(id);
                return;
            }
            // We ignore when not enough modifications from user
            if(parameters.getMinimalCorrectionCount() != -1) {
                if(parameters.getMinimalCorrectionCount() > nbModifs) {
                    ignoredModifsIds.add(id);
                    return; 
                }
            }

            // We ignore error of the wrong type
            if(parameters.getErrorType() != ErrorTypes.ANY && parameters.getErrorType() != errorTypesById.get(id)) {
                ignoredModifsIds.add(id);
                return; 
            }

            modifsIds.add(id);
        }
    }

    public void endDocument() throws SAXException {
        System.out.println("End of the parsing of the document");
        System.out.println(modifsIds.size() + ignoredModifsIds.size() + " modifs found.");
        System.out.println(modifsIds.size() + " modifs kept.");
        System.out.println(ignoredModifsIds.size() + " modifs ignored.");
    }
}