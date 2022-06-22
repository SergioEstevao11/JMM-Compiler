package pt.up.fe.comp.analysis.analyser.typeverification;

import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class IncompatibleReturnCheck extends PreorderJmmVisitor<Integer, Integer> {
    private final SymbolTableBuilder symbolTable;
    private final List<Report> reports;

    public IncompatibleReturnCheck(SymbolTableBuilder symbolTable, List<Report> reports) {

        this.reports = reports;
        this.symbolTable = symbolTable;
        addVisit("Return", this::visitReturn);
        setDefaultVisit((node, oi) -> 0);
    }
    public Integer visitReturn(JmmNode returnStatementNode, Integer ret) {
        String method_name = null;
        JmmNode left_node = returnStatementNode.getJmmChild(0);

        if( returnStatementNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getKind().equals("MainMethodHeader")) method_name = "main";
        else method_name = returnStatementNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getJmmChild(1).get("name");

        String methodReturnType = symbolTable.getReturnType(method_name).getName();

        boolean isMathExpression = symbolTable.isMathExpression(left_node.getKind());
        boolean isBooleanExpression = symbolTable.isBooleanExpression(left_node.getKind());

        if(returnStatementNode.getJmmChild(0).getKind().equals("DotAccess")) {
            String call_method_name = returnStatementNode.getJmmChild(0).getJmmChild(1).getJmmChild(0).get("name");
            String returnMethodType = null;
            if(!symbolTable.getMethods().contains(call_method_name)) {
                if (symbolTable.getSuper() != null) return 1;
                if (symbolTable.getImports().isEmpty()) {
                    reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Function does not exist", null));
                }
                return 1;
            }
            else{
                returnMethodType = symbolTable.getReturnType(call_method_name).getName();
            }
            if(!returnMethodType.equals(methodReturnType)) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Didn't expect return type " + returnMethodType , null));
        }
        else if(returnStatementNode.getJmmChild(0).getKind().equals("Identifier")){
            if(!symbolTable.getVariableType(method_name,left_node.get("name")).getName().equals(methodReturnType)) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Didn't expect that return type", null));
        }
        else if(isMathExpression ){
            if(!methodReturnType.equals("int")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Didn't expect  return type int", null));
        }
        else if (isBooleanExpression){
            if(!methodReturnType.equals("boolean")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Didn't expect return type boolean", null));
        }
        else {
            if(returnStatementNode.getJmmChild(0).getKind().equals("Number")){
                if(!methodReturnType.equals("int")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "\"" + left_node + "\" invalid return type4", null));

            }
            else {
                if(!methodReturnType.equals("boolean"))  reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "\"" + left_node + "\" invalid return type5", null));
            }

        }
        return 1;
    }
    public List<Report> getReports(){
        return reports;
    }

}
