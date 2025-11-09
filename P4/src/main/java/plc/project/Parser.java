package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Complete recursive-descent parser for Project 4 (typed variables & methods).
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /* =========================================================
     *                     TOP-LEVEL RULES
     * ========================================================= */

    /** source -> (field | method)* EOF */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while (tokens.has(0)) {
            if (!tokens.has(1)) break; // stop before end-of-list
            if (!peek("LET") && !peek("DEF")) break;

            if (peek("LET")) fields.add(parseField());
            else if (peek("DEF")) methods.add(parseMethod());
            else throw error("Expected 'LET' or 'DEF' at top level.");
        }

        return new Ast.Source(fields, methods);
    }

    /** field -> LET CONST? identifier ':' identifier ('=' expression)? ';' */
    public Ast.Field parseField() throws ParseException {
        expect("LET", "Expected 'LET'.");
        boolean constant = match("CONST");

        String name = parseIdentifier("Expected field name.");
        expect(":", "Expected ':' after field name.");
        String typeName = parseIdentifier("Expected type name after ':'.");

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        expect(";", "Expected ';' after field declaration.");
        // NOTE: typeName is String, not Optional<String>
        return new Ast.Field(name, typeName, constant, value);

    }


    /** method -> DEF identifier '(' parameters? ')' (':' identifier)? DO statement* END */
    public Ast.Method parseMethod() throws ParseException {
        expect("DEF", "Expected 'DEF'.");
        String name = parseIdentifier("Expected method name.");

        expect("(", "Expected '(' after method name.");
        List<String> params = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();
        if (!peek(")")) {
            do {
                String pName = parseIdentifier("Expected parameter name.");
                expect(":", "Expected ':' after parameter name.");
                String pType = parseIdentifier("Expected parameter type.");
                params.add(pName);
                paramTypes.add(pType);
            } while (match(","));
        }
        expect(")", "Expected ')' after parameter list.");

        Optional<String> returnType = Optional.empty();
        if (match(":")) returnType = Optional.of(parseIdentifier("Expected return type."));

        expect("DO", "Expected 'DO' before method body.");
        List<Ast.Statement> body = new ArrayList<>();
        while (!peek("END")) body.add(parseStatement());
        expect("END", "Expected 'END' after method body.");

        return new Ast.Method(name, params, paramTypes, returnType, body);
    }

    /* =========================================================
     *                       STATEMENTS
     * ========================================================= */

    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) return parseDeclarationStatement();
        if (peek("IF")) return parseIfStatement();
        if (peek("FOR")) return parseForStatement();
        if (peek("WHILE")) return parseWhileStatement();
        if (peek("RETURN")) return parseReturnStatement();

        // expression ('=' expression)? ';'
        Ast.Expression left = parseExpression();
        if (match("=")) {
            Ast.Expression right = parseExpression();
            expect(";", "Expected ';' after assignment.");
            return new Ast.Statement.Assignment(left, right);
        } else {
            expect(";", "Expected ';' after expression.");
            return new Ast.Statement.Expression(left);
        }
    }

    /** LET identifier (':' identifier)? ('=' expression)? ';' */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        expect("LET", "Expected 'LET'.");
        String name = parseIdentifier("Expected variable name.");

        Optional<String> type = Optional.empty();
        if (match(":")) type = Optional.of(parseIdentifier("Expected type."));

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) value = Optional.of(parseExpression());

        expect(";", "Expected ';' after declaration.");
        return new Ast.Statement.Declaration(name, type, value);
    }

    /** IF expression DO statement* (ELSE DO statement* END)? END */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        expect("IF", "Expected 'IF'.");
        Ast.Expression condition = parseExpression();

        expect("DO", "Expected 'DO' after condition.");
        List<Ast.Statement> thenStmts = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) thenStmts.add(parseStatement());

        List<Ast.Statement> elseStmts = new ArrayList<>();
        if (match("ELSE")) {
            expect("DO", "Expected 'DO' after ELSE.");
            while (!peek("END")) elseStmts.add(parseStatement());
        }

        expect("END", "Expected 'END' after IF.");
        return new Ast.Statement.If(condition, thenStmts, elseStmts);
    }

    /** FOR identifier IN expression DO statement* END */
    public Ast.Statement.For parseForStatement() throws ParseException {
        expect("FOR", "Expected 'FOR'.");
        expect("(", "Expected '(' after FOR.");

        // initializer
        Ast.Statement init = parseStatement();

        // condition
        Ast.Expression condition = parseExpression();
        expect(";", "Expected ';' after condition.");

        // update
        Ast.Statement update = parseStatement();
        expect(")", "Expected ')' after update.");

        expect("DO", "Expected 'DO'.");

        List<Ast.Statement> body = new ArrayList<>();
        while (!peek("END")) {
            body.add(parseStatement());
        }
        expect("END", "Expected 'END' after FOR.");

        return new Ast.Statement.For(init, condition, update, body);
    }


    /** WHILE expression DO statement* END */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        expect("WHILE", "Expected 'WHILE'.");
        Ast.Expression cond = parseExpression();

        expect("DO", "Expected 'DO'.");
        List<Ast.Statement> body = new ArrayList<>();
        while (!peek("END")) body.add(parseStatement());
        expect("END", "Expected 'END' after WHILE.");

        return new Ast.Statement.While(cond, body);
    }

    /** RETURN expression ';' */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        expect("RETURN", "Expected 'RETURN'.");
        Ast.Expression value = parseExpression();
        expect(";", "Expected ';'.");
        return new Ast.Statement.Return(value);
    }

    /* =========================================================
     *                      EXPRESSIONS
     * ========================================================= */

    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalOr();
    }

    private Ast.Expression parseLogicalOr() throws ParseException {
        Ast.Expression expr = parseLogicalAnd();
        while (match("OR"))
            expr = new Ast.Expression.Binary("OR", expr, parseLogicalAnd());
        return expr;
    }

    private Ast.Expression parseLogicalAnd() throws ParseException {
        Ast.Expression expr = parseComparison();
        while (match("AND"))
            expr = new Ast.Expression.Binary("AND", expr, parseComparison());
        return expr;
    }

    private Ast.Expression parseComparison() throws ParseException {
        Ast.Expression expr = parseAdditive();
        while (true) {
            if (match("<")) expr = new Ast.Expression.Binary("<", expr, parseAdditive());
            else if (match(">")) expr = new Ast.Expression.Binary(">", expr, parseAdditive());
            else if (match("==")) expr = new Ast.Expression.Binary("==", expr, parseAdditive());
            else if (match("!=")) expr = new Ast.Expression.Binary("!=", expr, parseAdditive());
            else break;
        }
        return expr;
    }

    private Ast.Expression parseAdditive() throws ParseException {
        Ast.Expression expr = parseMultiplicative();
        while (true) {
            if (match("+")) expr = new Ast.Expression.Binary("+", expr, parseMultiplicative());
            else if (match("-")) expr = new Ast.Expression.Binary("-", expr, parseMultiplicative());
            else break;
        }
        return expr;
    }

    private Ast.Expression parseMultiplicative() throws ParseException {
        Ast.Expression expr = parseSecondary();
        while (true) {
            if (match("*")) expr = new Ast.Expression.Binary("*", expr, parseSecondary());
            else if (match("/")) expr = new Ast.Expression.Binary("/", expr, parseSecondary());
            else break;
        }
        return expr;
    }

    /** secondary -> primary ('.' identifier ('(' args? ')')? )* */
    private Ast.Expression parseSecondary() throws ParseException {
        Ast.Expression expr = parsePrimary();
        while (match(".")) {
            String name = parseIdentifier("Expected member name.");
            if (match("(")) {
                List<Ast.Expression> args = new ArrayList<>();
                if (!peek(")")) {
                    do { args.add(parseExpression()); } while (match(","));
                }
                expect(")", "Expected ')'.");
                expr = new Ast.Expression.Function(Optional.of(expr), name, args);
            } else {
                expr = new Ast.Expression.Access(Optional.of(expr), name);
            }
        }
        return expr;
    }

    /** primary -> literal | identifier | '(' expression ')' | TRUE | FALSE | NIL */
    private Ast.Expression parsePrimary() throws ParseException {
        if (match("(")) {
            Ast.Expression e = parseExpression();
            expect(")", "Expected ')'.");
            return new Ast.Expression.Group(e);
        }

        if (match("TRUE"))  return new Ast.Expression.Literal(true);
        if (match("FALSE")) return new Ast.Expression.Literal(false);
        if (match("NIL"))   return new Ast.Expression.Literal(null);

        if (match(Token.Type.INTEGER))
            return new Ast.Expression.Literal(new BigInteger(previous().getLiteral()));
        if (match(Token.Type.DECIMAL))
            return new Ast.Expression.Literal(new BigDecimal(previous().getLiteral()));
        if (match(Token.Type.CHARACTER))
            return new Ast.Expression.Literal(previous().getLiteral().charAt(0));
        if (match(Token.Type.STRING))
            return new Ast.Expression.Literal(previous().getLiteral());

        if (match(Token.Type.IDENTIFIER)) {
            String name = previous().getLiteral();
            if (match("(")) {
                List<Ast.Expression> args = new ArrayList<>();
                if (!peek(")")) {
                    do { args.add(parseExpression()); } while (match(","));
                }
                expect(")", "Expected ')'.");
                return new Ast.Expression.Function(Optional.empty(), name, args);
            } else {
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        }

        throw error("Expected expression.");
    }

    /* =========================================================
     *                     TOKEN HELPERS
     * ========================================================= */

    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) return false;
            Object pat = patterns[i];
            Token look = tokens.get(i);

            if (pat instanceof Token.Type) {
                if (look.getType() != pat) return false;
            } else if (pat instanceof String) {
                if (!((String) pat).equals(look.getLiteral())) return false;
            } else throw new IllegalArgumentException();
        }
        return true;
    }

    private boolean match(Object... patterns) {
        if (peek(patterns)) {
            for (int i = 0; i < patterns.length; i++) tokens.advance();
            return true;
        }
        return false;
    }

    private void expect(Object pattern, String msg) throws ParseException {
        if (!match(pattern)) throw error(msg);
    }

    private String parseIdentifier(String msg) throws ParseException {
        if (match(Token.Type.IDENTIFIER)) return previous().getLiteral();
        throw error(msg);
    }

    private Token previous() { return tokens.get(-1); }

    private ParseException error(String msg) {
        int position = tokens.has(0) ? tokens.get(0).getIndex() : -1;
        return new ParseException(msg, position);
    }


    /* =========================================================
     *                     TOKEN STREAM
     * ========================================================= */

    private static final class TokenStream {
        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) { this.tokens = tokens; }

        boolean has(int offset) {
            int i = index + offset;
            return i >= 0 && i < tokens.size();
        }

        Token get(int offset) { return tokens.get(index + offset); }

        void advance() { index++; }
    }
}
