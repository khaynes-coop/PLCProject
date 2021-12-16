package plc.project;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(indent);
        for (Ast.Global a:ast.getGlobals()) {
            newline(indent + 1);
            visit(a);
        }
        if(ast.getGlobals().size() > 0)
            newline(indent);
        newline(indent+1);
        print("public static void main(String[] args) {");
        newline(indent + 2);
        print("System.exit(new Main().main());");
        newline(indent + 1);
        print("}");
        newline(indent);
        for (Ast.Function a : ast.getFunctions()) {
            indent++;
            newline(indent);
            visit(a);
            indent--;
            newline(indent);
        }
        newline(indent);
        print("}");
        return null;//TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(!ast.getMutable()){print("final ");}
        print(ast.getVariable().getType().getJvmName());
        if(ast.getValue().isPresent()){
            if(ast.getValue().get().getClass().getSimpleName().equals("PlcList")){
                print("[]");
                print(" ");
                print(ast.getName(), " = {");
                visit(ast.getValue().get());
                print("}");
            }
            else {
                print(" ", ast.getName());
                print(" = ");
                visit(ast.getValue().get());
            }
        }
        else{print(" ", ast.getName());}
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        //add args and args types, check for return
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getJvmName(), "(");
        for(int i = 0; i < ast.getParameters().size(); i++){
            String name = types(ast.getParameterTypeNames().get(i));
            print(name, " ", ast.getParameters().get(i));
            if(i < ast.getParameters().size() -1) {
                print(", ");
            }
        }

        print(") {");
        for (Ast.Statement a : ast.getStatements()) {
            newline(indent + 1);
            visit(a);
        }
        if(ast.getStatements().size() > 0)
            newline(indent);
        print("}");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");

        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getName());
        if(ast.getValue().isPresent()){
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ");
        visit(ast.getValue());
        print(";");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(), ") {");

        for (Ast.Statement a: ast.getThenStatements()) {
            newline(indent + 1);
            visit(a);
        }
        newline(indent);
        print("}");
        if(ast.getElseStatements().size() > 0){
            print(" else {");
            for (Ast.Statement a: ast.getElseStatements()) {
                newline(indent + 1);
                visit(a);
            }
            newline(indent);
            print("}");
        }
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (", ast.getCondition(),") {");
        for (Ast.Statement.Case a : ast.getCases()) {  visit(a);}
        newline(indent);
        print("}");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        indent++;
        newline(indent);
        indent++;
        if(ast.getValue().isPresent()){
        print("case ");
        visit(ast.getValue().get());
        print(":");
            for (Ast.Statement a : ast.getStatements()) { newline(indent); visit(a);}
        }
        else{
            print("default");
           print(":");
            for (Ast.Statement a : ast.getStatements()) { newline(indent); visit(a);}
        }
        indent --;
        indent --;

        return null;//TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");
        for (Ast.Statement a: ast.getStatements()) {
            newline(indent + 1);
            visit(a);
        }
        if(ast.getStatements().size() > 0){newline(indent);}
        print("}");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(typeis(Environment.Type.STRING, ast.getType())){print("\"", ast.getLiteral(), "\"");}
        else if(typeis(Environment.Type.CHARACTER, ast.getType())){print("\'", ast.getLiteral(), "\'");}
        else print(ast.getLiteral());
        return null;//TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;//TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String op  = operators(ast.getOperator());
        if(ast.getOperator().equals("^")){
            print(op);
            visit(ast.getLeft());
            print(", ");
            visit(ast.getRight());
            print(")");

        }
        else {
            visit(ast.getLeft());
            print(" ", op, " ");
            visit(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        if(ast.getOffset().isPresent()){
            print("[", ast.getOffset().get(),"]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName(), "(");
        for (Ast.Expression a : ast.getArguments()) { visit(a);}
        print(")");
        //newline(0);
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        for (int i = 0; i < ast.getValues().size(); i++) {

            visit(ast.getValues().get(i));
            if(i < ast.getValues().size() - 1) print(", ");
        }

        return null; //TODO
    }
    private boolean typeis(Environment.Type t, Environment.Type t2){
        return t == t2;
    }

    private String operators(String op){

        switch (op){
            case "AND":{
                return "&&";
            }
            case "OR":{
                return "||";
            }
            case "^" :{
                return "Math.pow(";
            }
        }
        return op;
    }


    private String types(String op){

        switch (op){
            case "Integer":{
                return "int";
            }
            case "Decimal":{
                return "double";
            }
            case "Boolean" :{
                return "boolean";
            }
            case "Character" :{
                return "char";
            }
            case "Any" :{
                return "Object";
            }
            case "Nil" :{
                return "Void";
            }
        }
        return op;
    }
}
