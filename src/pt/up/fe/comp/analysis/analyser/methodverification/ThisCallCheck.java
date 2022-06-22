package pt.up.fe.comp.analysis.analyser.methodverification;

import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class ThisCallCheck extends PreorderJmmVisitor<Integer, Integer> {

    private final SymbolTableBuilder symbolTable;
    private final List<Report> reports;

    public ThisCallCheck(SymbolTableBuilder symbolTable, List<Report> reports) {

        this.reports = reports;
        this.symbolTable = symbolTable;
        addVisit("ThisDeclaration", this::visitThisDeclaration);
        setDefaultVisit((node, oi) -> 0);
    }
    public Integer visitThisDeclaration(JmmNode thisNode, Integer ret) {
        if( thisNode.getAncestor("MethodDeclaration").get().getJmmChild(0).getKind().equals("MainMethodHeader")){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Cant have this on main method because it is static", null));
        }
        return 0;
    }
}
