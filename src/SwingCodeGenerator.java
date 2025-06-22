import java.io.*;
import java.util.*;

public class SwingCodeGenerator {

    static class Node {
        String type;
        Map<String, String> properties = new LinkedHashMap<>();
        List<Node> children = new LinkedList<>();
    }

    //static Node root = null;
    static List<Node> roots = new LinkedList<>(); // Frame nodes are roots
    static int varCount = 0;
    static String parentFrame;

    public static void main(String[] args) throws Exception {
        String inputPath, inputFile;
        String outputPath, outputFile;

        inputPath = "C:\\Users\\fjgarrido\\Documents\\Private\\Repositorio\\Github\\jagasoft\\SwingCodeGenerator/resource/";
        outputPath = "C:\\Users\\fjgarrido\\Documents\\Private\\Repositorio\\Github\\jagasoft\\SwingCodeGenerator\\src\\";

        inputFile = "example0";
        outputFile = capitalizeFirstLetter(inputFile);

        /*Node root =*/ parseFile(inputPath+inputFile+".swing");

        dumpAll();

        String javaCode = generateJavaCode(outputFile);
        writeToFile(outputPath+outputFile+".java", javaCode);
        System.out.println("Código generado en " + outputPath+outputFile+".java");
    }

    private static void dump(Node node, String indent) {
        if( roots.get(0) == node )
            System.out.println("Root type: " + node.type);
        else
            System.out.println(indent + "Node type: " + node.type);

        for (Map.Entry<String, String> entry : node.properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(indent + "Key: " + key + ", Value: " + value);
        }

        for (Node nextNode : node.children) {
            dump(nextNode, indent + "\t");
            System.out.println(indent + "Backtracking...");
        }
    }

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    static boolean isAComment(String line) {
        return  (line.substring(0, 1) + line.substring(1, 2)).equals("//");
    }

    static void parseFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        Stack<Node> stack = new Stack<>();
        String lineIn;
        int lineNum = 0;
        Node node = null;
        Node lastCommentNode = null;
        boolean isEnd = true;

        while ((lineIn = reader.readLine()) != null) {
            lineNum++;
            lineIn = lineIn.trim();
            if (lineIn.isEmpty()) continue;
System.out.println(lineIn);
            String[] parts = lineIn.split(" ", 2);
            String type = parts[0].toUpperCase(); // type or property
            String value = parts.length > 1 ? parts[1] : null;

            if( type.equals("BEGIN") ) {
                if( ! isEnd ) {
                    stack.push(node); // nth Begin nested
                }
                node = new Node();
                if( isEnd ) {
                    roots.add(node);    // the very first node is the root
                    isEnd = false;      // Begin node not nested
                }
                if( value != null ) {
                    parts = value.split(" ", 2);
                    type = parts[0].toUpperCase(); // type or property
                    value = parts.length > 1 ? parts[1] : null;
                } else {
                    throw new RuntimeException("Malformed file: Missing type for 'Begin' on line " + lineNum);
                }
                node.type = type;
                if ( ! stack.isEmpty() ) {
                    stack.peek().children.add(node); // links node to its children
                }
            } else if( type.equals("END") ) {
                if( node == null )
                    throw new RuntimeException("Malformed file: 'End " + value + "' was found on line " + lineNum + " without Begin");
                if( value != null && ! node.type.equals(value.toUpperCase()) ) { // End [optional component]
                    throw new RuntimeException("Malformed file: Expected to close '" + node.type + "' on line " + lineNum + " but 'End " + value + "' was found");
                }

                if( ! stack.isEmpty() ) {
                    node = stack.pop(); // previous node
                } else {
                    isEnd = true;
                }
                continue;
            } else if( isAComment(lineIn) ) {
                type = "COMMENT";
                value = lineIn; // whole line
                if ( ! stack.isEmpty() ) {
                    stack.peek().children.add(node);
                } else { // isEnd == true
                    node = new Node();
                    node.type = type;
                    roots.add(node);
                }
            }

            node.properties.put(type, value);
        }

        if( ! stack.isEmpty() || ! isEnd ) {
            throw new RuntimeException("Malformed file: EOF was found after line " + lineNum + ". 'End' missing?");
        }
    }

    static void dumpAll() {
        for (Node node : roots) {
            dump(node, "");
        }
    }

    static String generateJavaCode(String className) {
        StringBuilder sb = new StringBuilder();

        sb.append(
                "import javax.swing.*;\n"
              + "import java.awt.*;\n"
              + "public class "+ className +" {\n"
              + "\tpublic static void main(String[] args) {\n"
              + "\t\tSwingUtilities.invokeLater(() -> {\n");

        for (Node node : roots) {
            generateNodeCode(sb, node, "\t\t\t\t", null);
        }

        sb.append("\t\t});\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append(
                "    private static class CommandBus {\n"
              + "        public static void execute(String command) {\n"
              + "            System.out.println(command);\n");
        sb.append("\t\t}\n");
        sb.append("\t}\n");
        sb.append("}\n");
        return sb.toString();
    }

    static void generatePropertyCode(StringBuilder sb, Node node, String indent, String parentName) {
        for (Map.Entry<String, String> entry : node.properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println("Clave: " + key + ", Valor: " + value);

            switch (key) {
                case "TITLE":
                    sb.append(indent + parentName + ".setTitle(" + value + ");\n");
                    break;
                case "TEXT":
                    sb.append(indent + parentName + ".setText(" + value + ");\n");
                    break;
                case "BOUNDS":
                    sb.append(indent + parentName + ".setBounds(" + value + ");\n");
                    break;
                case "LAYOUT":
                    sb.append(indent + parentName + ".setLayout(new " + layoutCode(value) + ");\n");
                    break;
                case "BACKGROUND":
                    sb.append(indent + parentName + ".setBackground(" + value + ");\n");
                    break;
                case "ACTION":
                    sb.append(indent + parentName + ".addActionListener(e -> CommandBus.execute(" + value + "));\n");
                    break;
                case "COLUMNS":
                    sb.append(indent + parentName + ".setColumns(" + value + ");\n");
                    break;
                case "COMMENT":
                    sb.append(indent + value + "\n");
                    break;
                case "PACK":
                    sb.append(indent + parentName + ".pack();\n");
                    break;
            }
        }
    }

    static void generateNodeCode(StringBuilder sb, Node node, String indent, String parentName) {
        String varName = node.properties.get(node.type); // explict name

        if( varName == null )
            varName = node.type.toLowerCase() + "_" + varCount++; // (int) (Math.random() * 1000);

        switch (node.type) {
            case "FRAME":
                parentFrame = varName;
                sb.append(indent + "JFrame " + varName + " = new JFrame();\n");
                sb.append(indent + varName + ".setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName + ".getContentPane()");

                sb.append(indent + varName + ".setVisible(true);\n");
                break;

            case "PANEL":
                sb.append(indent + "JPanel " + varName + " = new JPanel();\n");

                generatePropertyCode(sb, node, indent, varName);

                if (parentName != null)
                    sb.append(indent + parentName + ".add(" + varName + ");\n");

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);
                break;

            case "BUTTON":
                sb.append(indent + "JButton " + varName + " = new JButton();\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentName != null)
                    sb.append(indent + parentName + ".add(" + varName + ");\n");
                break;

            case "MENUBAR":
                sb.append(indent + "JMenuBar " + varName + " = new JMenuBar();\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentFrame != null)
                    sb.append(indent + parentFrame + ".setJMenuBar(" + varName + ");\n");
                break;

            case "MENU":
                sb.append(indent + "JMenu " + varName + " = new JMenu();\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentFrame != null)
                    sb.append(indent + parentFrame + ".add(" + varName + ");\n");
                break;

            case "MENUITEM":
                sb.append(indent + "JMenuItem " + varName + " = new JMenuItem();\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentName != null)
                    sb.append(indent + parentName + ".add(" + varName + ");\n");
                break;

            case "DESKTOPPANE":
                sb.append(indent + "JDesktopPane " + varName + " = new JDesktopPane();\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentName != null)
                    sb.append(indent + parentName + ".add(" + varName + ");\n");
                break;

            case "INTERNALFRAME":
                sb.append(indent + "JInternalFrame " + varName + " = new JInternalFrame();\n");

                generatePropertyCode(sb, node, indent, varName);
                /*if (varName.startsWith("FRAME"))
                    sb.append(indent + varName + ".setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n");*/

                sb.append(indent + varName + ".setVisible(true);\n");

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentName != null)
                    sb.append(indent + parentName + ".add(" + varName + ");\n");
                break;

            case "LABEL":
                sb.append(indent + "JLabel " + varName + " = new JLabel();\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentName != null)
                    sb.append(indent + parentName + ".add(" + varName + ");\n");
                break;

            case "TEXTFIELD":
                if( ! node.properties.containsKey("COLUMNS") )
                    node.properties.put("COLUMNS", "20"); // insert default property

                sb.append(indent + "JTextField " + varName + " = new JTextField();\n");

                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                if (parentName != null)
                    sb.append(indent + parentName + ".add(" + varName + ");\n");
                break;

            case "COMMENT":
                generatePropertyCode(sb, node, indent, varName);

                for (Node nextNode : node.children)
                    generateNodeCode(sb, nextNode, indent, varName);

                break;

            default:
                System.out.println("Missing type " + node.type);
                // Otros componentes se pueden añadir aquí
                break;
        }

    }

    static String layoutCode(String layout) {
        switch (layout.toLowerCase()) {
            case "border": return "BorderLayout()";
            case "grid": return "GridLayout()";
            case "flow": // fall into
            default: return "FlowLayout()";
        }
    }

    static String getParentFrame() {
        return "frame_0"; // El primer JFrame
    }

    static void writeToFile(String filename, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(content);
        }
    }
}