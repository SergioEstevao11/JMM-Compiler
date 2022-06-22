package pt.up.fe.comp.analysis;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.swing.interop.SwingInterOpUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class SymbolTableBuilder implements SymbolTable {

    private final List<String> imports;
    private String className;
    private String superClass;

    private final List<String> methods;
    private final Map<String, Type> methodReturnType;
    private final Map<String, List<Symbol>> methodParams;
    private final Map<String, List<Symbol>> methodLocalVariables;
    private final Map<Symbol, Boolean> fields;

    public SymbolTableBuilder() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superClass = null;
        this.methods = new ArrayList<>();
        this.methodReturnType = new HashMap<>();
        this.methodParams = new HashMap<>();
        this.fields = new HashMap<>();
        this.methodLocalVariables = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void addImport(String importString) {
        imports.add(importString);
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(this.fields.keySet());
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methodReturnType.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        for (String method : this.methods){
            if (method.equals(methodSignature)){
                return methodParams.get(methodSignature);
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature){
        var localVariables = methodLocalVariables.get(methodSignature);
        if(localVariables == null){
            localVariables = new ArrayList<>();
        }
        return localVariables;
    }

    public static Type getType(JmmNode node, String attribute) {
        Type type;
        if (node.get(attribute).equals("int[]"))
            type = new Type("int", true);
        else if (node.get(attribute).equals("int"))
            type = new Type("int", false);
        else
            type = new Type(node.get(attribute), false);

        return type;
    }

    public void addField(Symbol field) {
        fields.put(field, false);
    }

    public void addLocalVariable(String methodName, Symbol symbol) {
        var localVariables = getLocalVariables(methodName);
        localVariables.add(symbol);
        methodLocalVariables.put(methodName, localVariables);
    }

    public void addMethod(String methodSignature, Type returnType, List<Symbol> params) {
        methods.add(methodSignature);
        methodReturnType.put(methodSignature, returnType);
        methodParams.put(methodSignature, params);
    }

    public void setClassName(String className) {
        this.className = className;
    }


    public Object setSuperClass(String superClass) {
        this.superClass = superClass;

        return this.superClass;
    }

    public boolean hasField(String name) {
        for (Symbol field : this.fields.keySet()) {
            if (field.getName().equals(name))
                return true;
        }
        return false;
    }

    public boolean hasMethod(String methodName) {
        return false;
    }

    public Type getVariableType(String methodName,String variable){
        //check if variable is local
        for (Symbol symbol : getLocalVariables(methodName)){
            if(symbol.getName().equals(variable)){
                return symbol.getType();
            }
        }
        //check if it is class variable
        for (Symbol symbol : getFields()){
            if(symbol.getName().equals(variable)){
                return symbol.getType();
            }
        }

        //check if it is methods param
        for (Symbol symbol : getParameters(methodName)){
            if(symbol.getName().equals(variable)){
                return symbol.getType();
            }
        }

        return new Type("impossible",false);
    }

    public boolean isObject(String methodName, String variable){
        if(getLocalVariables(methodName).isEmpty()){
            return false;
        }
        for (Symbol symbol : getLocalVariables(methodName)){
            if(symbol.getName().equals(variable)){
                return true;
            }
        }
        return false;
    }

    public boolean isArray(String methodName, String variable){
        Type type = getVariableType(methodName,variable);

        return type.isArray();
    }

    public boolean isMathExpression(String kind) {
        return kind.equals("Times") || kind.equals("Plus") || kind.equals("Minus") || kind.equals("Divide");
    }

    public boolean isBooleanExpression(String kind) {
        return kind.equals("Less") || kind.equals("And") || kind.equals("Not");
    }

}