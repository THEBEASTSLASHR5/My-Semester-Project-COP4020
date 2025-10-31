package plc.project;

import plc.project.Ast;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpreter — traverses the AST and evaluates nodes for our custom language.
 * Only this file is modified for the assignment.
 */
public class Interpreter implements Ast.Visitor<Environment.PlcObject> {


    private Scope scope;

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        // Built-in print(x): prints the unwrapped value and returns NIL
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    /* ============================================================
       Helper methods
       ============================================================ */

    private static Environment.PlcObject obj(Object value) {
        return value == null ? Environment.NIL : Environment.create(value);
    }

    private static <T> T requireType(Class<T> type, Environment.PlcObject value) {
        if (type.isInstance(value.getValue())) {
            return type.cast(value.getValue());
        }
        throw new RuntimeException("Expected " + type.getName() +
                " but got " + value.getValue().getClass().getName());
    }

    private Environment.PlcObject visit(Ast.Statement stmt) {
        if (stmt instanceof Ast.Statement.Expression) {
            return visit((Ast.Statement.Expression) stmt);
        } else if (stmt instanceof Ast.Statement.Declaration) {
            return visit((Ast.Statement.Declaration) stmt);
        } else if (stmt instanceof Ast.Statement.Assignment) {
            return visit((Ast.Statement.Assignment) stmt);
        } else if (stmt instanceof Ast.Statement.If) {
            return visit((Ast.Statement.If) stmt);
        } else if (stmt instanceof Ast.Statement.For) {
            return visit((Ast.Statement.For) stmt);
        } else if (stmt instanceof Ast.Statement.While) {
            return visit((Ast.Statement.While) stmt);
        } else if (stmt instanceof Ast.Statement.Return) {
            return visit((Ast.Statement.Return) stmt);
        } else {
            throw new RuntimeException("Unknown statement type: " + stmt.getClass().getName());
        }
    }
    private void execBlock(List<Ast.Statement> statements, Scope blockScope) {
        Scope previous = scope;
        try {
            scope = blockScope;
            for (Ast.Statement stmt : statements) {
                visit(stmt);
            }
        } finally {
            scope = previous;
        }
    }

    private int compareNumbers(Environment.PlcObject a, Environment.PlcObject b) {
        Object av = a.getValue(), bv = b.getValue();
        if (av instanceof BigDecimal || bv instanceof BigDecimal) {
            BigDecimal x = (av instanceof BigDecimal)
                    ? (BigDecimal) av : new BigDecimal((BigInteger) av);
            BigDecimal y = (bv instanceof BigDecimal)
                    ? (BigDecimal) bv : new BigDecimal((BigInteger) bv);
            return x.compareTo(y);
        } else {
            return ((BigInteger) av).compareTo((BigInteger) bv);
        }
    }

    private Environment.PlcObject add(Environment.PlcObject a, Environment.PlcObject b) {
        Object av = a.getValue(), bv = b.getValue();
        if (av instanceof String || bv instanceof String) {
            return obj(String.valueOf(av) + String.valueOf(bv));
        }
        if (av instanceof BigDecimal || bv instanceof BigDecimal) {
            BigDecimal x = (av instanceof BigDecimal)
                    ? (BigDecimal) av : new BigDecimal((BigInteger) av);
            BigDecimal y = (bv instanceof BigDecimal)
                    ? (BigDecimal) bv : new BigDecimal((BigInteger) bv);
            return obj(x.add(y));
        }
        return obj(((BigInteger) av).add((BigInteger) bv));
    }

    private Environment.PlcObject sub(Environment.PlcObject a, Environment.PlcObject b) {
        Object av = a.getValue(), bv = b.getValue();
        if (av instanceof BigDecimal || bv instanceof BigDecimal) {
            BigDecimal x = (av instanceof BigDecimal)
                    ? (BigDecimal) av : new BigDecimal((BigInteger) av);
            BigDecimal y = (bv instanceof BigDecimal)
                    ? (BigDecimal) bv : new BigDecimal((BigInteger) bv);
            return obj(x.subtract(y));
        }
        return obj(((BigInteger) av).subtract((BigInteger) bv));
    }

    private Environment.PlcObject mul(Environment.PlcObject a, Environment.PlcObject b) {
        Object av = a.getValue(), bv = b.getValue();
        if (av instanceof BigDecimal || bv instanceof BigDecimal) {
            BigDecimal x = (av instanceof BigDecimal)
                    ? (BigDecimal) av : new BigDecimal((BigInteger) av);
            BigDecimal y = (bv instanceof BigDecimal)
                    ? (BigDecimal) bv : new BigDecimal((BigInteger) bv);
            return obj(x.multiply(y));
        }
        return obj(((BigInteger) av).multiply((BigInteger) bv));
    }

    private Environment.PlcObject div(Environment.PlcObject a, Environment.PlcObject b) {
        Object av = a.getValue(), bv = b.getValue();
        if (av instanceof BigDecimal || bv instanceof BigDecimal) {
            BigDecimal x = (av instanceof BigDecimal)
                    ? (BigDecimal) av : new BigDecimal((BigInteger) av);
            BigDecimal y = (bv instanceof BigDecimal)
                    ? (BigDecimal) bv : new BigDecimal((BigInteger) bv);

            // Divide with 1 decimal of precision, rounding half up
            BigDecimal result = x.divide(y, 1, java.math.RoundingMode.HALF_UP);
            return obj(result.stripTrailingZeros());
        }
        // Integer division for BigIntegers
        return obj(((BigInteger) av).divide((BigInteger) bv));
    }

    /* ============================================================
       Visiting top-level structures
       ============================================================ */

    public Environment.PlcObject visit(Ast ast) {
        if (ast instanceof Ast.Source) {
            return visit((Ast.Source) ast);
        } else if (ast instanceof Ast.Field) {
            return visit((Ast.Field) ast);
        } else if (ast instanceof Ast.Method) {
            return visit((Ast.Method) ast);
        } else if (ast instanceof Ast.Statement) {
            return visit((Ast.Statement) ast);
        } else if (ast instanceof Ast.Expression) {
            return visit((Ast.Expression) ast);
        } else {
            throw new RuntimeException("Unknown AST node type: " + ast.getClass().getName());
        }
    }

    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field f : ast.getFields()) visit(f);
        for (Ast.Method m : ast.getMethods()) visit(m);
        return scope.lookupFunction("main", 0).invoke(List.of());
    }

    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject value =
                ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;
        scope.defineVariable(ast.getName(), ast.getConstant(), value);
        return Environment.NIL;
    }

    public Environment.PlcObject visit(Ast.Method ast) {
        final String name = ast.getName();
        final int arity = ast.getParameters().size();
        scope.defineFunction(name, arity, args -> {
            Scope saved = this.scope;
            this.scope = new Scope(saved);
            try {
                for (int i = 0; i < arity; i++) {
                    this.scope.defineVariable(ast.getParameters().get(i), false, args.get(i));
                }
                try {
                    for (Ast.Statement s : ast.getStatements()) visit(s);
                } catch (Return r) {
                    return r.value;
                }
                return Environment.NIL;
            } finally {
                this.scope = saved;
            }
        });
        return Environment.NIL;
    }

    /* ============================================================
       Visiting statements
       ============================================================ */

    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject value =
                ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;
        scope.defineVariable(ast.getName(), false, value);
        return Environment.NIL;
    }

    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Invalid assignment target.");
        }
        Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
        Environment.PlcObject value = visit(ast.getValue());
        if (access.getReceiver().isPresent()) {
            Environment.PlcObject recv = visit(access.getReceiver().get());
            recv.setField(access.getName(), value);
        } else {
            Environment.Variable var = scope.lookupVariable(access.getName());
            if (var.getConstant()) {
                throw new RuntimeException("Cannot assign to constant: " + access.getName());
            }
            var.setValue(value);
        }
        return Environment.NIL;
    }

    public Environment.PlcObject visit(Ast.Statement.If ast) {
        boolean cond = requireType(Boolean.class, visit(ast.getCondition()));
        if (cond) {
            execBlock(ast.getThenStatements(), new Scope(scope));
        } else {
            execBlock(ast.getElseStatements(), new Scope(scope));
        }
        return Environment.NIL;
    }

    public Environment.PlcObject visit(Ast.Statement.While ast) {
        Scope loopScope = new Scope(scope);
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            execBlock(ast.getStatements(), loopScope);
        }
        return Environment.NIL;
    }
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        // Run the initialization statement (e.g., let num = 0;)
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }

        // Loop while condition evaluates to true
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            // Execute the loop body inside a new child scope
            execBlock(ast.getStatements(), new Scope(scope));
            // Run the increment statement (e.g., num = num + 1)
            if (ast.getIncrement() != null) {
                visit(ast.getIncrement());
            }
        }

        return Environment.NIL;
    }
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    private Environment.PlcObject visit(Ast.Expression expr) {
        if (expr instanceof Ast.Expression.Literal) {
            return visit((Ast.Expression.Literal) expr);
        } else if (expr instanceof Ast.Expression.Group) {
            return visit((Ast.Expression.Group) expr);
        } else if (expr instanceof Ast.Expression.Binary) {
            return visit((Ast.Expression.Binary) expr);
        } else if (expr instanceof Ast.Expression.Access) {
            return visit((Ast.Expression.Access) expr);
        } else if (expr instanceof Ast.Expression.Function) {
            return visit((Ast.Expression.Function) expr);
        } else {
            throw new RuntimeException("Unknown expression type: " + expr.getClass().getName());
        }
    }

    /* ============================================================
       Visiting expressions
       ============================================================ */

    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        Object v = ast.getLiteral();
        return v == null ? Environment.NIL : Environment.create(v);
    }

    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        Environment.PlcObject left = visit(ast.getLeft());

        // short-circuit boolean ops
        if ("&&".equals(op)) {
            boolean l = requireType(Boolean.class, left);
            if (!l) return obj(false);
            boolean r = requireType(Boolean.class, visit(ast.getRight()));
            return obj(l && r);
        }
        if ("||".equals(op)) {
            boolean l = requireType(Boolean.class, left);
            if (l) return obj(true);
            boolean r = requireType(Boolean.class, visit(ast.getRight()));
            return obj(l || r);
        }

        if ("&&".equals(op) || "AND".equalsIgnoreCase(op)) {
            boolean l = requireType(Boolean.class, left);
            if (!l) return obj(false);
            boolean r = requireType(Boolean.class, visit(ast.getRight()));
            return obj(l && r);
        }
        if ("||".equals(op) || "OR".equalsIgnoreCase(op)) {
            boolean l = requireType(Boolean.class, left);
            if (l) return obj(true);
            boolean r = requireType(Boolean.class, visit(ast.getRight()));
            return obj(l || r);
        }

        Environment.PlcObject right = visit(ast.getRight());

        switch (op) {
            case "+":  return add(left, right);
            case "-":  return sub(left, right);
            case "*":  return mul(left, right);
            case "/":  return div(left, right);
            case "==": return obj(left.getValue().equals(right.getValue()));
            case "!=": return obj(!left.getValue().equals(right.getValue()));
            case "<":  return obj(compareNumbers(left, right) < 0);
            case "<=": return obj(compareNumbers(left, right) <= 0);
            case ">":  return obj(compareNumbers(left, right) > 0);
            case ">=": return obj(compareNumbers(left, right) >= 0);
            default:   throw new RuntimeException("Unknown operator " + op);
        }
    }

    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject recv = visit(ast.getReceiver().get());
            return recv.getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> args = new ArrayList<>();
        for (Ast.Expression e : ast.getArguments()) args.add(visit(e));
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject recv = visit(ast.getReceiver().get());
            return recv.callMethod(ast.getName(), args);
        }
        return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(args);
    }

    /* ============================================================
       Return-signal helper
       ============================================================ */
    private static class Return extends RuntimeException {
        final Environment.PlcObject value;
        Return(Environment.PlcObject value) { this.value = value; }
    }
}