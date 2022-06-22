package pt.up.fe.comp.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.analysis.analyser.methodverification.CallToUndeclaredMethodCheck;
import pt.up.fe.comp.analysis.analyser.methodverification.ThisCallCheck;
import pt.up.fe.comp.analysis.analyser.typeverification.*;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class JmmAnalyser implements JmmAnalysis{

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        var rootNode = parserResult.getRootNode();

        if (rootNode == null  || rootNode.getJmmParent() != null ) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null or had a parent");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        List<Report> reports = new ArrayList<>();
        var symbolTable = new SymbolTableBuilder();

        var symbolTableFiller = new SymbolTableFiller(symbolTable, reports);
        symbolTableFiller.visit(rootNode, null);
        reports.addAll(reports);



        var operatorsCheck = new OperatorsCheck(symbolTable,reports);
        operatorsCheck.visit(rootNode,null);

        var arrayIndexNotIntCheck = new ArrayIndexNotIntCheck(symbolTable,reports);
        arrayIndexNotIntCheck.visit(rootNode,null);

        var callToUndeclaredMethodCheck = new CallToUndeclaredMethodCheck(symbolTable,reports);
        callToUndeclaredMethodCheck.visit(rootNode,null);

        var assignmentsCheck = new AssignmentsCheck(symbolTable,reports);
        assignmentsCheck.visit(rootNode,null);

        var arrayInWhileIfCondition = new WhileIfConditionCheck(symbolTable,reports);
        arrayInWhileIfCondition.visit(rootNode,null);

        var incompatibleArguments = new IncompatibleArgumentsCheck(symbolTable,reports);
        incompatibleArguments.visit(rootNode,null);

        var incompatibleReturnCheck = new IncompatibleReturnCheck(symbolTable,reports);
        incompatibleReturnCheck.visit(rootNode,null);

        var thisCallCheck = new ThisCallCheck(symbolTable,reports);
        thisCallCheck.visit(rootNode,null);

        System.out.println(reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
