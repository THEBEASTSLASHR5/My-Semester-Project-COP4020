package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // class Main {
        print("public class Main {");
        indent++;

        // fields
        for (Ast.Field field : ast.getFields()) {
            newline(indent);
            visit(field);
        }

        if (!ast.getFields().isEmpty()) {
            newline(0); // empty line
        }

        // main() wrapper
        newline(indent);
        print("public static void main(String[] args) {");
        newline(indent + 1);
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");

        // methods
        for (Ast.Method method : ast.getMethods()) {
            newline(0);
            newline(indent);
            visit(method);
        }

        indent--;
        newline(0);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        // Field type is not stored on Field AST; type is inside the Environment
        // The Environment is already resolved before codegen.

        print(ast.getVariable().getType().getJvmName(), " ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getName(), "(");

        // Parameter types also stored in the Function environment
        for (int i = 0; i < ast.getParameters().size(); i++) {
            if (i > 0) print(", ");
            print(
                    ast.getFunction().getParameterTypes().get(i).getJvmName(),
                    " ",
                    ast.getParameters().get(i)
            );
        }

        print(") {");

        indent++;
        for (Ast.Statement stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }
        indent--;

        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");

        indent++;
        for (Ast.Statement stmt : ast.getThenStatements()) {
            newline(indent);
            visit(stmt);
        }
        indent--;

        if (!ast.getElseStatements().isEmpty()) {
            newline(indent);
            print("} else {");
            indent++;
            for (Ast.Statement stmt : ast.getElseStatements()) {
                newline(indent);
                visit(stmt);
            }
            indent--;
        }

        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");

        indent++;
        for (Ast.Statement stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }
        indent--;

        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();

        if (literal == null) {
            print("null");
        } else if (literal instanceof String) {
            print("\"", literal.toString().replace("\"", "\\\""), "\"");
        } else if (literal instanceof Character) {
            print("'", literal.toString().replace("'", "\\'"), "'");
        } else {
            print(literal.toString());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        print(" ", ast.getOperator(), " ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        // special case: print()
        if (!ast.getReceiver().isPresent() && ast.getName().equals("print")) {
            print("System.out.println(");
            if (!ast.getArguments().isEmpty()) {
                visit(ast.getArguments().get(0));
            }
            print(")");
            return null;
        }

        // normal function call
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }

        print(ast.getName(), "(");

        for (int i = 0; i < ast.getArguments().size(); i++) {
            if (i > 0) print(", ");
            visit(ast.getArguments().get(i));
        }

        print(")");
        return null;
    }
}
