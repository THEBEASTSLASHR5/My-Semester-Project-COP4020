package plc.project;

import java.util.Optional;
import java.util.List;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * This parser uses the recursive descent style. Each grammar rule is implemented
 * as a separate function. If a rule refers to another rule, that simply means
 * we call the other function.
 */
public final class Parser {

    // Stream of tokens we will consume during parsing
    private final TokenStream tokens;

    public Parser(List<Token> SeanParrell1) {
        // Wrap the raw list of tokens in our TokenStream helper
        this.tokens = new TokenStream(SeanParrell1);
    }

    // ----------------------------------------------------------------------
    // Source / Field / Method (left unimplemented for Part 2B)
    // ----------------------------------------------------------------------

    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new java.util.ArrayList<>();
        List<Ast.Method> methods = new java.util.ArrayList<>();

        while (peek("LET") || peek("DEF")) {
            if (peek("LET")) {
                fields.add(parseField());
            } else if (peek("DEF")) {
                methods.add(parseMethod());
            } else {
                throw new ParseException("Expected field or method definition", tokens.get(0).getIndex());
            }
        }

        return new Ast.Source(fields, methods);
    }

    public Ast.Field parseField() throws ParseException {
        match("LET");
        String name = require(Token.Type.IDENTIFIER).getLiteral();
        require(":");
        String typeName = require(Token.Type.IDENTIFIER).getLiteral();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
        }

        return new Ast.Field(name, typeName, false, value);
    }

    public Ast.Method parseMethod() throws ParseException {
        match("DEF");
        String name = require(Token.Type.IDENTIFIER).getLiteral();
        require("(");

        List<String> parameters = new java.util.ArrayList<>();
        List<String> parameterTypes = new java.util.ArrayList<>();

        if (!peek(")")) {
            do {
                String paramName = require(Token.Type.IDENTIFIER).getLiteral();
                require(":");
                String typeName = require(Token.Type.IDENTIFIER).getLiteral();
                parameters.add(paramName);
                parameterTypes.add(typeName);
            } while (match(","));
        }

        require(")");

        Optional<String> returnType = Optional.empty();
        if (match(":")) {
            returnType = Optional.of(require(Token.Type.IDENTIFIER).getLiteral());
        }

        require("DO");
        List<Ast.Statement> statements = new java.util.ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        require("END");

        return new Ast.Method(name, parameters, parameterTypes, returnType, statements);
    }

    // ----------------------------------------------------------------------
    // Statements
    // ----------------------------------------------------------------------

    public Ast.Statement parseStatement() throws ParseException {
        // Declaration statement
        if (peek("LET")) {
            Ast.Statement.Declaration decl = parseDeclarationStatement();
            require(";");
            return decl;
        }

        // IF statement
        else if (peek("IF")) {
            return parseIfStatement();
        }

        // WHILE statement
        else if (peek("WHILE")) {
            return parseWhileStatement();
        }

        // RETURN statement
        else if (peek("RETURN")) {
            Ast.Statement.Return ret = parseReturnStatement();
            require(";");
            return ret;
        }

        // FOR statement (if applicable)
        else if (peek("FOR")) {
            return parseForStatement();
        }

        // Default: expression or assignment statement
        else {
            Ast.Expression expr = parseExpression();
            if (match("=")) {
                Ast.Expression value = parseExpression();
                require(";");
                if (!(expr instanceof Ast.Expression.Access)) {
                    throw new ParseException("Invalid assignment target", tokens.get(0).getIndex());
                }
                return new Ast.Statement.Assignment((Ast.Expression.Access) expr, value);
            } else {
                require(";");
                return new Ast.Statement.Expression(expr);
            }
        }
    }

    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        String name = require(Token.Type.IDENTIFIER).getLiteral();

        Optional<String> typeName = Optional.empty();
        Optional<Ast.Expression> value = Optional.empty();

        if (match(":")) {
            typeName = Optional.of(require(Token.Type.IDENTIFIER).getLiteral());
        }

        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        return new Ast.Statement.Declaration(name, typeName, value);
    }

    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expression condition = parseExpression();
        require("DO");

        List<Ast.Statement> thenStmts = new java.util.ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            thenStmts.add(parseStatement());
        }

        List<Ast.Statement> elseStmts = new java.util.ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStmts.add(parseStatement());
            }
        }

        require("END");
        return new Ast.Statement.If(condition, thenStmts, elseStmts);
    }

    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expression condition = parseExpression();
        require("DO");

        List<Ast.Statement> stmts = new java.util.ArrayList<>();
        while (!peek("END")) {
            stmts.add(parseStatement());
        }

        require("END");
        return new Ast.Statement.While(condition, stmts);
    }

    public Ast.Statement.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
    }


    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expression value = parseExpression();
        return new Ast.Statement.Return(value);
    }

    // ----------------------------------------------------------------------
    // Expressions
    // ----------------------------------------------------------------------

    public Ast.Expression parseExpression() throws ParseException {
        // Top-level rule for expressions starts at logical level
        return parseLogicalExpression();
    }

    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression SeanParrell1 = parseEqualityExpression();
        // Loop handles multiple chained && or || operators
        while (peek(Token.Type.OPERATOR) &&
                (tokens.get(0).getLiteral().equals("&&") || tokens.get(0).getLiteral().equals("||"))) {
            String SeanParrell2 = tokens.get(0).getLiteral(); // operator
            tokens.advance();
            Ast.Expression SeanParrell3 = parseEqualityExpression(); // right side
            SeanParrell1 = new Ast.Expression.Binary(SeanParrell2, SeanParrell1, SeanParrell3);
        }
        return SeanParrell1;
    }

    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression SeanParrell1 = parseAdditiveExpression();
        // Loop handles chained comparisons like == or >=
        while (peek(Token.Type.OPERATOR) &&
                List.of("<", "<=", ">", ">=", "==", "!=").contains(tokens.get(0).getLiteral())) {
            String SeanParrell2 = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression SeanParrell3 = parseAdditiveExpression();
            SeanParrell1 = new Ast.Expression.Binary(SeanParrell2, SeanParrell1, SeanParrell3);
        }
        return SeanParrell1;
    }

    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression SeanParrell1 = parseMultiplicativeExpression();
        // Handle + and - left-associatively
        while (peek(Token.Type.OPERATOR) &&
                (tokens.get(0).getLiteral().equals("+") || tokens.get(0).getLiteral().equals("-"))) {
            String SeanParrell2 = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression SeanParrell3 = parseMultiplicativeExpression();
            SeanParrell1 = new Ast.Expression.Binary(SeanParrell2, SeanParrell1, SeanParrell3);
        }
        return SeanParrell1;
    }

    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression SeanParrell1 = parseSecondaryExpression();
        // Handle * and / left-associatively
        while (peek(Token.Type.OPERATOR) &&
                (tokens.get(0).getLiteral().equals("*") || tokens.get(0).getLiteral().equals("/"))) {
            String SeanParrell2 = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression SeanParrell3 = parseSecondaryExpression();
            SeanParrell1 = new Ast.Expression.Binary(SeanParrell2, SeanParrell1, SeanParrell3);
        }
        return SeanParrell1;
    }

    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression SeanParrell1 = parsePrimaryExpression();
        // Handle property access (expr.name)
        while (match(".")) {
            String name = require(Token.Type.IDENTIFIER).getLiteral();
            if (match("(")) {
                List<Ast.Expression> args = new java.util.ArrayList<>();
                if (!peek(")")) {
                    do {
                        args.add(parseExpression());
                    } while (match(","));
                }
                require(")");
                SeanParrell1 = new Ast.Expression.Function(Optional.of(SeanParrell1), name, args);
            } else {
                SeanParrell1 = new Ast.Expression.Access(Optional.of(SeanParrell1), name);
            }
        }
        return SeanParrell1;
    }

    public Ast.Expression parsePrimaryExpression() throws ParseException {
        // Handle literal keywords
        if (match("NIL")) return new Ast.Expression.Literal(null);
        if (match("TRUE")) return new Ast.Expression.Literal(true);
        if (match("FALSE")) return new Ast.Expression.Literal(false);

        // Integer literal
        if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(Integer.valueOf(tokens.get(-1).getLiteral()));
        }

        // Decimal literal
        if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new java.math.BigDecimal(tokens.get(-1).getLiteral()));
        }

        // Character literal, including escapes
        if (match(Token.Type.CHARACTER)) {
            String SeanParrell1 = tokens.get(-1).getLiteral(); // raw like `'a'` or `'\\n'`
            String SeanParrell2 = SeanParrell1.substring(1, SeanParrell1.length() - 1); // strip quotes
            char SeanParrell3;
            switch (SeanParrell2) {
                case "\\n": SeanParrell3 = '\n'; break;
                case "\\t": SeanParrell3 = '\t'; break;
                case "\\r": SeanParrell3 = '\r'; break;
                case "\\'": SeanParrell3 = '\''; break;
                case "\\\"": SeanParrell3 = '\"'; break;
                case "\\\\": SeanParrell3 = '\\'; break;
                default: SeanParrell3 = SeanParrell2.charAt(0);
            }
            return new Ast.Expression.Literal(SeanParrell3);
        }

        // String literal, with escape replacements
        if (match(Token.Type.STRING)) {
            String SeanParrell1 = tokens.get(-1).getLiteral(); // includes quotes
            String SeanParrell2 = SeanParrell1.substring(1, SeanParrell1.length() - 1);
            String SeanParrell3 = SeanParrell2
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\");
            return new Ast.Expression.Literal(SeanParrell3);
        }

        // Identifier: variable access or function call
        if (match(Token.Type.IDENTIFIER)) {
            String SeanParrell1 = tokens.get(-1).getLiteral();
            if (match("(")) {
                List<Ast.Expression> SeanParrell2 = new java.util.ArrayList<>();
                if (!peek(")")) {
                    do {
                        SeanParrell2.add(parseExpression());
                    } while (match(","));
                }
                if (!match(")")) {
                    throw new ParseException("Expected ')'", tokens.has(0) ? tokens.get(0).getIndex() : -1);
                }
                return new Ast.Expression.Function(Optional.empty(), SeanParrell1, SeanParrell2);
            }
            return new Ast.Expression.Access(Optional.empty(), SeanParrell1);
        }

        // Parenthesized expression
        if (match("(")) {
            Ast.Expression SeanParrell1 = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected ')'", tokens.has(0) ? tokens.get(0).getIndex() : -1);
            }
            return new Ast.Expression.Group(SeanParrell1);
        }

        // If nothing matches, this is invalid
        throw new ParseException("Invalid primary expression", tokens.get(0).getIndex());
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private boolean peek(Object... SeanParrell1) {
        for (int SeanParrell2 = 0; SeanParrell2 < SeanParrell1.length; SeanParrell2++) {
            if (!tokens.has(SeanParrell2)) return false;
            Object SeanParrell3 = SeanParrell1[SeanParrell2];
            Token SeanParrell4 = tokens.get(SeanParrell2);
            if (SeanParrell3 instanceof Token.Type) {
                if (SeanParrell4.getType() != SeanParrell3) return false;
            } else if (SeanParrell3 instanceof String) {
                if (!SeanParrell4.getLiteral().equals(SeanParrell3)) return false;
            } else {
                throw new IllegalArgumentException("Invalid pattern type: " + SeanParrell3);
            }
        }
        return true;
    }

    private boolean match(Object... SeanParrell1) {
        if (peek(SeanParrell1)) {
            for (int SeanParrell2 = 0; SeanParrell2 < SeanParrell1.length; SeanParrell2++) {
                tokens.advance();
            }
            return true;
        }
        return false;
    }
    private Token require(Object expected) throws ParseException {
        if (match(expected)) return tokens.get(-1);
        throw new ParseException("Expected " + expected, tokens.has(0) ? tokens.get(0).getIndex() : -1);
    }

    // Wrapper around the token list that tracks current index
    private static final class TokenStream {
        private final List<Token> SeanParrell1;
        private int SeanParrell2 = 0;

        private TokenStream(List<Token> SeanParrell3) {
            this.SeanParrell1 = SeanParrell3;
        }

        public boolean has(int SeanParrell4) {
            return SeanParrell2 + SeanParrell4 < SeanParrell1.size();
        }

        public Token get(int SeanParrell4) {
            return SeanParrell1.get(SeanParrell2 + SeanParrell4);
        }

        public void advance() {
            SeanParrell2++;
        }
    }
}