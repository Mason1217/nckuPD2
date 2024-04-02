import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JPopupMenu.Separator;

import java.util.regex.Matcher;

interface Parser {
    public abstract void readFile();
    public abstract void parse(String line);
}

class Identifier {

    ArrayList<String> ID = new ArrayList<String>();

    /* 
        attrIdentity = {"attr", modifier, type, attrName}
        methodIdentity = {"method", modifier, returnType, methodName, args, statements}
    */

    public void showInfo() {

        System.out.println("memberName: " + ID.get(3));
        System.out.println("-----------------------------------------");

        for (String info : ID) {
            System.out.print(info + " ");
        }
        System.out.println("\n-----------------------------------------\n");
    }

}

class KeywordSeparator {

    HashMap<String, String> classFinder = new HashMap<String, String>();
    HashMap<String, String> memberFinder = new HashMap<String, String>();
    HashMap<String, String> methodRelated = new HashMap<String, String>();
    HashMap<String, String> attrRelated = new HashMap<String, String>();

    public KeywordSeparator() {
        classFinder.put("classRegex", "class" + "\\s+" + "(\\w+)");
        classFinder.put("classNameRegex", "(\\w+)");
        classFinder.put("colon", "\\:");

        memberFinder.put("attrRegex", "([+-])" + "\\s*" + "(\\w+)" + "\\s+" + "(\\w+)");
        memberFinder.put("attrArrRegex", "([+-])" + "\\s*" + "(\\w+)" + "([\\[])" + "(.*)" + "([\\]])" + "\\s*" + "(\\w+)");
        memberFinder.put("methodRegex", "([+-])" + "\\s*" + "(\\w+)" + "([\\(])" + "(.*)" + "([\\)])" + "\\s*" + "(\\w+)");

        methodRelated.put("argsRegex", "(\\w+)" + "\\s+" + "(\\w+)");
        methodRelated.put("setterRegex", "set[A-Z]\\w*");
        methodRelated.put("getterRegex", "get[A-Z]\\w*");

        attrRelated.put("arrSquaresRegex", "(\\[)" + "\\s*" + "(\\])");
    }
}

class ClassClassifier {
    
    HashMap<String, ArrayList< Identifier >> classAndMember = new HashMap<String, ArrayList< Identifier >>();
    ArrayList<String> classes = new ArrayList<String>();

    public boolean containCertainClass(String className) {

        int i = 0;
        for (i = 0; i < classes.size(); i++) {
            if (classes.get(i).equals(className)) {
                break;
            }
        }
        if (classes.size() != 0 && (i < classes.size() || (i == classes.size()-1 && classes.get(i).equals(className)))) {
            return true;
        }

        return false;
    }

    public void addClass(String className) {

        if (containCertainClass(className) == false) {

            classes.add(className);
            ArrayList<Identifier> classMembers = new ArrayList<Identifier>();
            classAndMember.put(className, classMembers);

            return;
        }
        else {
            System.out.println("class: \"" + className + "\" is already exists!");
            return;
        }
    }

    public void addAttr(String className, String modifier, String type, String attrName) {

        Identifier newAttr = new Identifier();

        newAttr.ID.add("attr");
        newAttr.ID.add(modifier);
        newAttr.ID.add(type);
        newAttr.ID.add(attrName);

        classAndMember.get(className).add(newAttr);

    }
    
    public String arrangeArgs(ArrayList<ArrayList< String >> argsList) {

        String argsString = "(";

        for (int i = 0; i < argsList.size(); i++) {

            if (i != argsList.size()-1) {
                argsString += argsList.get(i).get(0) + " " + argsList.get(i).get(1) + ", ";
            }
            else if (i == argsList.size()-1) {
                argsString += argsList.get(i).get(0) + " " + argsList.get(i).get(1);
            }
        }
        argsString += ")";

        return argsString;
    }
    
    public void addMethod(String className, String modifier, String type, String methodName, String argsString) {

        Identifier newMethod = new Identifier();

        KeywordSeparator separator = new KeywordSeparator();

        String methodArgs = "";
        String statement = "";

        ArrayList<ArrayList< String >> argsList = new ArrayList<ArrayList< String >>();
        Pattern argsPattern = Pattern.compile(separator.methodRelated.get("argsRegex"));
        Matcher argsMatcher = argsPattern.matcher(argsString);
        
        while (argsMatcher.find()) {
            ArrayList<String> arg = new ArrayList<String>();
            arg.add(argsMatcher.group(1));
            arg.add(argsMatcher.group(2));
            argsList.add(arg);
        }

        Pattern setPattern = Pattern.compile(separator.methodRelated.get("setterRegex"));
        Pattern getPattern = Pattern.compile(separator.methodRelated.get("getterRegex"));
        Matcher setMatcher = setPattern.matcher(methodName);
        Matcher getMatcher = getPattern.matcher(methodName);

        if (setMatcher.find()) {
            String attrName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            statement = "{\n        this." + attrName + " = " + argsList.get(0).get(1) + ";\n    }";
            methodArgs = arrangeArgs(argsList);
        }
        else if (getMatcher.find()) {
            String attrName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            statement = "{\n        return " + attrName + ";\n    }";
            methodArgs = arrangeArgs(argsList);
        }
        else {

            if (type.equals("int")) {
                statement = "{return 0;}";
            }
            else if (type.equals("boolean")) {
                statement = "{return false;}";
            }
            else if (type.equals("String")) {
                statement = "{return \"\";}";
            }
            else if (type.equals("void")) {
                statement = "{;}";
            }

            methodArgs = arrangeArgs(argsList);
            
        }

        newMethod.ID.add("method");
        newMethod.ID.add(modifier);
        newMethod.ID.add(type);
        newMethod.ID.add(methodName);
        newMethod.ID.add(methodArgs);
        newMethod.ID.add(statement);

        classAndMember.get(className).add(newMethod);

        //print method info
        //newMethod.showInfo();

    }
}

class MermaidParser implements Parser {

    String fileName = "";
    ArrayList<String> mermaidCode = new ArrayList<String>();
    ClassClassifier classClassifier = new ClassClassifier();
    String className = "";  //Record className in the present, and change it when encounter new className

    public MermaidParser(String fileName) {
        this.fileName = fileName;
    }

    public String determineModifier(String modifier) {

        if (modifier.equals("+")) {
            return "public";
        }
        else {
            return "private";
        }
    }

    public void readFile() {

        String line = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            while ((line = reader.readLine()) != null) {
                parse(line);
            }
            reader.close();
        }
        catch (IOException e) {
            System.err.println("Can't read file " + fileName);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void parse(String line) {

        String modifier = "";
        String type = "";
        String name = "";
        String argsString = "";

        KeywordSeparator separator = new KeywordSeparator();

        Pattern pattern = Pattern.compile(separator.classFinder.get("classRegex"));
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {

            className = matcher.group(1);

            if (!(classClassifier.containCertainClass(className))) {

                classClassifier.addClass(className);
            }
            
            return;
        }
        pattern = Pattern.compile(separator.classFinder.get("colon"));
        matcher = pattern.matcher(line);
        
        if (matcher.find()) {
            
            pattern = Pattern.compile(separator.classFinder.get("classNameRegex"));
            matcher = pattern.matcher(line);
            matcher.find();
            className = matcher.group(1);
        }
        
        for (HashMap.Entry<String, String> iterator : separator.memberFinder.entrySet()) {

            pattern = Pattern.compile(iterator.getValue());
            matcher = pattern.matcher(line);
            
            if (matcher.find()) {
                if (iterator.getKey().equals("attrRegex")) {

                    modifier = determineModifier(matcher.group(1));
                    type = matcher.group(2);
                    name = matcher.group(3);

                    classClassifier.addAttr(className, modifier, type, name);
                }
                else if (iterator.getKey().equals("attrArrRegex")) {
                    
                    type = matcher.group(2);
                    modifier = determineModifier(matcher.group(1));
                    name = matcher.group(6);
                    
                    String squares = matcher.group(3) + matcher.group(4) + matcher.group(5);
                    Pattern squaresPattern = Pattern.compile(separator.attrRelated.get("arrSquaresRegex"));
                    Matcher squaresMatcher = squaresPattern.matcher(squares);
                    
                    while (squaresMatcher.find()) {
                        type += (squaresMatcher.group(1) + squaresMatcher.group(2));
                    }

                    classClassifier.addAttr(className, modifier, type, name);
                }
                else if (iterator.getKey().equals("methodRegex")) {
                    modifier = determineModifier(matcher.group(1));
                    type = matcher.group(6);
                    name = matcher.group(2);
                    argsString = matcher.group(3) + matcher.group(4) + matcher.group(5);
                    /*print out args
                    System.out.println("args haven't been dealed with: " + argsString);
                    */

                    classClassifier.addMethod(className, modifier, type, name, argsString);
                }
            }
        }

    }

}

class JavaCodeGenerator {

    ClassClassifier classClassifier = new ClassClassifier();

    public JavaCodeGenerator(ClassClassifier classifier) {

        this.classClassifier = classifier;
    }
    
    public String generateContent(String className) {
    
        String content = "public class " + className + " {\n";
        
        /*System.out.println("*******About class " + className + "\'s members: *******");
        System.out.println("-----------------------------------------\n");*/
    
        for (Identifier classMember : classClassifier.classAndMember.get(className)) {
    
            //System.out.println("~Ready to add " + classMember.ID.get(3) + " into content~");
            //classMember.showInfo();
    
            if (classMember.ID.get(0).equals("attr")) {
    
                String memberModifier = classMember.ID.get(1);
                String memberType = classMember.ID.get(2);
                String memberName = classMember.ID.get(3);
    
                content += "    " + memberModifier + " " + memberType + " " + memberName + ";\n";
            }
            else if (classMember.ID.get(0).equals("method")) {
    
                String memberModifier = classMember.ID.get(1);
                String memberReturnType = classMember.ID.get(2);
                String memberName = classMember.ID.get(3);
                String memberAargs = classMember.ID.get(4);
                String memberStatement = classMember.ID.get(5);
    
                content += "    " + memberModifier + " " + memberReturnType + " " + memberName + memberAargs + " " + memberStatement + "\n";
            }
        }
        content += "}";
        //System.out.println("---------------end of class--------------\n");
    
        return content;
    }
    
    public void generateFile() {

        for (String className : classClassifier.classes) {

            try {

                String output = className + ".java";
                String content = generateContent(className);
                File file = new File(output);
    
                if (!file.exists()) {
                    file.createNewFile();
                }
    
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(content);
                }
                System.out.println("Java class has been generated: " + output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

public class CodeGenerator {
    public static void main(String[] args){

        if (args.length == 0){
            System.err.println("Please enter the file name");
            return;
        }
        String fileName = args[0];
        System.out.println("File name: " + fileName);

        MermaidParser mermaidParser = new MermaidParser(fileName);
        mermaidParser.readFile();
        JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(mermaidParser.classClassifier);
        javaCodeGenerator.generateFile();
    }
}

