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

    @Override
    public Environment.PlcObject visit(Ast.Source SeanParrell4) {
        for (Ast.Field SeanParrell5 : SeanParrell4.getFields()) {
            visit(SeanParrell5);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Field SeanParrell6) {
        Environment.PlcObject SeanParrell7 = SeanParrell6.getValue().isPresent()
                ? visit(SeanParrell6.getValue().get())
                : Environment.NIL;
        SeanParrell1.define(SeanParrell6.getName(), SeanParrell7);
        return Environment.NIL;
    }

    /* ==========================================================
       SIMPLE EXPRESSION TYPES
       ========================================================== */

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal SeanParrell8) {
        Object SeanParrell9 = SeanParrell8.getLiteral();
        if (SeanParrell9 == null) return Environment.NIL;
        return Environment.create(SeanParrell9);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group SeanParrell10) {
        return visit(SeanParrell10.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access SeanParrell11) {
        return SeanParrell1.lookup(SeanParrell11.getName()).get();
    }

    /* ==========================================================
       NEWLY IMPLEMENTED SECTIONS BELOW
       ========================================================== */

    /**
     * Variable declaration (ex: let x = 5; or let y;)
     * Added this so code can now handle declaring local variables.
     */
    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration SeanParrell14) {
        Environment.PlcObject value = Environment.NIL;
        if (SeanParrell14.getValue().isPresent()) {
            value = visit(SeanParrell14.getValue().get());
        }
        SeanParrell1.define(SeanParrell14.getName(), value);
        return Environment.NIL;
    }

    /**
     * Assignment statement (ex: x = 10)
     * Added this so code can now modify existing variable values.
     */
    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment SeanParrell15) {
        Environment.Variable var = SeanParrell1.lookup(((Ast.Expr.Access)SeanParrell15.getReceiver()).getName());
        Environment.PlcObject value = visit(SeanParrell15.getValue());
        var.set(value);
        return Environment.NIL;
    }

    /**
     * Basic binary operations (ex: +, -, *, /, ==, <, >)
     * Added this so arithmetic and logical expressions now actually compute results.
     */
    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary SeanParrell20) {
        Environment.PlcObject left = visit(SeanParrell20.getLeft());
        Environment.PlcObject right = visit(SeanParrell20.getRight());
        String op = SeanParrell20.getOperator();

        // Just a few main cases to make math and comparison work
        switch (op) {
            case "+":
                if (left.getValue() instanceof String || right.getValue() instanceof String)
                    return Environment.create(left.getValue().toString() + right.getValue().toString());
                else
                    return Environment.create(requireType(BigInteger.class, left).add(requireType(BigInteger.class, right)));
            case "-":
                return Environment.create(requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, right)));
            case "*":
                return Environment.create(requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, right)));
            case "/":
                BigInteger divisor = requireType(BigInteger.class, right);
                if (divisor.equals(BigInteger.ZERO)) throw new RuntimeException("Division by zero");
                return Environment.create(requireType(BigInteger.class, left).divide(divisor));
            case "==":
                return Environment.create(left.getValue().equals(right.getValue()));
            case "!=":
                return Environment.create(!left.getValue().equals(right.getValue()));
            case "<":
                return Environment.create(requireType(BigInteger.class, left).compareTo(requireType(BigInteger.class, right)) < 0);
            case ">":
                return Environment.create(requireType(BigInteger.class, left).compareTo(requireType(BigInteger.class, right)) > 0);
            default:
                throw new RuntimeException("Unknown operator: " + op);
        }
    }

    /**
     * Simple if-statement handling (added basic branching support).
     */
    @Override
    public Environment.PlcObject visit(Ast.Stmt.If SeanParrell16) {
        Environment.PlcObject condition = visit(SeanParrell16.getCondition());
        if (requireType(Boolean.class, condition)) {
            SeanParrell1 = new Scope(SeanParrell1); // push new scope
            for (Ast.Stmt stmt : SeanParrell16.getThenStatements()) {
                visit(stmt);
            }
            SeanParrell1 = SeanParrell1.getParent(); // pop scope
        } else {
            SeanParrell1 = new Scope(SeanParrell1);
            for (Ast.Stmt stmt : SeanParrell16.getElseStatements()) {
                visit(stmt);
            }
            SeanParrell1 = SeanParrell1.getParent();
        }
        return Environment.NIL;
    }

    /**
     * While loop (added basic looping logic).
     */
    @Override
    public Environment.PlcObject visit(Ast.Stmt.While SeanParrell18) {
        while (requireType(Boolean.class, visit(SeanParrell18.getCondition()))) {
            SeanParrell1 = new Scope(SeanParrell1);
            for (Ast.Stmt stmt : SeanParrell18.getStatements()) {
                visit(stmt);
            }
            SeanParrell1 = SeanParrell1.getParent();
        }
        return Environment.NIL;
    }

    // Other stubbed methods still here for completeness
    @Override public Environment.PlcObject visit(Ast.Method SeanParrell12){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.Expression SeanParrell13){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.For SeanParrell17){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Stmt.Return SeanParrell19){ return Environment.NIL; }
    @Override public Environment.PlcObject visit(Ast.Expr.Function SeanParrell21){ return Environment.NIL; }

    /* ==========================================================
       UTILITY METHODS
       ========================================================== */

    private static <T> T requireType(Class<T> SeanParrell22, Environment.PlcObject SeanParrell23) {
        if (SeanParrell22.isInstance(SeanParrell23.getValue()))
            return SeanParrell22.cast(SeanParrell23.getValue());
        throw new RuntimeException("Expected type " + SeanParrell22.getName() +
                ", got " + SeanParrell23.getValue().getClass().getName());
    }

    private static class Return extends RuntimeException {
        final Environment.PlcObject SeanParrell24;
        Return(Environment.PlcObject SeanParrell25){
            SeanParrell24 = SeanParrell25;
        }
    }
}