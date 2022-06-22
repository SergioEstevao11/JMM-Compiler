package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class OllirUtils {

    public static String getCode(Symbol symbol){

        return symbol.getName() + getOllirType(symbol.getType());
    }

    public static String getOllirType(Type type){
        StringBuilder code = new StringBuilder();
        code.append(".");

        if (type.isArray())
            code.append("array.");

        String jmmType = type.getName();

        switch(jmmType){
            case "int":
                code.append("i32");
                break;
            case "boolean":
                code.append("bool");
                break;
            case "void":
                code.append("V");
                break;
            default:
                code.append(jmmType);
                break;
        }

        return code.toString();
    }

    public static boolean isOperation(JmmNode operation) {
        return operation.getKind().equals("Plus") || operation.getKind().equals("Minus") ||
                operation.getKind().equals("Times") || operation.getKind().equals("Divide") ||
                operation.getKind().equals("Less") || operation.getKind().equals("And") || operation.getKind().equals("Not");
    }

    public static boolean isFinalOperation(JmmNode operation) {
        return operation.getKind().equals("Less") || operation.getKind().equals("And") || operation.getKind().equals("Not");
    }

    public static String getOllirOperator(JmmNode jmmOperator){

        switch (jmmOperator.getKind()){
            case "Plus":
                return ".i32";
            case "Less":
                return ".bool";
            case "Minus":
                return ".i32";
            case "Times":
                return ".i32";
            case "Divide":
                return ".i32";
            case "And":
                return ".bool";
            case "Not":
                return ".bool";
            default:
                return ".V";
        }
    }

}