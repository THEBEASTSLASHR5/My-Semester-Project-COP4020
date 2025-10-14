package plc.project;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    public List<Token> lex() {
        // SeanParrell1 is a new ArrayList we made to store all the tokens we find
        List<Token> SeanParrell1 = new ArrayList<>();
        // loop while we still have chars in the input
        while (chars.has(0)) {
            // skip whitespace, we dont want to emit them as tokens
            if (peek("[ \b\n\r\t]")) {
                chars.advance();   // move forward in the stream
                chars.skip();      // reset token length since we ignored this space
            } else {
                // otherwise, create a token and add it into our SeanParrell1 list
                SeanParrell1.add(lexToken());
            }
        }
        // return the final list of tokens we collected
        return SeanParrell1;
    }

    public Token lexToken() {
        // Decide what kind of token to make based on first char
        // if first char is letter or underscore -> IDENTIFIER
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
            // if first char is sign or digit -> NUMBER
        } else if (peek("[+-]", "[0-9]") || peek("[0-9]")) {
            return lexNumber();
            // if first char is single quote -> CHARACTER literal
        } else if (peek("'")) {
            return lexCharacter();
            // if first char is double quote -> STRING literal
        } else if (peek("\"")) {
            return lexString();
            // otherwise default to OPERATOR (symbols like =, ;, etc.)
        } else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        // loop through letters, digits, underscore or hyphen
        while (peek("[A-Za-z0-9_-]")) {
            chars.advance();  // consume the charcter
        }
        // emit IDENTIFIER token from the consumed chars
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        // allow optional + or - sign
        match("[+-]");

        // check if number starts with 0
        if (match("0")) {
            // if another digit comes after 0 -> error because leading zero numbr
            if (peek("[0-9]")) {
                throw new ParseException("Leading zero not allowed", chars.index);
            }
            // if number starts with nonzero digit
        } else if (match("[1-9]")) {
            // keep consuming digits until they end
            while (match("[0-9]")) {
            }
            // if it wasnt a digit at all -> invalid numbr
        } else {
            throw new ParseException("Invalid number", chars.index);
        }

        // if there is a decimal point, then its a DECIMAL token
        if (match("\\.")) {
            // must be followed by digits, otherwise invalid
            if (!match("[0-9]")) {
                throw new ParseException("Invalid decimal", chars.index);
            }
            // consume fraction digits
            while (match("[0-9]")) {
            }
            return chars.emit(Token.Type.DECIMAL);
        }
        // if no decimal, then its just INTEGER token
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        // must start with single quote '
        if (!match("'")) {
            throw new ParseException("Character literal must start with single quote", chars.index);
        }
        // if first char is backslash, it means escape seq like '\n'
        if (match("\\\\")) {
            lexEscape();
            // if normal char (not quote, not newline)
        } else if (peek("[^'\n\r]")) {
            chars.advance();  // consume that single charcter
            // otherwise invalid -> empty or wrong char literal
        } else {
            throw new ParseException("Invalid or empty character literal", chars.index);
        }
        // must end with closing single quote '
        if (!match("'")) {
            throw new ParseException("Unterminated character literal", chars.index);
        }
        // emit CHARACTER token
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        // must start with double quote "
        if (!match("\"")) {
            throw new ParseException("String must start with double quote", chars.index);
        }
        // loop until closing " is found
        while (!peek("\"") && chars.has(0)) {
            // if escape sequence like \n, handle it
            if (match("\\\\")) {
                lexEscape();
                // if normal char that is not newline, consume it
            } else if (peek("[^\n\r]")) {
                chars.advance();
                // otherwise invalid character inside string
            } else {
                throw new ParseException("Invalid character in string", chars.index);
            }
        }
        // if we never found a closing quote, thats unterminated string
        if (!match("\"")) {
            throw new ParseException("Unterminated string literal", chars.index);
        }
        // emit STRING token
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        // escape must be one of b, n, r, t, ', ", \
        if (!(match("b") || match("n") || match("r") || match("t") ||
                match("'") || match("\"") || match("\\\\"))) {
            throw new ParseException("Invalid escape sequence", chars.index);
        }
    }

    public Token lexOperator() {
        // List of multi-character operators to check first
        String[] multiOps = {
                "==", "!=", "<=", ">=", "&&", "||"
        };

        // Try to match multi-character operators
        for (String op : multiOps) {
            if (peek(op.split(""))) {  // peek each char of the operator
                for (int i = 0; i < op.length(); i++) {
                    chars.advance();
                }
                return chars.emit(Token.Type.OPERATOR);
            }
        }

        // If not multi-char, just consume a single symbol
        if (peek(".")) {
            chars.advance();
            return chars.emit(Token.Type.OPERATOR);
        }
        if (peek("[^A-Za-z0-9\\s]")) { // any non-alphanumeric, non-whitespace char
            chars.advance();
            return chars.emit(Token.Type.OPERATOR);
        }

        // If nothing matched, throw error
        throw new ParseException("Invalid operator", chars.index);
    }


    public boolean peek(String... SeanParrell2) {
        // this method looks ahead without consuming chars
        // SeanParrell2 holds regex patterns we want to match
        for (int SeanParrell3 = 0; SeanParrell3 < SeanParrell2.length; SeanParrell3++) {
            // if we dont even have that many chars left -> fail
            if (!chars.has(SeanParrell3)) {
                return false;
            }
            // check if the character matches the regex pattern
            if (!String.valueOf(chars.get(SeanParrell3)).matches(SeanParrell2[SeanParrell3])) {
                return false;
            }
        }
        return true;
    }

    public boolean match(String... SeanParrell4) {
        // match uses peek to check, then consumes if its true
        if (peek(SeanParrell4)) {
            for (int SeanParrell5 = 0; SeanParrell5 < SeanParrell4.length; SeanParrell5++) {
                chars.advance(); // actually consume charcters
            }
            return true;
        }
        return false;
    }

    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
