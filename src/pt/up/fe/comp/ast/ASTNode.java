package pt.up.fe.comp.ast;

import pt.up.fe.specs.util.SpecsStrings;

public enum ASTNode {

    START,
    IMPORT_DECLARATION,
    VAR_DECLARATION,
    CLASS_DECLARATION,
    METHOD_DECLARATION,
    IDENTIFIER,
    NUMBER,
    TRUE,
    FALSE,
    NEW_DECLARATION,
    EXPRESSION_STATEMENT,
    EXPRESSION,
    AND,
    LESS,
    PLUS,
    MINUS,
    TIMES,
    DIVIDE,
    NOT,
    DOT_ACCESS,
    VAR_ACCESS,
    ARGUMENTS,
    IF_STATEMENT,
    ELSE_SCOPE,
    WHILE_STATEMENT,
    ASSIGNMENT,
    RETURN,
    ARRAY_ACCESS;



    private final String name;

    private ASTNode(){
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    @Override
    public String toString(){
        return name;
    }
    
}
