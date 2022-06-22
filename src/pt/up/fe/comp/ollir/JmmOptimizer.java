package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult){
        System.out.println("dentro do JmmOptimizer");
        var ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        var ollirCode = ollirGenerator.getCode();

//        String ollirCode = "import ioPlus;"
            //+"import BoardBase;"
            //+"import java.io.File;"
            //+""
            //+"class HelloWorld extends BoardBase{"
            //+"    public static void main(String[] args){"
            //+"        ioPlus.printHelloWorld();"
            //+"    }";

        System.out.println(ollirCode);
        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

}