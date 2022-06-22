package cp2lecture;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

import static org.junit.Assert.*;

public class AnalyseTest {
    @Test
    public void test(){
         var results = TestUtils.analyse(SpecsIo.getResource("cp2lecture/HelloWorld.jmm"));
        // var results = TestUtils.analyse(SpecsIo.getResource("/home/sofia/Desktop/MIEIC-2semestre/C/comp2022-9g/test/fixtures/public/cp2/SymbolTable.jmm));
         System.out.println("Symbol Table: " + results.getSymbolTable().print());
         TestUtils.noErrors(results);
    }

    /*
    @Test
    public void testVariableExistenceCorrect() {
        JmmSemanticsResult result = TestUtils.analyse("class ola {public int foo(int arg){arg = 0; return 0;}}");
        TestUtils.noErrors(result.getReports());
        result = TestUtils.analyse("class ola {int a; public int foo(){a = 0; return 0;}}");
        TestUtils.noErrors(result.getReports());
        result = TestUtils.analyse("class ola {public int foo(){int a; a = 0; a = 2; return 0;}}");
        TestUtils.noErrors(result.getReports());
    }*/
}
