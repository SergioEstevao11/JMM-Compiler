package pt.up.fe.comp.analysis.analyser.typeverification;

import jdk.swing.interop.SwingInterOpUtils;
import pt.up.fe.comp.Identifier;
import pt.up.fe.comp.MethodBody;
import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;


public class OperatorsCheck extends PreorderJmmVisitor<Integer, Integer> {
    private final SymbolTableBuilder symbolTable;
    private final List<Report> reports;
    public OperatorsCheck(SymbolTableBuilder symbolTable, List<Report> reports) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        addVisit("Times", this::visitPlus);
        addVisit("Plus", this::visitPlus);
        addVisit("Minus", this::visitPlus);
        addVisit("Less", this::visitPlus);
        addVisit("And", this::visitAnd);
        setDefaultVisit((node, oi) -> 0);
    }


    public Integer visitAnd(JmmNode andNode,Integer ret){

        String method_name = null;
        String left_side_type = null;
        String right_side_type = null;
        if( andNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getKind().equals("MainMethodHeader")) method_name = "main";
        else method_name = andNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getJmmChild(1).get("name");

        boolean isMathExpression = symbolTable.isMathExpression(andNode.getJmmChild(0).getKind());
        boolean isMathExpressionRight = symbolTable.isMathExpression(andNode.getJmmChild(1).getKind());

        if(andNode.getJmmChild(0).getKind().equals("Identifier")) {
            if(symbolTable.isArray(method_name, andNode.getJmmChild(0).get("name"))) {
                if(andNode.getJmmChild(1).getKind().equals("Identifier")) {
                    if (symbolTable.isArray(method_name, andNode.getJmmChild(1).get("name"))) {
                        reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't use and withou booleans", null));
                    } else
                        reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't use and withou booleans", null));
                }
            }
            left_side_type = symbolTable.getVariableType(method_name, andNode.getJmmChild(0).get("name")).getName();
        }
        else if(andNode.getJmmChild(0).getKind().equals("DotAccess")) {
            if(andNode.getJmmChild(0).getJmmChild(1).getJmmChild(0).getKind().equals("Length")){
                left_side_type = "int";
            }
            else{
                String call_method_name = andNode.getJmmChild(1).getJmmChild(0).get("name");
                left_side_type = symbolTable.getReturnType(call_method_name).getName();
            }
        }
        else if(andNode.getJmmChild(0).getKind().equals("ArrayAccess")){

            left_side_type = symbolTable.getVariableType(method_name,andNode.getJmmChild(0).getJmmChild(0).get("name")).getName();
        }
        else if(andNode.getJmmChild(0).getKind().equals("Number") || isMathExpression){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't use and withou booleans", null));
            return 1;
        }
        else{
            left_side_type = "boolean";
        }

        if(!left_side_type.equals("boolean")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't use and withou booleans", null));

        if (andNode.getJmmChild(1).getKind().equals("Identifier")) {
            if (symbolTable.isArray(method_name, andNode.getJmmChild(1).get("name"))){
                if(symbolTable.isArray(method_name, andNode.getJmmChild(0).get("name"))){
                    reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add two arrays", null));
                }
                else reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add int/boolean to an array", null));
            }
//            else if(symbolTable.isArray(method_name, plusNode.getJmmChild(0).get("name"))){
//                if(symbolTable.isArray(method_name, plusNode.getJmmChild(1).get("name"))){
//                    reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add two arrays", null));
//                }
//                else reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add int/boolean to an array", null));
//            }
            right_side_type = symbolTable.getVariableType(method_name, andNode.getJmmChild(1).get("name")).getName();

        }
        else if(andNode.getJmmChild(1).getKind().equals("DotAccess")) {
            if(andNode.getJmmChild(1).getJmmChild(1).getJmmChild(0).getKind().equals("Length")){
                right_side_type = "int";
            }
            else{
                String call_method_name = andNode.getJmmChild(1).getJmmChild(1).getJmmChild(0).get("name");
                right_side_type = symbolTable.getReturnType(call_method_name).getName();
            }
        }
        else if(andNode.getJmmChild(1).getKind().equals("ArrayAccess")){
            right_side_type = symbolTable.getVariableType(method_name,andNode.getJmmChild(1).getJmmChild(0).get("name")).getName();
        }
        else if(andNode.getJmmChild(1).getKind().equals("Number") || isMathExpressionRight){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add booleans", null));
            return 1;
        }
        else{
            right_side_type = "boolean";
        }

        if(!right_side_type.equals("boolean")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't use and without booleans", null));

        return 0;
    }

    public Integer visitPlus(JmmNode plusNode,Integer ret){
        String method_name = null;
        String left_side_type = null;
        String right_side_type = null;
        if( plusNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getKind().equals("MainMethodHeader")) method_name = "main";
        else method_name = plusNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getJmmChild(1).get("name");

        boolean isBooleanExpression = symbolTable.isBooleanExpression(plusNode.getJmmChild(0).getKind());
        boolean isBooleanExpressionRight = symbolTable.isBooleanExpression(plusNode.getJmmChild(1).getKind());

        if(plusNode.getJmmChild(0).getKind().equals("Identifier")) {
            if(symbolTable.isArray(method_name, plusNode.getJmmChild(0).get("name"))) {
                if(plusNode.getJmmChild(1).getKind().equals("Identifier")) {
                    if (symbolTable.isArray(method_name, plusNode.getJmmChild(1).get("name"))) {
                        reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add two arrays", null));
                    } else
                        reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add int/boolean to an array", null));
                }
            }
            left_side_type = symbolTable.getVariableType(method_name, plusNode.getJmmChild(0).get("name")).getName();
        }
        else if(plusNode.getJmmChild(0).getKind().equals("DotAccess")) {
            if(plusNode.getJmmChild(0).getJmmChild(1).getJmmChild(0).getKind().equals("Length")){
                left_side_type = "int";
            }
            else{
                String call_method_name = plusNode.getJmmChild(1).getJmmChild(0).get("name");
                left_side_type = symbolTable.getReturnType(call_method_name).getName();
            }
        }
        else if(plusNode.getJmmChild(0).getKind().equals("ArrayAccess")){

            left_side_type = symbolTable.getVariableType(method_name,plusNode.getJmmChild(0).getJmmChild(0).get("name")).getName();
        }
        else if(plusNode.getJmmChild(0).getKind().equals("True") || plusNode.getJmmChild(0).getKind().equals("False") || isBooleanExpression){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add booleans", null));
            return 1;
        }
        else{
            left_side_type = "int";
        }

        if(!left_side_type.equals("int")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add not ints", null));

        if (plusNode.getJmmChild(1).getKind().equals("Identifier")) {
            if (symbolTable.isArray(method_name, plusNode.getJmmChild(1).get("name"))){
                if(symbolTable.isArray(method_name, plusNode.getJmmChild(0).get("name"))){
                    reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add two arrays", null));
                }
                else reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add int/boolean to an array", null));
            }

            right_side_type = symbolTable.getVariableType(method_name, plusNode.getJmmChild(1).get("name")).getName();

        }
        else if(plusNode.getJmmChild(1).getKind().equals("DotAccess")) {
            if(plusNode.getJmmChild(1).getJmmChild(1).getJmmChild(0).getKind().equals("Length")){
                right_side_type = "int";
            }
            else{
                String call_method_name = plusNode.getJmmChild(1).getJmmChild(1).getJmmChild(0).get("name");
                right_side_type = symbolTable.getReturnType(call_method_name).getName();
            }
        }
        else if(plusNode.getJmmChild(1).getKind().equals("ArrayAccess")){
            right_side_type = symbolTable.getVariableType(method_name,plusNode.getJmmChild(1).getJmmChild(0).get("name")).getName();
        }
        else if(plusNode.getJmmChild(1).getKind().equals("True") || plusNode.getJmmChild(1).getKind().equals("False") || isBooleanExpressionRight){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Can't add booleans", null));
            return 1;
        }
        else{
            right_side_type = "int";
        }

        if (!left_side_type.equals(right_side_type)){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "variables of different type7", null));
        }

        return 0;
    }

    public List<Report> getReports(){
        return reports;
    }

}
