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
        throw new UnsupportedOperationException(); // not required yet
    }

    public Ast.Field parseField() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
    }

    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
    }

    // ----------------------------------------------------------------------
    // Statements
    // ----------------------------------------------------------------------

    public Ast.Statement parseStatement() throws ParseException {
        // SeanParrell1: first try to parse any expression (assignment or plain expr)
        Ast.Expression SeanParrell1 = parseExpression();

        // If we see '=', this is an assignment statement
        if (match("=")) {
            Ast.Expression SeanParrell2 = parseExpression(); // right-hand side
            if (!match(";")) {
                throw new ParseException("Expected ';'", tokens.get(0).getIndex());
            }
            // Only an access expression (like a variable) can be assigned to
            if (!(SeanParrell1 instanceof Ast.Expression.Access)) {
                throw new ParseException("Invalid assignment target", tokens.get(0).getIndex());
            }
            return new Ast.Statement.Assignment((Ast.Expression.Access) SeanParrell1, SeanParrell2);
        }

        // Otherwise it must be just an expression followed by a semicolon
        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
        }
        return new Ast.Statement.Expression(SeanParrell1);
    }

    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
    }

    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
    }

    public Ast.Statement.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
    }

    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
    }

    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); // not required yet
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
            if (match(Token.Type.IDENTIFIER)) {
                String SeanParrell2 = tokens.get(-1).getLiteral();
                SeanParrell1 = new Ast.Expression.Access(Optional.of(SeanParrell1), SeanParrell2);
            } else {
                throw new ParseException("Expected identifier after '.'", tokens.get(0).getIndex());
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