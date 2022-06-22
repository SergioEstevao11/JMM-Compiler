package pt.up.fe.comp.analysis.analyser.typeverification;

import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class WhileIfConditionCheck extends PreorderJmmVisitor<Integer, Integer> {

    private final SymbolTableBuilder symbolTable;
    private final List<Report> reports;

    public WhileIfConditionCheck(SymbolTableBuilder symbolTable, List<Report> reports) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        addVisit("WhileStatement", this::visitIfWhileCondition);
        addVisit("IfStatement", this::visitIfWhileCondition);
        setDefaultVisit((node, oi) -> 0);
    }
    public Integer visitIfWhileCondition(JmmNode whileIfStatementNode, Integer ret) {
        String method_name = null;
        if( whileIfStatementNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getKind().equals("MainMethodHeader")) method_name = "main";
        else method_name = whileIfStatementNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getJmmChild(1).get("name");

        boolean isMathExpression = symbolTable.isMathExpression(whileIfStatementNode.getJmmChild(0).getKind());
        boolean isBooleanExpression = symbolTable.isBooleanExpression(whileIfStatementNode.getJmmChild(0).getKind());

        if(isMathExpression || whileIfStatementNode.getJmmChild(0).getKind().equals("Number"))  reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Invalid type: can't have an int on a while/if statement", null));

        else if(isBooleanExpression || whileIfStatementNode.getJmmChild(0).getKind().equals("True") || whileIfStatementNode.getJmmChild(0).getKind().equals("False") ) return 1;

        else if(whileIfStatementNode.getJmmChild(0).getKind().equals("DotAccess")){
            String call_method_name = whileIfStatementNode.getJmmChild(0).getJmmChild(1).getJmmChild(0).get("name");
            String returnMethodType = symbolTable.getReturnType(call_method_name).getName();
            if(!returnMethodType.equals("boolean")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Function did not return a boolean", null));
        }

        else if(!symbolTable.getVariableType(method_name,whileIfStatementNode.getJmmChild(0).get("name")).getName().equals("boolean")) reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Invalid type: has to be a boolean", null));
        return 1;
    }
    public List<Report> getReports(){
        return reports;
    }
}
