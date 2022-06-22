package pt.up.fe.comp.jmm.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.ElseScope;
import pt.up.fe.comp.Int;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;


public class Backend implements JasminBackend{
    private String className;
    private String extendsDef;
    private ArrayList<String> imports;
    int stacklimit;
    int stack;
    private int conditionals;
    private int comparisons;
    private HashMap<String, Descriptor> currVarTable;
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();
        try {

            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method

            this.className = ollirClass.getClassName();
            this.extendsDef = ollirClass.getSuperClass();
            this.imports = ollirClass.getImports();
            this.conditionals = 0;
            this.comparisons = 0;

            StringBuilder jasminCode = new StringBuilder();


            jasminCode.append(this.generateClassDecl(ollirClass)); //builds the class declaration
            jasminCode.append(this.generateClassMethods(ollirClass)); //builds the class methods


            List<Report> reports = new ArrayList<>();

            return new JasminResult(ollirResult, jasminCode.toString(), reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Error in Jasmin generation", e)));
        }

    }

    private String generateClassDecl(ClassUnit ollirClass) {
        StringBuilder classCode = new StringBuilder();

        // Class: Definition
        classCode.append(".class");
        if (ollirClass.getClassAccessModifier() != AccessModifiers.DEFAULT) {
            classCode.append(" ").append(ollirClass.getClassAccessModifier().toString().toLowerCase());
        }
        else {
            classCode.append(" public");
        }

        classCode.append(" ").append(className).append("\n");

        // Class: Extends
        classCode.append(".super ").append(generateSuper()).append("\n");

        // Class: Fields
        for(Field field: ollirClass.getFields()) {
            classCode.append(this.generateClassField(field));
        }

        return classCode.toString();
    }
    private String generateClassField(Field field) {
        StringBuilder FieldCode = new StringBuilder();

        FieldCode.append(".field");

        if (field.getFieldAccessModifier() != AccessModifiers.DEFAULT) {
            FieldCode.append(" ").append(field.getFieldAccessModifier().toString().toLowerCase());
        }else{
            FieldCode.append(" public");
        }

        if (field.isStaticField()) {
            FieldCode.append(" static");
        }
        if (field.isFinalField()) {
            FieldCode.append(" final");
        }

        FieldCode.append(" " + field.getFieldName() + " " + getJasminType(field.getFieldType()));

        if (field.isInitialized()) {
            FieldCode.append(" = ").append(field.getInitialValue());
        }
        FieldCode.append("\n");
        return FieldCode.toString();
    }

    private String generateSuper() {
        return this.extendsDef == null ? "java/lang/Object" : this.extendsDef;
    }

    private String generateClassMethods(ClassUnit ollirClass) {
        StringBuilder classMethodsCode = new StringBuilder();

        for(Method method: ollirClass.getMethods()) {
            conditionals = 0;
            stacklimit = 0;
            stack = 0;
            comparisons = 0;

            classMethodsCode.append("\n")
                            .append(generateClassMethodHeader(method))
                            .append(generateClassMethodBody(method));
        }

        return classMethodsCode.toString();
        
    }

    private String generateClassMethodHeader(Method method) {
        StringBuilder methodHeaderCode = new StringBuilder();

        methodHeaderCode.append(".method");

        if (method.getMethodAccessModifier() != AccessModifiers.DEFAULT)
            methodHeaderCode.append(" ").append(method.getMethodAccessModifier().toString().toLowerCase());
        else
            methodHeaderCode.append(" public");

        if (method.isStaticMethod())
            methodHeaderCode.append(" static");
        if (method.isFinalMethod())
            methodHeaderCode.append(" final");

        if (method.isConstructMethod())
            methodHeaderCode.append(" <init>");
        else
            methodHeaderCode.append(" ").append(method.getMethodName());

        methodHeaderCode.append("(");
        for (Element param : method.getParams())
            methodHeaderCode.append(getJasminType(param.getType()));

        methodHeaderCode.append(")").append(getJasminType(method.getReturnType())).append("\n");

        return methodHeaderCode.toString();
    }

    private String generateClassMethodBody(Method method) {



        StringBuilder methodBodyCode = new StringBuilder();
        StringBuilder instructions = new StringBuilder();
        currVarTable = method.getVarTable();
        HashMap<String, Instruction> labels = method.getLabels();


        for (int i = 0; i < method.getInstructions().size(); i++) {
            Instruction instruction = method.getInstr(i);
            for (String key : labels.keySet()) {

                if (labels.get(key).getId() == instruction.getId()) {
                    instructions.append(key).append(":\n");
                }
            }

            //gets instruction code
            instructions.append(getJasminInst(instruction));

            if (instruction.getInstType() == InstructionType.CALL) {
                if (((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
                    instructions.append("\tpop\n");
                }
            }
        }

        methodBodyCode.append(generateStackLimits());

        HashSet<Integer> locals = new HashSet<>();
        for (Descriptor d : currVarTable.values()) {
            if (!locals.contains(d.getVirtualReg()))
                locals.add(d.getVirtualReg());
        }
        if (!locals.contains(0) && !method.isConstructMethod())
            locals.add(0);
        Integer localstack = locals.size();

        methodBodyCode.append("\t.limit locals ").append(localstack).append("\n\n");

        methodBodyCode.append(instructions);

        if (method.isConstructMethod())
            methodBodyCode.append("\treturn\n");

        methodBodyCode.append(".end method\n");

        return methodBodyCode.toString();
    }


    private String getJasminType(Type type){
        if (type.getTypeOfElement() == ElementType.ARRAYREF){
            return "[" + getJasminType( ((ArrayType)type).getTypeOfElements());
        }
        else if(type.getTypeOfElement() == ElementType.OBJECTREF) {
            String className = ((ClassType) type).getName();
            for (String imported : this.imports) {
                if (imported.endsWith("." + className))
                    return  "L" + imported.replace('.', '/') + ";";
            }
            return  "L" + className + ";";
        }


        return getJasminType(type.getTypeOfElement());
    }

    private String getJasminType(ElementType type) {
        String jasminType;

        if(type == ElementType.INT32) {jasminType = "I";}
        else if(type == ElementType.BOOLEAN) {jasminType = "Z";}
        else if(type == ElementType.VOID) {jasminType = "V";}
        else if(type == ElementType.STRING) {jasminType = "Ljava/lang/String;";}
        else{throw new IllegalStateException("Unexpected JasminType");}

        return jasminType;
    }

    private String getJasminInst(Instruction instr) {

        if (instr instanceof SingleOpInstruction)
            return this.generateSingleOp((SingleOpInstruction) instr);
        if (instr instanceof AssignInstruction)
            return this.generateAssignOp((AssignInstruction) instr);
        if (instr instanceof BinaryOpInstruction)
            return this.generateBinaryOp((BinaryOpInstruction) instr);
        if (instr instanceof CallInstruction)
            return this.generateCallOp((CallInstruction) instr);
        if (instr instanceof GotoInstruction)
            return this.generateGotoOp((GotoInstruction) instr);
        if (instr instanceof ReturnInstruction)
            return this.generateReturnOp((ReturnInstruction) instr);
        if (instr instanceof CondBranchInstruction)
            return this.generateBranchOp((CondBranchInstruction) instr);
        if (instr instanceof GetFieldInstruction)
            return this.generateGetFieldOp((GetFieldInstruction) instr);
        if (instr instanceof PutFieldInstruction)
            return this.generatePutFieldOp((PutFieldInstruction) instr);
        if (instr instanceof UnaryOpInstruction){
            return this.generateUnaryOp((UnaryOpInstruction) instr);
        }
        return "ERROR: instruction doesn't exist";

    }

    private String generateUnaryOp(UnaryOpInstruction instr) {
        OperationType opType = instr.getOperation().getOpType();
        Element leftElem = instr.getOperands().get(0);
        String branchOpCode = loadElement(leftElem);

        if (opType == OperationType.NOTB || opType == OperationType.NOT) {
            comparisons++;

            branchOpCode +=  "\tifne NOTB" + comparisons + "\n" +
                    "\ticonst_1" + "\n" +
                    "\tgoto " + "Continue" + comparisons + "\n" +
                    "NOTB" + comparisons + ":\n" +
                    "\ticonst_0" + "\n" +
                    "Continue" + comparisons + ":\n";

            limitStack(stack);
            stack -= 1;
        }

        return branchOpCode;

    }

        private String generatePutFieldOp(PutFieldInstruction instr) {

        String jasminCode = loadElement(instr.getFirstOperand())
                + loadElement(instr.getThirdOperand()) + "\tputfield "
                + getObjectName(((Operand) instr.getFirstOperand()).getName())
                + "/" + ((Operand) instr.getSecondOperand()).getName()
                + " " + getJasminType(instr.getSecondOperand().getType()) + "\n";

        limitStack(stack);
        stack = 0;
        return jasminCode;

    }

    private String generateGetFieldOp(GetFieldInstruction instr) {

        Operand op = (Operand) instr.getSecondOperand();
        Element elem = instr.getFirstOperand();
        String getCode = loadElement(elem) + "\tgetfield "
                + getObjectName(((Operand) elem).getName())
                + "/" + op.getName()
                + " " + getJasminType(instr.getFieldType()) + "\n";
        limitStack(stack);
        stack = 0;
        return getCode;
    }

    private String generateBranchOp(CondBranchInstruction instr) {


        Element leftElem = instr.getOperands().get(0);
        String branchOpCode = loadElement(leftElem);

        OperationType opCondType = null;
        Instruction condInstr = instr.getCondition();

        if (condInstr instanceof OpInstruction) {
            Element rightElem = instr.getOperands().get(1);
            OpInstruction opCondInstr = (OpInstruction) condInstr;
            opCondType = opCondInstr.getOperation().getOpType();
            if (opCondType == OperationType.AND || opCondType == OperationType.ANDB) {
                comparisons++;
                limitStack(1);
                stack = 0;

                branchOpCode += "\tifeq False" + comparisons + "\n" +
                        loadElement(rightElem) +
                        "\tifeq False" + comparisons + "\n" +
                        "\tgoto " + instr.getLabel() + "\n" +
                        "False" + comparisons + ":\n";

            }
            else if(opCondType == OperationType.OR || opCondType == OperationType.ORB) {


                //TODO

            }else {

                branchOpCode += loadElement(rightElem)
                        + "\t" + getJasminBranchComparison(opCondInstr.getOperation()) + " " + instr.getLabel() + "\n";
            }
        }
        else{
            comparisons++;

            branchOpCode +=  "\tifeq False" + comparisons + "\n" +
                    "\tgoto " + instr.getLabel() + "\n" +
                    "False" + comparisons + ":\n";
        }


        limitStack(stack);
        stack = 0;
        return branchOpCode;
    }

    private String generateReturnOp(ReturnInstruction instr) {
        StringBuilder returnOpCode = new StringBuilder();

        if (!instr.hasReturnValue())
            return "\treturn\n";

        ElementType returnType = instr.getOperand().getType().getTypeOfElement();
        returnOpCode.append(loadElement(instr.getOperand())).append("\t");

        if (returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN){
            returnOpCode.append("i");
        }else{
            returnOpCode.append("a");
        }

        returnOpCode.append("return\n");
        limitStack(stack);
        stack = 0;
        return returnOpCode.toString();
    }

    private String generateGotoOp(GotoInstruction instr) {
        return "\tgoto " + instr.getLabel() + "\n";
    }


    private String generateCallOp(CallInstruction instr) {
        switch (instr.getInvocationType()){
            case invokevirtual:
                return generateInvokeVirtual(instr);

            case invokespecial:
                return generateInvokeSpecial(instr);

            case invokestatic:
                return generateInvokeStatic(instr);
                
            case NEW:
                return generateNew(instr);
                
            case arraylength:
                return generateArrayLength(instr);
                
            case ldc:
                return generateLdc(instr);

            default:
                throw new IllegalStateException("Error");
        }

    }

    private String generateLdc(CallInstruction instr) {
        return loadElement(instr.getFirstArg());
    }

    private String generateArrayLength(CallInstruction instr) {
        return loadElement(instr.getFirstArg()) + "\tarraylength\n";
    }

    private String generateNew(CallInstruction instr) {
        StringBuilder jasminCode = new StringBuilder();

        for (Element e : instr.getListOfOperands()) {
            jasminCode.append(loadElement(e));
        }

        if (instr.getReturnType().getTypeOfElement() == ElementType.ARRAYREF) {

            jasminCode.append("\tnewarray ");

            if (instr.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.INT32) {
                jasminCode.append("int\n");
            }
            else if (instr.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.STRING) {
                jasminCode.append("Ljava/lang/String\n");
            } else {
                jasminCode.append("GENERATENEW TYPE NOT IMPLEMENTED\n");
            }

        }
        else if (instr.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {

            jasminCode.append("\tnew ")
                    .append(((Operand) instr.getFirstArg()).getName()).append("\n")
                    .append("\tdup\n");
        } else {
            jasminCode.append("GENERATENEW TYPE NOT IMPLEMENTED\n");
        }

        limitStack(stack);
        stack = 1;
        return jasminCode.toString();
    }

    private String generateInvokeSpecial(CallInstruction instr) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(loadElement(instr.getFirstArg()));
        limitStack(stack);
        String invokedClassName = ((ClassType) instr.getFirstArg().getType()).getName();


        jasminCode.append("\tinvokespecial ")
                .append((instr.getFirstArg().getType().getTypeOfElement() == ElementType.THIS) ? generateSuper() : invokedClassName)
                .append("/<init>(");

        for (Element e : instr.getListOfOperands())
            jasminCode.append(getJasminType(e.getType()));

        jasminCode.append(")").append(getJasminType(instr.getReturnType())).append("\n");
        stack = 0;
        return jasminCode.toString();
    }



    private String generateInvokeVirtual(CallInstruction instr) {
        StringBuilder jasminCode = new StringBuilder();


        jasminCode.append(loadElement(instr.getFirstArg()));



        for (Element e : instr.getListOfOperands())
            jasminCode.append(loadElement(e));

        limitStack(stack + 1);
        stack = (instr.getReturnType().getTypeOfElement() == ElementType.VOID) ? 0 : 1;


        jasminCode.append("\tinvokevirtual ")
                .append(getObjectName(((ClassType) instr.getFirstArg().getType()).getName()))
                .append(".").append(((LiteralElement) instr.getSecondArg()).getLiteral().replace("\"", ""))
                .append("(");

        for (Element e : instr.getListOfOperands())
            jasminCode.append(getJasminType(e.getType()));

        jasminCode.append(")").append(getJasminType(instr.getReturnType())).append("\n");

        return jasminCode.toString();
    }

    private String generateInvokeStatic(CallInstruction instr) {
        StringBuilder jasminCode = new StringBuilder();

        for (Element e : instr.getListOfOperands())
            jasminCode.append(loadElement(e));

        limitStack(stack);

        jasminCode.append("\tinvokestatic ")
                .append(getObjectName(((Operand) instr.getFirstArg()).getName()))
                .append(".").append(((LiteralElement) instr.getSecondArg()).getLiteral().replace("\"", ""))
                .append("(");

        for (Element e : instr.getListOfOperands())
            jasminCode.append(getJasminType(e.getType()));

        jasminCode.append(")").append(getJasminType(instr.getReturnType())).append("\n");
        stack = (instr.getReturnType().getTypeOfElement() == ElementType.VOID) ? 0 : 1;
        return jasminCode.toString();
    }

    private String getObjectName(String name) {
        if (name.equals("this"))
            return className;
        return name;
    }

    private String generateBinaryOp(BinaryOpInstruction instr) {

        OperationType opType = instr.getOperation().getOpType();

        if (opType == OperationType.NOTB || opType == OperationType.NOT) {
            conditionals++;
            limitStack(1);
            stack = 0;

            String jasminCode = loadElement(instr.getLeftOperand());
            if (((Operand) instr.getRightOperand()).getName().equals(
                    ((Operand) instr.getLeftOperand()).getName())) {
                jasminCode += "\tifeq";

            } else {
                jasminCode += loadElement(instr.getRightOperand()) +
                        "\t" + getJasminBranchComparison(instr.getOperation());
            }

            return jasminCode + " True" + conditionals + "\n" +
                    "\ticonst_0\n" +
                    "\tgoto Store" + conditionals + "\n" +
                    "True" + conditionals + ":\n" +
                    "\ticonst_1\n" +
                    "Store" + conditionals + ":\n";
        }

        if (opType == OperationType.ANDB || opType == OperationType.AND) {
            conditionals++;
            limitStack(1);
            stack = 0;

            return  loadElement(instr.getLeftOperand()) +
                    "\tifeq False" + conditionals + "\n" +
                    loadElement(instr.getRightOperand()) +
                    "\tifeq False" + conditionals + "\n" +
                    "\ticonst_1\n" +
                    "\tgoto Store" + conditionals + "\n" +
                    "False" + conditionals + ":\n" +
                    "\ticonst_0\n" +
                    "Store" + conditionals + ":\n";
        }

        if (opType == OperationType.LTH) {
            conditionals++;
            limitStack(1);
            stack = 0;

            String jasminCode = loadElement(instr.getLeftOperand()) +
                    loadElement(instr.getRightOperand()) +
                    "\t" + getJasminBranchComparison(instr.getOperation()) + " True" + conditionals + "\n" +
                    "\ticonst_0\n" +
                    "\tgoto Store" + conditionals + "\n" +
                    "True" + conditionals + ":\n" +
                    "\ticonst_1\n" +
                    "Store" + conditionals + ":\n";

            return jasminCode;
        }


        return loadElement(instr.getLeftOperand()) + loadElement(instr.getRightOperand())
                + "\t" + getJasminNumOperation(instr.getOperation()) + "\n";
    }



    private String getJasminNumOperation(Operation operation) {
        OperationType opType = operation.getOpType();

        if (opType == OperationType.ADD)
            return "iadd";

        if ( opType == OperationType.SUB)
            return "isub";

        if ( opType == OperationType.MUL)
            return "imul";

        if ( opType == OperationType.DIV)
            return "idiv";

        return "ERROR operation not implemented yet";
    }

    private String generateAssignOp(AssignInstruction instr) {

        StringBuilder jasminCode = new StringBuilder();
        Operand op = (Operand) instr.getDest();
        int reg = currVarTable.get(op.getName()).getVirtualReg();

        if (instr.getRhs().getInstType() == InstructionType.BINARYOPER) {

            BinaryOpInstruction binOp = (BinaryOpInstruction) instr.getRhs();
            Element leftOp = binOp.getLeftOperand();
            Element rightOp = binOp.getRightOperand();

            if (binOp.getOperation().getOpType() == OperationType.ADD) {


                if (!leftOp.isLiteral() && rightOp.isLiteral()) {

                    Integer rightOpValue = Integer.parseInt(((LiteralElement) rightOp).getLiteral());

                    if (((Operand) leftOp).getName().equals(op.getName())
                            && rightOpValue <= Byte.MAX_VALUE && rightOpValue >= Byte.MIN_VALUE) {
                        return "\tiinc " + reg + " " + rightOpValue + "\n";
                    }
                }
                else if (leftOp.isLiteral() && !rightOp.isLiteral()) {

                    Integer leftOpValue = Integer.parseInt(((LiteralElement) leftOp).getLiteral());
                    if (((Operand) rightOp).getName().equals(op.getName())
                            && leftOpValue <= Byte.MAX_VALUE && leftOpValue >= Byte.MIN_VALUE) {
                        return "\tiinc " + reg + " " + leftOpValue + "\n";
                    }
                }
            }
        }

        if (op.getType().getTypeOfElement() != ElementType.ARRAYREF){
            if (currVarTable.get(op.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {

                ArrayOperand arrayOp = (ArrayOperand) op;
                Element index = arrayOp.getIndexOperands().get(0);

                jasminCode.append(loadDescriptor(currVarTable.get(op.getName())))
                        .append(loadElement(index));
            }
        }

        if (op.getType().getTypeOfElement() == ElementType.INT32 || op.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            if (currVarTable.get(op.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF)
                stack += 1;
        }

        jasminCode.append(getJasminInst(instr.getRhs()));

        if (op.getType().getTypeOfElement() == ElementType.INT32 || op.getType().getTypeOfElement() == ElementType.BOOLEAN) {

            if (currVarTable.get(op.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                jasminCode.append("\tiastore\n");

                limitStack(stack);
                stack = 0;

                return jasminCode.toString();
            } else
                jasminCode.append("\tistore");
        } else {
            jasminCode.append("\tastore");
        }

        if(reg <=3){
            jasminCode.append("_");
        }else{
            jasminCode.append(" ");
        }

        jasminCode.append(reg).append("\n");

        limitStack(stack);
        stack = 0;

        return jasminCode.toString();
    }

    private String generateSingleOp(SingleOpInstruction instr) {
        String singleOpCode = loadElement(instr.getSingleOperand());
        return singleOpCode;
    }

    private String loadElement(Element elem) {
        if (elem.isLiteral())
            return loadLiteral((LiteralElement) elem);
        if (elem.getType().getTypeOfElement() == ElementType.BOOLEAN){
            String name = ((Operand) elem).getName();
            if (name.equals("true") || name.equals("false"))
                return loadBoolean(elem);
        }

        Descriptor descriptor = currVarTable.get(((Operand) elem).getName());
        if (descriptor == null)
            return "NULL";

        try {
            if (elem.getType().getTypeOfElement() != ElementType.ARRAYREF
                    && descriptor.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                ArrayOperand arrayOp = (ArrayOperand) elem;
                Element index = arrayOp.getIndexOperands().get(0);
                return loadDescriptor(descriptor) + loadElement(index) + "\tiaload\n";
            }
        } catch (NullPointerException | ClassCastException except) {
            System.out.println(((Operand) elem).getName() + " is invalid");
        }

        return loadDescriptor(descriptor);
    }

    private String loadBoolean(Element elem){
        String jasminCode = "\t";
        stack += 1;

        String name = ((Operand) elem).getName();

        if (name.equals("true")){
            return jasminCode + "iconst_1" + "\n";
        }
        else{
            return jasminCode + "iconst_0" + "\n";
        }
    }

    private String loadDescriptor(Descriptor descriptor) {
        StringBuilder jasminCode = new StringBuilder("\t");
        stack += 1;

        ElementType t = descriptor.getVarType().getTypeOfElement();
        if (t == ElementType.THIS) {
            jasminCode.append("aload_0\n");
            return jasminCode.toString();
        }

        if (t == ElementType.INT32 || t == ElementType.BOOLEAN){
            jasminCode.append("i");
        }else{
            jasminCode.append("a");
        }

        jasminCode.append("load");
        int virtualReg = descriptor.getVirtualReg();
        if (virtualReg <=3){
            jasminCode.append("_");
        }else{
            jasminCode.append(" ");
        }

        jasminCode.append(virtualReg).append("\n");

        return jasminCode.toString();
    }

    private String loadLiteral(LiteralElement elem) {
        stack += 1;
        String jasminCode = "\t";
        int elemValue = Integer.parseInt(elem.getLiteral());
        ElementType elemType = elem.getType().getTypeOfElement();
        if (elemType == ElementType.INT32 || elemType == ElementType.BOOLEAN) {
            if (elemValue <= 5 && elemValue >= -1)
                jasminCode += "iconst_";
            else if (elemValue <= 127)
                jasminCode += "bipush ";
            else if (elemValue <= Short.MAX_VALUE)
                jasminCode += "sipush ";
            else
                jasminCode += "ldc ";
        } else
            jasminCode += "ldc ";

        if (elemValue == -1)
            return jasminCode + "m1\n";

        return jasminCode + elemValue + "\n";
    }


    private String getJasminBranchComparison(Operation operation) {
        String jasminCode = "isub" + "\n" + "\t";
        switch (operation.getOpType()) {
            case LTE:
                jasminCode += "ifle";
                return jasminCode;
            case LTH:
                jasminCode += "iflt";
                return jasminCode;
            case GTE:
                jasminCode += "ifge";
                return jasminCode;
            case GTH:
                jasminCode += "ifge";
                return jasminCode;
            case EQ:
                jasminCode += "ifeq";
                return jasminCode;
            case NOTB: case NEQ:
                jasminCode += "ifne";
                return jasminCode;
            default:
                System.out.println(operation.getOpType());
                return "ERROR comparison not implemented yet";
        }
    }

    //Stack Functions
    private String generateStackLimits()
    {
        Integer res = stacklimit;
        return "\t.limit stack " + res + "\n";
    }

    private void limitStack(int s) {
        if (s > stacklimit)
            stacklimit = s;
    }

}
