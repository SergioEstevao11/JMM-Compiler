package pt.up.fe.comp.analysis.analyser.typeverification;

import org.eclipse.jgit.util.io.IsolatedOutputStream;
import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class AssignmentsCheck extends PreorderJmmVisitor<Integer, Integer> {
    private final SymbolTableBuilder symbolTable;
    private final List<Report> reports;

    public AssignmentsCheck(SymbolTableBuilder symbolTable, List<Report> reports) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        addVisit("Assignment", this::visitAssignment);

        setDefaultVisit((node, oi) -> 0);
    }
    public Integer visitAssignment(JmmNode assignmentNode,Integer ret){
        String method_name = null;
        String identifierType;
        boolean found = false;

        if(assignmentNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getKind().equals("MainMethodHeader")){
            method_name = "main";
            List<Symbol> mainMethodVariables = symbolTable.getLocalVariables(method_name);
            for (Symbol mainMethodVariable : mainMethodVariables) {
                if(assignmentNode.getJmmChild(0).getKind().equals("Identifier")){
                    if (assignmentNode.getJmmChild(0).get("name").equals(mainMethodVariable.getName())){
                        found = true;
                    }
                }
                else if(assignmentNode.getJmmChild(0).getKind().equals("ArrayAccess")){
                    if (assignmentNode.getJmmChild(0).getJmmChild(0).get("name").equals(mainMethodVariable.getName())){
                        found = true;
                    }
                }
            }
            if(!found){
                reports.add(Report.newError(Stage.SEMANTIC, -1, -1, " Variable not declared on main", null));
            }
        }
        else method_name = assignmentNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getJmmChild(1).get("name");

        if(assignmentNode.getJmmChild(0).getKind().equals("Identifier")) {
            identifierType = symbolTable.getVariableType(method_name, assignmentNode.getJmmChild(0).get("name")).getName();
        }
        else {
            identifierType = symbolTable.getVariableType(method_name,assignmentNode.getJmmChild(0).getJmmChild(0).get("name")).getName();
        }

        boolean isMathExpression = symbolTable.isMathExpression(assignmentNode.getJmmChild(1).getKind());
        boolean isBooleanExpression = symbolTable.isBooleanExpression(assignmentNode.getJmmChild(1).getKind());

        if(assignmentNode.getJmmChild(1).getKind().equals("Identifier")){
            if(symbolTable.isObject(method_name,assignmentNode.getJmmChild(1).get("name"))) {
                if(symbolTable.getSuper() == null){
                    if(assignmentNode.getJmmChild(0).getKind().equals("Identifier")) {
                        if (symbolTable.isObject(method_name, assignmentNode.getJmmChild(0).get("name"))) {
                            if (symbolTable.getVariableType(method_name, assignmentNode.getJmmChild(0).get("name")).getName().equals(symbolTable.getClassName())) {
                                reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Cannot assign a not imported class to an object", null));
                                return 1;
                            } else if (symbolTable.getImports().contains(symbolTable.getVariableType(method_name, assignmentNode.getJmmChild(0).get("name")).getName()) && symbolTable.getImports().contains(symbolTable.getVariableType(method_name, assignmentNode.getJmmChild(1).get("name")).getName()))
                                return 1;
                        }
                    }
                }
                else if(!symbolTable.getImports().isEmpty()) return 1;
            }
            if(!symbolTable.getVariableType(method_name,assignmentNode.getJmmChild(1).get("name")).getName().equals(identifierType)) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Cannot assign a int to a boolean", null));
        }
        else if(assignmentNode.getJmmChild(1).getKind().equals("DotAccess")){
            String call_method_name = null;
            String returnMethodType;
            if(assignmentNode.getJmmChild(1).getJmmChild(1).getJmmChild(0).getKind().equals("Length")){
                returnMethodType = "int";
            }
            else{
                call_method_name = assignmentNode.getJmmChild(1).getJmmChild(1).getJmmChild(0).get("name");
                if(!symbolTable.getMethods().contains(call_method_name)){
                    if(symbolTable.getSuper() != null || !symbolTable.getImports().isEmpty()) return 1;
                    else {
                        reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Method is missing.", null));
                        return 1;
                    }
                }
                else{
                    returnMethodType = symbolTable.getReturnType(call_method_name).getName();
                }
            }
            if(!returnMethodType.equals(identifierType)) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "\"" + assignmentNode.getJmmChild(0).getKind().equals("Number") + "\" invalid type: expecting a boolean.", null));
        }
        else if(assignmentNode.getJmmChild(1).getKind().equals("True") || assignmentNode.getJmmChild(1).getKind().equals("False") || isBooleanExpression){
            if(identifierType.equals("boolean")) return 1;
            else reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Invalid type: expecting a boolean.", null));
        }
        else if(assignmentNode.getJmmChild(1).getKind().equals("Number") || isMathExpression)
            if(identifierType.equals("int")) return 1;
            else reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Invalid type: expecting a int.", null));
        else if(assignmentNode.getJmmChild(1).getKind().equals("NewDeclaration")){
            return 1;
        }

        return 0;
    }

    public List<Report> getReports(){
        return reports;
    }
}
