package sax;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.XMLReader;


public class MyParser {
    
    private static void usage() {
        String usage = """
        usage: MyParser [options] [parameters] <file-path>
        parameters:
            --error-type: string, type of errors (default: any)
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

        public String getValue() {
            return value;
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


    private static class Parameters {
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
        public Parameters(ErrorTypes errorType, int questionCount, boolean onlyLoggedUsers, int minimalCorrectionCount,
                String filePath) {
            this.errorType = errorType;
            this.questionCount = questionCount;
            this.onlyLoggedUsers = onlyLoggedUsers;
            this.minimalCorrectionCount = minimalCorrectionCount;
            this.filePath = filePath;
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

    private static Node buildSemanticTree(ArrayList<Token> tokens, Node root) {
        if(root == null) {
                root = new Node(Node.Type.ARGUMENTS , null);
        }
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case FLAG_ERROR_TYPE:
                case FLAG_MIN_USER_COUNT:
                case FLAG_QUESTION_COUNT: 
                    if (root.type == Node.Type.ARGUMENTS) {
                        Node parametersListNode = new Node(Node.Type.PARAMETERS_LIST, null);
                        parametersListNode.children.add(buildSemanticTree(tokens, parametersListNode));
                    } else if (root.type == Node.Type.PARAMETERS_LIST) {
                        Node optionNode = new Node(Node.Type.VALUED_OPTION_FLAG, token);
                        tokens.remove(0);
                        optionNode.children.add(
                            buildSemanticTree(tokens, optionNode)
                        );
                    } else {
                        throw new Error("Can't have option flag here: " + token.value);
                    }
                    break;
                case FLAG_NO_ANON:
                    if (root.type == Node.Type.ARGUMENTS) {
                        Node parametersListNode = new Node(Node.Type.PARAMETERS_LIST, null);
                        parametersListNode.children.add(buildSemanticTree(tokens, parametersListNode));
                        root.children.add(parametersListNode);
                    } else if (root.type == Node.Type.PARAMETERS_LIST) {
                        Node flagNode = new Node(Node.Type.BOOLEAN_OPTION_FLAG, token);
                        tokens.remove(0);
                        root.children.add(flagNode);
                    } else {
                        throw new Error("Can't have option flag here: " + token.value);
                    }
                    break;
                case VALUE:
                    if (root.type == Node.Type.VALUED_OPTION_FLAG) {
                        Node optionValueNode = new Node(Node.Type.VALUED_OPTION_VALUE, token);
                        tokens.remove(0);
                        root.children.add(optionValueNode);
                    } else if (root.type == Node.Type.ARGUMENTS) {
                        Node postitionalNode = new Node(Node.Type.POSITIONAL, token);
                        tokens.remove(0);
                        root.children.add(postitionalNode);
                    } else {
                        throw new Error("Can't have option value here: " + token.value);
                    }
                    break;
                default:
                    throw new Error("Unknown token type for: " + token.value);
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

        ArrayList<Token> tokens = getTokens(args);
        Node tree = buildSemanticTree(tokens, null);
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
                            default:
                                break;
                        }

                    }
                case POSITIONAL:
                    filePath = node.token.value;
                default:
                    break;
            }

        }
        if (filePath == null) {
            throw new Exception("Missing filePath.");
        }

        return new Parameters(errorType, questionCount, noAnon, minUserCount, filePath);
    }

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

    static public void main(String[] args) throws Exception {
        Parameters parameters;
        try {
            parameters = getArgs(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            usage();
            return;
        }

        System.out.println(parameters);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setErrorHandler(new MyErrorHandler(System.err));
        xmlReader.setContentHandler(new MyHandler());
        xmlReader.parse(convertToFileURL(parameters.filePath));
    }
}
