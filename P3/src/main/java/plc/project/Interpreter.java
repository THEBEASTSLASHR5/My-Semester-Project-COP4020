package plc.project;

import java.math.BigInteger;

/**
 * Interpreter class — walks an abstract syntax tree (AST) and
 * evaluates code step-by-step. Implements the Visitor pattern for AST nodes.
 */
public class Interpreter implements Ast.Visitor {

    /** The current variable/function scope for this interpreter instance. */
    private Scope SeanParrell1;

    /**
     * Constructor initializes the global scope and defines a built-in "print" function.
     * @param SeanParrell2 Parent scope (can be null for the global root).
     */
    public Interpreter(Scope SeanParrell2) {
        // Create a new scope chained to the given parent.
        SeanParrell1 = new Scope(SeanParrell2);

        // Define a built-in function named "print" that takes 1 argument and prints it.
        SeanParrell1.defineFunction("print", 1, SeanParrell3 -> {
            // Print the first argument's value to the console.
            System.out.println(SeanParrell3.get(0).getValue());
            // Return NIL (nothing) as the function result.
            return Environment.NIL;
        });
    }

    /** Returns the current active scope for use in other parts of the program. */
    public Scope getScope() {
        return SeanParrell1;
    }

    /* ==========================================================
       VISITOR IMPLEMENTATION FOR AST NODES
       ========================================================== */

    /**
     * Visit a source node (the root of the program).
     * Currently, this only initializes global variables (fields),
     * but does not execute any functions yet.
     */
    @Override
    public Environment.PlcObject visit(Ast.Source SeanParrell4) {
        // Loop through each declared global field and evaluate it.
        for (Ast.Field SeanParrell5 : SeanParrell4.getFields()) {
            visit(SeanParrell5);
        }
        // Return NIL since we’re not running main() yet.
        return Environment.NIL;
    }

    /**
     * Visit a field declaration (global variable).
     * Example: var x = 10;
     */
    @Override
    public Environment.PlcObject visit(Ast.Field SeanParrell6) {
        // Evaluate the initializer value if it exists, otherwise default to NIL.
        Environment.PlcObject SeanParrell7 = SeanParrell6.getValue().isPresent()
                ? visit(SeanParrell6.getValue().get())
                : Environment.NIL;

        // Define the variable in the current scope with the computed value.
        SeanParrell1.define(SeanParrell6.getName(), SeanParrell7);

        // Return NIL — defining a variable doesn’t produce a runtime value.
        return Environment.NIL;
    }

    /* ==========================================================
       SIMPLE EXPRESSION TYPES
       ========================================================== */

    /**
     * Visit a literal expression.
     * Example: numbers (10), strings ("hello"), booleans (true), etc.
     */
    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal SeanParrell8) {
        // Retrieve the literal’s raw Java value.
        Object SeanParrell9 = SeanParrell8.getLiteral();

        // If it’s null (e.g., "nil"), return NIL.
        if (SeanParrell9 == null) return Environment.NIL;

        // Otherwise, wrap the literal value in a PlcObject and return it.
        return Environment.create(SeanParrell9);
    }

    /**
     * Visit a grouped expression.
     * Example: (x + 1) — just evaluate the expression inside the parentheses.
     */
    @Override
    public Environment.PlcObject visit(Ast.Expr.Group SeanParrell10) {
        return visit(SeanParrell10.getExpression());
    }

    /**
     * Visit a variable access expression.
     * Example: reading the value of a variable (like “x”).
     */
    @Override
    public Environment.PlcObject visit(Ast.Expr.Access SeanParrell11) {
        // Look up the variable name from the current scope and return its value.
        return SeanParrell1.lookup(SeanParrell11.getName()).get();
    }

    /* ==========================================================
       STUB METHODS — these are placeholders so code compiles
       ========================================================== */

    // Each of these will be implemented later for full language features.
    @Override public Environment.PlcObject visit(Ast.Method SeanParrell12){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.Expression SeanParrell13){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.Declaration SeanParrell14){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.Assignment SeanParrell15){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.If SeanParrell16){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.For SeanParrell17){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.While SeanParrell18){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.Return SeanParrell19){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Expr.Binary SeanParrell20){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Expr.Function SeanParrell21){ return Environment.NIL; }

    /* ==========================================================
       UTILITY METHODS
       ========================================================== */

    /**
     * Ensures an object’s underlying Java value matches the expected type.
     * @param SeanParrell22 The expected class type.
     * @param SeanParrell23 The object to check.
     * @return The underlying value cast to the expected type.
     * @throws RuntimeException if the object type does not match.
     */
    private static <T> T requireType(Class<T> SeanParrell22, Environment.PlcObject SeanParrell23) {
        if (SeanParrell22.isInstance(SeanParrell23.getValue()))
            return SeanParrell22.cast(SeanParrell23.getValue());
        throw new RuntimeException("Expected type " + SeanParrell22.getName() +
                ", got " + SeanParrell23.getValue().getClass().getName());
    }

    /**
     * Custom runtime exception used to represent a "return" control flow.
     * When thrown inside a function, it carries the return value upward.
     */
    private static class Return extends RuntimeException {
        final Environment.PlcObject SeanParrell24;

        /**
         * Creates a Return exception carrying a return value.
         * @param SeanParrell25 The value being returned.
         */
        Return(Environment.PlcObject SeanParrell25){
            SeanParrell24 = SeanParrell25;
        }
    }
}
