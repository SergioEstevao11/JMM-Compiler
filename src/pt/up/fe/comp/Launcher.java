package pt.up.fe.comp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.Backend;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();
        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);
        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

         // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();
        // Analysis stage
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);
        // Check if there are parsing errors
        TestUtils.noErrors(analysisResult.getReports());

        // Instantiate JmmOptimizer
        JmmOptimizer optimizer = new JmmOptimizer();
        // Optimization stage
        OllirResult ollirResult = optimizer.toOllir(analysisResult);
        // Check if there are optimization errors
        TestUtils.noErrors(ollirResult.getReports());

        System.out.println("ola54");

        // Jasmin
        var jasminBackend = new Backend();
        // Optimization stage
        var jasminResults = jasminBackend.toJasmin(ollirResult);
        // Check if there are optimization errors
        TestUtils.noErrors(jasminResults);

        System.out.println("ola");
        Path mainDir = Paths.get("ToolResults/");
        try {
            if (!Files.exists(mainDir)) {
                Files.createDirectory(mainDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path path = Paths.get("ToolResults/" + ollirResult.getSymbolTable().getClassName() + "/");
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/ast.json");
            myWriter.write(parserResult.toJson());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/symbolTable.txt");
            myWriter.write(analysisResult.getSymbolTable().print());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/ollir.ollir");
            myWriter.write(ollirResult.getOllirCode());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/jasmin.j");
            myWriter.write(jasminResults.getJasminCode());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        jasminResults.compile(path.toFile());
    }

}
