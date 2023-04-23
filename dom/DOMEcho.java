package dom;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class DOMEcho {

    static final String outputEncoding = "UTF-8";

    private static void usage() {
        System.out.println("usage: DOMEcho <file-path>");
    }

    public static void main(String[] args) throws Exception {
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

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder(); 
        Document doc = db.parse(new File(filename));
    }
}