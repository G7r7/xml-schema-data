package sax;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.XMLReader;

import sax.MyParser.Parameters.ErrorTypes;

public class MyParser {
    
    private static void usage() {
        String usage = """
        usage: MyParser [options] [parameters] <file-path> <error-file-path>
        parameters:
            --error-type: string, type of errors: non_word_error, real_word_error, any (default: any)
            --min-user-count: number, minimal contributions for users (default: -1)
            --questions-count: number, how many question to extract from file (default: -1)
        options: 
            --no-anon: no anonymous users contributions
        """;
        System.out.println(usage);
    }

    private static class Token {
        private String value;
        private Type type;

        public Token(String value, Type type) {
            this.value = value;
            this.type = type;
        }

        public static enum Type {
            FLAG_ERROR_TYPE,
            FLAG_MIN_USER_COUNT,
            FLAG_QUESTION_COUNT,
            FLAG_NO_ANON,
            VALUE,
            UNKNOWN
        }
    }

    private static ArrayList<Token> getTokens(String[] args) {
        ArrayList<Token> tokens = new ArrayList<Token>();
        for (String arg : args) {
            Token.Type tokenType = Parameters.validFlags.get(arg);
            if (tokenType != null) {
                tokens.add(new Token(arg, tokenType));
            } else {
                tokens.add(new Token(arg, Token.Type.VALUE));
            }
        }
        return tokens;
    }


    protected static class Parameters {
        public static HashMap<String, Token.Type> validFlags = new HashMap<String, Token.Type>() {{
            put("--error-type", Token.Type.FLAG_ERROR_TYPE);
            put("--min-user-count", Token.Type.FLAG_MIN_USER_COUNT);
            put("--questions-count", Token.Type.FLAG_QUESTION_COUNT);
            put("--no-anon", Token.Type.FLAG_NO_ANON);
        }};

        public static HashMap<String, ErrorTypes> ErrorTypesValues = new HashMap<String, ErrorTypes>() {{
            put("non_word_error", ErrorTypes.NON_WORD_ERROR);
            put("real_word_error", ErrorTypes.REAL_WORD_ERROR);
            put("any", ErrorTypes.ANY);
        }};

        public static enum ErrorTypes {
            NON_WORD_ERROR,
            REAL_WORD_ERROR,
            ANY
        }

        private ErrorTypes errorType;
        private int questionCount;
        private boolean onlyLoggedUsers;
        private int minimalCorrectionCount;
        private String filePath;
        private String errorFilePath;
        public Parameters(ErrorTypes errorType, int questionCount, boolean onlyLoggedUsers, int minimalCorrectionCount,
                String filePath, String errorFilePath) {
            this.errorType = errorType;
            this.questionCount = questionCount;
            this.onlyLoggedUsers = onlyLoggedUsers;
            this.minimalCorrectionCount = minimalCorrectionCount;
            this.filePath = filePath;
            this.errorFilePath = errorFilePath;
        }
        public ErrorTypes getErrorType() {
            return errorType;
        }
        public int getQuestionCount() {
            return questionCount;
        }
        public boolean isOnlyLoggedUsers() {
            return onlyLoggedUsers;
        }
        public int getMinimalCorrectionCount() {
            return minimalCorrectionCount;
        }
        public String getFilePath() {
            return filePath;
        }
        public String getErrorFilePath() {
            return errorFilePath;
        }
    }

    private static class Node {

        public static enum Type {
            ARGUMENTS,
            PARAMETERS_LIST,
            BOOLEAN_OPTION_FLAG,
            VALUED_OPTION_FLAG,
            VALUED_OPTION_VALUE,
            POSITIONAL,
            UNKNOWN
        }

        private Type type;
        private Token token;
        private ArrayList<Node> children;
        public Node(Type type, Token token) {
            this.type = type;
            this.token = token;
            this.children = new ArrayList<Node>();
        }
    }

    private static Node buildSemanticTree(Node root, ListIterator<Token> it) throws Exception {
        if(root == null) {
                root = new Node(Node.Type.ARGUMENTS , null);
        }
        if (it.hasNext()) {
            switch (root.type) {
                case ARGUMENTS: {
                    Node parameters = new Node(Node.Type.PARAMETERS_LIST, null);
                    while (it.hasNext() && it.next().type != Token.Type.VALUE) {
                        it.previous();
                        parameters = buildSemanticTree(parameters, it);
                    }
                    it.previous();
                    root.children.add(parameters);
                    while (it.hasNext() && it.next().type == Token.Type.VALUE) {
                        it.previous();
                        Node positional = new Node(Node.Type.POSITIONAL, it.next());
                        root.children.add(positional);
                    }
                    break;
                }
                case PARAMETERS_LIST: {
                    Token token = it.next();
                    switch (token.type) {
                        case FLAG_ERROR_TYPE:
                        case FLAG_MIN_USER_COUNT:
                        case FLAG_QUESTION_COUNT: {
                            Node valueFlag = new Node(Node.Type.VALUED_OPTION_FLAG, token);
                            root.children.add(valueFlag);
                            valueFlag = buildSemanticTree(valueFlag, it);
                            break;
                        }
                        case FLAG_NO_ANON: {
                            Node flag = new Node(Node.Type.BOOLEAN_OPTION_FLAG, token);
                            root.children.add(flag);
                            break;
                        } 
                        case VALUE: {
                            break;
                        }  
                        default:
                            throw new Exception("Invalid token \"" + token.value + "\" of type : " + token.type.name());
                    }
                    break;
                }
                case VALUED_OPTION_FLAG: {
                    Token token = it.next();
                    switch (token.type) {
                        case VALUE: 
                        {
                            Node node = new Node(Node.Type.VALUED_OPTION_VALUE, token);
                            root.children.add(node);
                            break;
                        }
                        default:
                            throw new Exception("Unknown option value \"" + token.value + "\" of type : " + token.type.name());
                    }
                    break;
                }
                default:
                    throw new Exception("Can't build semantic of node of type : " + root.type.name());
            }
        }
        return root;
    }

    private static Parameters getArgs(String[] args) throws Exception {
        Integer minUserCount = -1;
        Integer questionCount = -1;
        Boolean noAnon = false;
        Parameters.ErrorTypes errorType = Parameters.ErrorTypes.ANY;
        String filePath = null;
        String errorFilePath = null;

        int positionalsTreated = 0;
        ArrayList<Token> tokens = getTokens(args);
        ListIterator<Token> it = tokens.listIterator();
        Node tree = buildSemanticTree(null, it);
        for (int i = 0; i < tree.children.size(); i++) {
            Node node = tree.children.get(i);
            switch (node.type) {
                case PARAMETERS_LIST:
                    for (Node node2 : node.children) {
                        switch (node2.type) {
                            case VALUED_OPTION_FLAG:
                                switch (node2.token.type) {
                                    case FLAG_ERROR_TYPE:
                                        errorType = Parameters.ErrorTypesValues.get(node2.children.get(0).token.value);
                                        if(errorType == null) {
                                            throw new Exception("Invalid value \""+ node2.children.get(0).token.value +"\" for option error-type");
                                        }
                                        break;    
                                    case FLAG_MIN_USER_COUNT:
                                        minUserCount =  Integer.decode(node2.children.get(0).token.value);
                                        break;
                                    case FLAG_QUESTION_COUNT:
                                        questionCount =  Integer.decode(node2.children.get(0).token.value);
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case BOOLEAN_OPTION_FLAG:
                                switch (node2.token.type) {
                                    case FLAG_NO_ANON:
                                        noAnon =  true;
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            default:
                                break;
                        }

                    }
                    break;
                case POSITIONAL:
                    switch (positionalsTreated) {
                        case 0:
                            filePath = node.token.value;
                            positionalsTreated++;
                            break;
                        case 1:
                            errorFilePath = node.token.value;
                            positionalsTreated++;
                            break;
                        default:
                            throw new Exception("Unexpected positionnal argument: " + node.token.value);
                    }
                    break;
                default:
                    break;
            }

        }
        if (filePath == null) {
            throw new Exception("Missing filePath.");
        }
        if (errorFilePath == null) {
            throw new Exception("Missing errorFilePath.");
        }

        return new Parameters(errorType, questionCount, noAnon, minUserCount, filePath, errorFilePath);
    }

    protected static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    static public void main(String[] args) throws Exception {
        Parameters parameters;
        try {
            parameters = getArgs(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            usage();
            return;
        }

        System.out.println("error-type: \""+ parameters.errorType +"\"");
        System.out.println("min-user-count: \""+ parameters.minimalCorrectionCount +"\"");
        System.out.println("questions-count: \""+ parameters.questionCount +"\"");
        System.out.println("no-anon \""+ parameters.onlyLoggedUsers +"\"");
        System.out.println("file-path \""+ parameters.filePath +"\"");
        System.out.println("error-file-path \""+ parameters.errorFilePath +"\"");

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();


        // Spelling error types parsing
        XMLReader errorTypeXmlReader = saxParser.getXMLReader();
        errorTypeXmlReader.setErrorHandler(new MyErrorHandler(System.err));
        errorTypeXmlReader.setContentHandler(new MyHandlerForErrors());
        errorTypeXmlReader.parse(convertToFileURL(parameters.errorFilePath));
        HashMap<Integer, ErrorTypes> errorTypesById = ((MyHandlerForErrors)(errorTypeXmlReader.getContentHandler())).errorTypesById;

        // modifs parsing
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setErrorHandler(new MyErrorHandler(System.err));
        xmlReader.setContentHandler(new MyHandler(parameters, errorTypesById));
        xmlReader.parse(convertToFileURL(parameters.filePath));
    }
}
