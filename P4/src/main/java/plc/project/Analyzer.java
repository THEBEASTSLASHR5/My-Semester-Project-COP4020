package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Semantic analyzer: decorates/binds names and types, and throws RuntimeException
 * when semantic constraints are violated (as described in the assignment).
 */
public final class Analyzer implements Ast.Visitor<Void> {

    /**
     * Current lexical scope; the analyzer is responsible for pushing/popping as needed.
     */
    public Scope scope;

    /**
     * The method currently being analyzed (used for return type checks).
     */
    private Ast.Method currentMethod = null;

    public Analyzer(Scope parent) {
        this.scope = new Scope(parent);

        // Provide a basic builtin 'print' so tests or examples can call it.
        scope.defineFunction(
                "print",
                "System.out.println",
                Arrays.asList(Environment.Type.ANY),
                Environment.Type.NIL,
                args -> Environment.NIL
        );
    }

    public Scope getScope() {
        return scope;
    }

    /* =========================================================
     * Helpers
     * ========================================================= */

    /**
     * Map a type name string to an Environment.Type via the registry.
     */
    private static Environment.Type resolveType(String name) {
        return Environment.getType(name);
    }

    /**
     * Enforce assignability rules:
     * - same types
     * - target Any accepts anything
     * - target Comparable accepts {Integer, Decimal, Character, String}
     * - otherwise
     */
    public static void requireAssignable(Environment.Type target, Environment.Type actual) {
        if (target.equals(actual)) return;
        if (target.equals(Environment.Type.ANY)) return;

        if (target.equals(Environment.Type.COMPARABLE)) {
            if (actual.equals(Environment.Type.INTEGER) ||
                    actual.equals(Environment.Type.DECIMAL) ||
                    actual.equals(Environment.Type.CHARACTER) ||
                    actual.equals(Environment.Type.STRING)) {
                return;
            }
        }

        throw new RuntimeException("Cannot assign " + actual.getName() + " to " + target.getName() + ".");
    }

    /* =========================================================
     * Program, Fields, Methods
     * ========================================================= */

    @Override
    public Void visit(Ast.Source ast) {
        // 1) Bind fields (variables) first so methods can reference them.
        for (Ast.Field f : ast.getFields()) {
            visit(f);
        }

        // 2) Predeclare all method headers (name/arity/param types/return type).
        for (Ast.Method m : ast.getMethods()) {
            List<Environment.Type> paramTypes = new ArrayList<>();
            for (String tn : m.getParameterTypeNames()) {
                paramTypes.add(resolveType(tn));
            }
            Environment.Type ret =
                    m.getReturnTypeName().isPresent()
                            ? resolveType(m.getReturnTypeName().get())
                            : Environment.Type.ANY;

            // Define the function symbol in the current (top-level) scope.
            scope.defineFunction(
                    m.getName(),
                    m.getName(),
                    paramTypes,
                    ret,
                    args -> Environment.NIL
            );

            // Also store the bound function object on the method node for convenience.
            Environment.Function fn = scope.lookupFunction(m.getName(), paramTypes.size());
            m.setFunction(fn);
        }

        // 3) Analyze method bodies.
        for (Ast.Method m : ast.getMethods()) {
            visit(m);
        }

        // 4) Must have main/0 that returns Integer.
        try {
            Environment.Function main = scope.lookupFunction("main", 0);
            if (!main.getReturnType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Program must define main(): Integer.");
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Program must define main(): Integer.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        Environment.Type declared = resolveType(ast.getTypeName());

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(declared, ast.getValue().get().getType());
        } else if (ast.getConstant()) {
            // Constant fields must have an initializer.
            throw new RuntimeException("Constant field '" + ast.getName() + "' must be initialized.");
        }

        Environment.Variable var = new Environment.Variable(
                ast.getName(), ast.getName(), declared, ast.getConstant(), Environment.NIL);
        scope.defineVariable(var.getName(), var.getJvmName(), var.getType(), var.getConstant(), var.getValue());
        ast.setVariable(var);
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        // Function header already defined in visit(Source); retrieve it.
        Environment.Function fn = scope.lookupFunction(ast.getName(), ast.getParameters().size());
        if (fn == null) throw new RuntimeException("Method header not defined for " + ast.getName() + ".");
        ast.setFunction(fn);

        // New scope for parameters and body.
        Scope saved = scope;
        scope = new Scope(scope);
        currentMethod = ast;

        // Declare parameters as variables with their bound types.
        for (int i = 0; i < ast.getParameters().size(); i++) {
            String paramName = ast.getParameters().get(i);
            Environment.Type paramType = fn.getParameterTypes().get(i);
            scope.defineVariable(paramName, paramName, paramType, false, Environment.NIL);
        }

        // Analyze body.
        for (Ast.Statement s : ast.getStatements()) {
            visit(s);
        }

        // Restore.
        currentMethod = null;
        scope = saved;
        return null;
    }

    /* =========================================================
     * Statements
     * ========================================================= */

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type;

        if (ast.getTypeName().isPresent()) {
            type = resolveType(ast.getTypeName().get());
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(type, ast.getValue().get().getType());
            }
        } else {
            // No explicit type — must have initializer to infer.
            if (!ast.getValue().isPresent()) {
                throw new RuntimeException("Declaration of '" + ast.getName() + "' requires a type or initializer.");
            }
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        }

        Environment.Variable var =
                new Environment.Variable(ast.getName(), ast.getName(), type, false, Environment.NIL);
        scope.defineVariable(var.getName(), var.getJvmName(), var.getType(), var.getConstant(), var.getValue());
        ast.setVariable(var);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Assignment receiver must be an access.");
        }
        Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
        visit(access); // binds the variable

        if (access.getVariable().getConstant()) {
            throw new RuntimeException("Cannot assign to constant '" + access.getName() + "'.");
        }

        visit(ast.getValue());
        requireAssignable(access.getVariable().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("If statement must have at least one 'then' statement.");
        }

        // then
        Scope saved = scope;
        scope = new Scope(scope);
        for (Ast.Statement s : ast.getThenStatements()) visit(s);
        scope = saved;

        // else
        scope = new Scope(scope);
        for (Ast.Statement s : ast.getElseStatements()) visit(s);
        scope = saved;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        // C-style for; any of its parts may be null (per equals implementation).
        if (ast.getInitialization() != null) visit(ast.getInitialization());
        if (ast.getCondition() != null) {
            visit(ast.getCondition());
            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        }
        if (ast.getIncrement() != null) visit(ast.getIncrement());

        Scope saved = scope;
        scope = new Scope(scope);
        for (Ast.Statement s : ast.getStatements()) visit(s);
        scope = saved;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        Scope saved = scope;
        scope = new Scope(scope);
        for (Ast.Statement s : ast.getStatements()) visit(s);
        scope = saved;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        if (currentMethod == null) {
            throw new RuntimeException("Return outside of a method.");
        }
        visit(ast.getValue());
        Environment.Function fn = scope.lookupFunction(currentMethod.getName(), currentMethod.getParameters().size());
        requireAssignable(fn.getReturnType(), ast.getValue().getType());
        return null;
    }

    /* =========================================================
     * Expressions
     * ========================================================= */

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object lit = ast.getLiteral();
        if (lit == null) {
            ast.setType(Environment.Type.NIL);
        } else if (lit instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (lit instanceof BigInteger) {
            ast.setType(Environment.Type.INTEGER);
            // Check integer range fits within 32-bit signed int
            BigInteger val = (BigInteger) lit;
            if (val.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                    val.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Integer literal out of range: " + val);
            }
        } else if (lit instanceof BigDecimal) {
            ast.setType(Environment.Type.DECIMAL);
        } else if (lit instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (lit instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else {
            throw new RuntimeException("Unknown literal type.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        String op = ast.getOperator();
        Environment.Type lt = ast.getLeft().getType();
        Environment.Type rt = ast.getRight().getType();

        switch (op) {
            case "+":
                // Numeric addition OR string concatenation
                if (lt.equals(Environment.Type.STRING) || rt.equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING); // allow concatenation with any type
                } else if (lt.equals(Environment.Type.INTEGER) && rt.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (lt.equals(Environment.Type.DECIMAL) && rt.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Invalid '+' operation between " +
                            lt.getName() + " and " + rt.getName() + ".");
                }
                break;

            case "-":
            case "*":
                // numeric ops; both Integer or both Decimal (no mixing)
                if (lt.equals(Environment.Type.INTEGER) && rt.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (lt.equals(Environment.Type.DECIMAL) && rt.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Invalid numeric operation between " +
                            lt.getName() + " and " + rt.getName() + ".");
                }
                break;
            case "/":
                // division: Integer/Integer -> Decimal or keep Decimal/Decimal -> Decimal
                if (lt.equals(Environment.Type.INTEGER) && rt.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else if (lt.equals(Environment.Type.DECIMAL) && rt.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Invalid division between " +
                            lt.getName() + " and " + rt.getName() + ".");
                }
                break;
            case "==":
            case "!=":
                // Equality requires same type (simple rule).
                if (!lt.equals(rt)) {
                    throw new RuntimeException("Equality requires same operand types.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<":
            case "<=":
            case ">":
            case ">=":
                // Relational over numbers only, same numeric type.
                if ((lt.equals(Environment.Type.INTEGER) && rt.equals(Environment.Type.INTEGER)) ||
                        (lt.equals(Environment.Type.DECIMAL) && rt.equals(Environment.Type.DECIMAL))) {
                    ast.setType(Environment.Type.BOOLEAN);
                } else {
                    throw new RuntimeException("Relational operators require numeric operands of the same type.");
                }
                break;
            case "&&":
            case "||":
                if (lt.equals(Environment.Type.BOOLEAN) && rt.equals(Environment.Type.BOOLEAN)) {
                    ast.setType(Environment.Type.BOOLEAN);
                } else {
                    throw new RuntimeException("Boolean operators require boolean operands.");
                }
                break;
            default:
                throw new RuntimeException("Unknown operator: " + op);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            // receiver.field
            Ast.Expression recv = ast.getReceiver().get();
            visit(recv);
            Environment.Variable field = recv.getType().getField(ast.getName());
            if (field == null) throw new RuntimeException("Unknown field '" + ast.getName() + "'.");
            ast.setVariable(field);
        } else {
            // simple variable
            Environment.Variable v = scope.lookupVariable(ast.getName());
            if (v == null) throw new RuntimeException("Unknown variable '" + ast.getName() + "'.");
            ast.setVariable(v);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function fn;
        if (ast.getReceiver().isPresent()) {
            Ast.Expression recv = ast.getReceiver().get();
            visit(recv);
            fn = recv.getType().getFunction(ast.getName(), ast.getArguments().size());
        } else {
            fn = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        }
        if (fn == null) {
            throw new RuntimeException("Unknown function '" + ast.getName() + "'.");
        }

        // Analyze each argument and check against param types
        for (int i = 0; i < ast.getArguments().size(); i++) {
            Ast.Expression arg = ast.getArguments().get(i);
            visit(arg);
            requireAssignable(fn.getParameterTypes().get(i), arg.getType());
        }

        ast.setFunction(fn);
        return null;
    }
}