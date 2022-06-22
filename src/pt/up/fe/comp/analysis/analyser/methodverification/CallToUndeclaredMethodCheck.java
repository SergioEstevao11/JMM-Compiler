package pt.up.fe.comp.analysis.analyser.methodverification;

import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class CallToUndeclaredMethodCheck  extends PreorderJmmVisitor<Integer, Integer> {
    private final SymbolTableBuilder symbolTable;
    private final List<Report> reports;

    public CallToUndeclaredMethodCheck(SymbolTableBuilder symbolTable, List<Report> reports) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        addVisit("DotAccess", this::visitDotAccess);
        setDefaultVisit((node, oi) -> 0);
    }
    public Integer visitDotAccess(JmmNode dotAccessNode,Integer ret){
        if(dotAccessNode.getJmmChild(1).getJmmChild(0).getKind().equals("Length")){
            return 1;
        }
        String method_node_name = dotAccessNode.getJmmChild(1).getJmmChild(0).get("name");
        String method_name = null;

        if( dotAccessNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getKind().equals("MainMethodHeader")) method_name = "main";
        else method_name = dotAccessNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getJmmChild(1).get("name");

        if(symbolTable.getMethods().contains(method_node_name)){
            return 1;
        }

        if(symbolTable.getSuper() != null){
            return 1;
        }

        if(!symbolTable.getImports().isEmpty()) {
            return 1;
        }

        reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Method \"" + method_node_name + "\" is missing.", null));
        return 0;
    }

    public List<Report> getReports(){
        return reports;
    }
}
