package com.costin.converter;

import java.util.ArrayList;
import java.util.Objects;

public class Lexer {

    private static String text;
    private static int pos;
    private static char currentChar;
    private static boolean nullChar;

    private static ArrayList<Token> tokens;
    private static int index;

    private static Token atObj(int i) { // avoids new lines
        if (0 <= i + index && i + index < tokens.size()) {
            Token tk;
            do {
                tk = tokens.get(index + i);
                if (i >= 0)
                    i++;
                else i--;
            } while (Objects.equals(tk.getValue(), "\n"));
            return tk;
        } else return new Token(TokenType.IGNORE, "");
    }

    private static Token atNoSpace(int i) { // avoids spaces
        Token tk;
        do {
            if (0 <= i + index && i + index < tokens.size()) {
                tk = tokens.get(index + i);
                if (i >= 0)
                    i++;
                else i--;
            } else {
                tk = new Token(TokenType.IGNORE, "");
                break;
            }
        } while (Objects.equals(tk.getValue(), "\n") || Objects.equals(tk.getValue(), " "));
        return tk;
    }

    private static Token atNoSemicolon(int i) { // avoids semicolons
        if (0 <= i + index && i + index < tokens.size()) {
            Token tk;
            do {
                tk = tokens.get(index + i);
                if (i >= 0)
                    i++;
                else i--;
            } while (Objects.equals(tk.getValue(), ";"));
            return tk;
        } else return new Token(TokenType.IGNORE, "");
    }

    private static Token at(int i) {
        if (0 <= i + index && i + index < tokens.size()) {
            return tokens.get(index + i);
        } else return new Token(TokenType.IGNORE, "");
    }

    private static boolean canSemicolon() {
        return
                (atNoSemicolon(-1).getValue().equals(" ")
                        || atNoSemicolon(-1).getType().equals(TokenType.KEYWORD)
                        || atNoSemicolon(-1).getType().equals(TokenType.STRING)
                        || atNoSemicolon(-1).getType().equals(TokenType.NUMBER)
                        || atNoSemicolon(-1).getType().equals(TokenType.IDENTIFIER)
                        || (atNoSemicolon(-1).getType().equals(TokenType.OPERATOR)
                        //&& !atNoSemicolon(-1).getValue().equals("}") fuck you } stupid character
                        && !atNoSemicolon(-1).getValue().equals("{")
                        && !atNoSemicolon(-1).getValue().equals("[")
                        && !atNoSemicolon(-1).getValue().equals("(")
                        && !atNoSemicolon(-1).getValue().equals("\n")
                        && !atNoSemicolon(-1).getValue().equals(","))
                )
                        && !atNoSpace(0).getValue().equals("{")
                        && !atNoSpace(0).getValue().equals("[")
                        && !atNoSpace(0).getValue().equals("(")
                        && !atNoSpace(0).getValue().equals(",")

                ;
    }

    public static String postProcess(String text) {
        Lexer.text = text;
        pos = -1;

        currentChar = ' ';
        nullChar = true;
        advance();
        tokens = tokenize();
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < tokens.size(); i++) {
            index = i;

            output.append(at(0).getValue());
        }

        return output.toString();
    }

    public static ArrayList<String> process(String text) {
        ArrayList<String> needed = new ArrayList<>();
        Lexer.text = text;
        pos = -1; // header contains 0xEF 0xBB 0xBF for byte order
        if((int)text.charAt(0) > 176 || (int)text.charAt(0) < 32) {
            pos = 2;
        }
        currentChar = ' ';
        nullChar = true;
        advance();
        tokens = tokenize();
        StringBuilder output = new StringBuilder();

        int indent = 0;

        for (int i = 0; i < tokens.size(); i++) {
            index = i;

            if (at(0).getValue().equals(";")) {
                if (at(1).getValue().equals("\n") || at(1).getValue().equals(";")) continue;
            }

            if (at(0).getType() == TokenType.COMMENT) {
                if (at(-1).getType() != TokenType.COMMENT && !at(-1).getValue().equals("\n")) {
                    if (canSemicolon()) output.append(";");
                    output.append(" ");
                }

                output.append("//");
            }


            // JAVA TRANSFORMATION

            // the package thingy

            if(at(0).getValue().equals("package")) {
                int index3 = 1;
                while (!atNoSpace(index3).getValue().equals("{")) {
                    index3++;
                }
                if(index3 == 1) {
                    at(0).delete();
                    at(1).delete();
                    at(2).delete();
                }
                else atNoSpace(index3).setValue(";");
            }

            // transform keywords
            if (at(0).getType() == TokenType.KEYWORD) {
                switch (at(0).getValue()) {
                    case "static": // not included in doc
                        break; // no code = it's identical in java
                    case "as": // explicit cast
                        // TODO as (non-existent in java)
                        break;
                    case "break":
                    case "case":
                    case "catch":
                    case "class":
                        break;
                    case "const":
                        at(0).setValue("final");
                        break;
                    case "continue":
                    case "default":
                        break;
                    case "delete":
                        // TODO delete (non-existent in java)
                        break;
                    case "enum": // doesn't seem to exist
                    case "extends":
                    case "false":
                    case "finally":
                    case "for":
                        break;
                    case "function":

                        int index = 1;
                        String identifier = at(index).getValue();
                        index++;
                        if (identifier.equals("get") || identifier.equals("set")) {
                            at(index - 1).setValue(" ");
                            identifier = at(index).getValue();
                            index++;
                        }

                        index++; // ( character
                        while (!atNoSpace(index).getValue().equals(")")) {
                            // remake variables in functions
                            if (atNoSpace(index + 1).getValue().equals(":")) {
                                String val = atNoSpace(index + 2).getValue();
                                switch (val) {
                                    case "Number":
                                        val = "double"; // hopefully not float. damn as3
                                        break;
                                    case "Boolean":
                                        val = "boolean"; // (B)oolean is correct in java too but eh
                                        break;

                                    // free to expand if needed!
                                }
                                atNoSpace(index + 1).setValue(val);
                                atNoSpace(index + 2).setValue(atNoSpace(index).getValue());
                                atNoSpace(index).setValue("");
                            }
                            index++;
                        }
                        index++;
                        if(atNoSpace(index).getValue().equals(":")) { // it can mean only one thing

                            atNoSpace(index).delete();
                            index++;

                            {
                                String val = atNoSpace(index).getValue();
                                switch (val) {
                                    case "Number":
                                        val = "double"; // hopefully not float. damn as3
                                        break;
                                    case "Boolean":
                                        val = "boolean"; // (B)oolean is correct in java too but eh
                                        break;

                                    // free to expand if needed!
                                }

                                at(0).setValue(val);
                                at(index).delete();
                            }
                        } else {
                            at(0).delete();
                        }
                        break;
                    case "goto": // probably unused
                        break;
                    case "if":
                    case "implements":
                    case "import":
                        StringBuilder needed2 = new StringBuilder();
                        int index4 = 1;
                        while(at(index4).getType().equals(TokenType.IDENTIFIER) && !at(index4).getValue().equals("\n") || at(index4).getValue().equals(".") || at(index4).getValue().equals("*")) {
                            needed2.append(at(index4).getValue());
                            index4++;
                        }
                        if(needed2.length() != 0) // for some odd reason it can create fake "imports" with 0 length
                        needed.add(needed2.toString());
                        break;
                    case "in":
                    case "instanceof":
                    case "interface":
                        break;
                    case "is": // TODO is (non-existent in java)
                        break;
                    case "namespace": // TODO namespace (non-existent in java)

                        break;
                    case "new":
                    case "null":
                        break;
                    case "package": // moved somewhere else
                        break;
                    case "private":
                    case "public":
                    case "return":
                    case "super":
                    case "switch":
                    case "this":
                    case "throw":
                        break;
                    case "true":
                    case "try":
                        break;
                    case "typeof": // TODO typeof (non-existent in java)

                        break;
                    case "undefined":
                        at(0).setValue("null"); // hopefully right
                        break;
                    case "use": // TODO use (non-existent in java)

                        break;
                    case "var": // TODO var
                        String val = atNoSpace(3).getValue();
                        switch (val) {
                            case "Number":
                                val = "double"; // hopefully not float. damn as3
                                break;
                            case "Boolean":
                                val = "boolean"; // (B)oolean is correct in java too but eh
                                break;

                            // free to expand if needed!
                        }
                        atNoSpace(0).setValue(val);
                        atNoSpace(2).setValue("");
                        atNoSpace(3).setValue("");
                        break;
                    case "void":
                        break;
                    case "with": // TODO with (non-existent in java)

                        break;
                    case "while":
                        break;
                    case "override":
                        int index2 = -1;
                        while(!at(index2).getValue().equals("\n")) {
                            index2--;
                        }
                        //at(index2 - 1).setValue("@Override" + at(index2 - 1).getValue());
                        //sadly this is not gonna work because of how I made the StringBuilder append.

                        at(0).delete();
                        break;
                }
            }
            // JAVA TRANSFORMATION

            if (at(0).getType() == TokenType.OPERATOR) {
                if (!at(0).getValue().equals(":")
                        && !at(0).getValue().equals(".")
                        && !at(0).getValue().equals("\n")
                        && !at(0).getValue().equals("(")
                        && !at(0).getValue().equals(")")
                        && !at(0).getValue().equals(",")
                        && !at(0).getValue().equals("]")
                        && !at(0).getValue().equals("[")
                        && !at(0).getValue().equals(";")

                        && !(at(0).getValue().equals("!") && at(-1).getValue().equals("("))
                        || at(-1).getType() == TokenType.OPERATOR
                        &&
                        (at(-1).getValue().equals("**")
                                || at(-1).getValue().equals("*")
                                || at(-1).getValue().equals("/")
                                || at(-1).getValue().equals("%")
                                || at(-1).getValue().equals("+")
                                || at(-1).getValue().equals("-")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                                || at(-1).getValue().equals("")
                        )
                ) {
                    // idk the role of this so I commented it.
                    //if (!(!at(0).getValue().equals("{") && at(-1).getValue().equals("\n"))) {}
                        //output.append(" "); // put space before token //
                }
            }


            if (at(0).getType() == TokenType.STRING) output.append('"');
            if (!at(0).getValue().equals("\n")) output.append(at(0).getValue());
            if (at(0).getType() == TokenType.STRING) output.append('"');

            if (((
                    at(0).getType() == TokenType.OPERATOR
                            && (!at(0).getValue().equals(":")
                            && !at(0).getValue().equals(".")
                            && !at(0).getValue().equals("\n")
                            && !at(0).getValue().equals("(")
                            && !at(0).getValue().equals("[")
                            && !at(0).getValue().equals("!")
                    ))
                    || at(0).getType() == TokenType.IDENTIFIER
                    || at(0).getType() == TokenType.KEYWORD
                    || at(0).getType() == TokenType.STRING)
                    && (
                    at(1).getType() == TokenType.IDENTIFIER
                            || at(1).getType() == TokenType.KEYWORD
                            || at(1).getType() == TokenType.NUMBER
                            || at(1).getType() == TokenType.STRING)) {
                output.append(" "); // put space after token //
            }

            if (at(0).getType() == TokenType.OPERATOR) {
                switch (at(0).getValue()) {
                    case "\n":
                        if (canSemicolon()) output.append(";");

                        if (!Objects.equals(at(1).getValue(), "{")) output.append("\n");
                        if (!at(1).getValue().equals("{") && !at(1).getValue().equals("}"))
                            for (int j = 0; j < indent; j++) {
                                output.append("    ");
                            }
                        break;
                    case "}":
                        if (!Objects.equals(at(-1).getValue(), "{")) {
                            indent--;
                            output.setLength(output.length() - 1);
                            for (int j = 0; j < indent; j++) {
                                output.append("    ");
                            }
                            if(indent != 0) // might want to remove this in case of invisible bugs
                                output.append('}');
                        }
                        break;
                    case "{":
                        if (!Objects.equals(at(1).getValue(), "}")) {
                            indent++;
                        }
                        break;
                    default: break;
                }
            }
        }

        //output
        needed.add(0, output.toString());
        return needed;
    }

    private static void advance() {
        pos++;
        if (pos < text.length()) {
            currentChar = text.charAt(pos);
            nullChar = false;
        } else nullChar = true;
    }

    private static Token procIdentifier() {
        StringBuilder token = new StringBuilder();

        while (!nullChar && (Character.isLetter(currentChar) || Character.isDigit(currentChar) || currentChar == '_' || currentChar == '$')) {
            token.append(currentChar);
            advance();
        }

        String str = token.toString();
        if (str.equals("")) return null; // prob never
        switch (str) {
            case "static": // not included in doc

            case "as": // explicit cast
            case "break":
            case "case":
            case "catch":
            case "class":
            case "const":
            case "continue":
            case "default":
            case "delete":
            case "enum":
            case "extends":
            case "false":
            case "finally":
            case "for":
            case "function":
            case "goto":
            case "if":
            case "implements":
            case "import":
            case "in":
            case "instanceof":
            case "interface":
            case "is":
            case "namespace":
            case "new":
            case "null":
            case "package":
            case "private":
            case "public":
            case "return":
            case "super":
            case "switch":
            case "this":
            case "throw":
            case "true":
            case "try":
            case "typeof":
            case "undefined":
            case "use":
            case "var":
            case "void":
            case "with":
            case "while":
            case "override":
                return new Token(TokenType.KEYWORD, str);
        }
        return new Token(TokenType.IDENTIFIER, str);
    }

    private static Token procNumber() {
        StringBuilder token = new StringBuilder();
        int dotCount = 0;

        while (!nullChar && (Character.isDigit(currentChar) || currentChar == '.')) {
            if (currentChar == '.') {
                if (dotCount == 1) break;
                dotCount++;
            } else token.append(currentChar);
            advance();
        }

        if (token.toString().equals("")) return null;
        return new Token(TokenType.NUMBER, token.toString());
    }

    private static Token procString(boolean doubleQuote) {
        StringBuilder token = new StringBuilder();
        advance();
        boolean escapedStr = false; // to avoid stopping at \"
        boolean wasSlash = false; // to remove useless '\' for single quotes
        while (!nullChar) {
            //if (wasSlash && currentChar == '\'') token.setLength(token.length() - 1);
            //token.append(currentChar != '\"' ? currentChar : "\\\"");

            //escapedStr = currentChar == '\\';
            //wasSlash = currentChar == '\\';

            //if (!escapedStr && currentChar == '"' && doubleQuote) break;
            //if (!escapedStr && currentChar == '\'' && !doubleQuote) break;

            if(doubleQuote && currentChar == '"') break;
            if(!doubleQuote && currentChar == '\'') break;
            //token.append(currentChar);

            advance();
        }
        advance();


        return new Token(TokenType.STRING, token.toString());
    }

    private static Token procSingleComment() {
        StringBuilder token = new StringBuilder();
        advance();

        while (!nullChar && currentChar != '\n') {
            token.append(currentChar);
            advance();
        }

        return new Token(TokenType.COMMENT, token.toString());
    }

    private static Token procMultiComment() {
        StringBuilder token = new StringBuilder();
        advance();

        boolean shouldAdvance = true;
        while (!nullChar) {
            token.append(currentChar);
            if(shouldAdvance)
                advance();
            else shouldAdvance = true;

            if(currentChar == '*') {
                advance();
                if(currentChar == '/') {
                    break;
                } else {
                    shouldAdvance = false;
                }
            }
        }

        return new Token(TokenType.COMMENT, token.toString());
    }

    private static Token procOperator() {

        switch (currentChar) {
            case '~':
            case '!':
            case '+': //++
            case '-': //--
            case '[':
            case ']':
            case '(':
            case ')':
            case '{':
            case '}':
                // ::
            case '.':
                // **
            case '*':
            case '/':
            case '%':
                //<<
                //!<
                //>>
                //!>
                //>>>
                //<=
                //>=
                //~=
                //==
                //===
                //!=
            case '<':
            case '>':
                //!==
            case '&':
            case '^':
            case '|':
                //&&
                //^^
                //||
                //?<
                //?>
            case '=': //:=
                //**=
                //*=
                // /=
                //%=
                //+=
                //-=
                //<<=
                //!<=
                //>>=
                //!>=
                //>>>=
                //&=
                //^=
                //|=
                //&&=
                //^^=
                //||=
                //?<=
                //?>=
                //,

                // ... ? ... : ...
        }
        return null;
    }

    private static ArrayList<Token> tokenize() {
        ArrayList<Token> tokens = new ArrayList<>();
        while (!nullChar) {
            Token token = null;

            if (currentChar == '/') {
                advance();
                if (currentChar == '/') {
                    token = procSingleComment();
                } else if (currentChar == '*') {
                    token = procMultiComment();
                }
                else {
                    advance();
                    token = new Token(TokenType.OPERATOR, "/");
                }
            }

            if (currentChar == '&') {
                advance();
                if (currentChar == '&') {
                    token = new Token(TokenType.OPERATOR, "&&");
                    advance();
                } else {
                    advance();
                    token = new Token(TokenType.OPERATOR, "&");
                }
            }
            if (currentChar == '|') {
                advance();
                if (currentChar == '|') {
                    token = new Token(TokenType.OPERATOR, "||");
                    advance();
                } else {
                    advance();
                    token = new Token(TokenType.OPERATOR, "|");
                }
            }

            if (token == null) { // idk why
                switch (currentChar) {
                    case ' ':
                    case '\t':
                        advance();
                        break;
                    case '\n':
                    case '(':
                    case ')':
                    case '[':
                    case ']':
                    case '{':
                    case '}':

                    case ':':
                        // temp zone
                    case '~':
                    case '!':
                    case '+':
                    case '-':

                    case ';': // in case of multiple statements in one line

                    case '*':
                    case '%':
                    case '<':
                    case '>':
                    case '^':
                    case '=':
                    case ',':
                    case '.':
                        token = new Token(TokenType.OPERATOR, String.valueOf(currentChar));
                        advance();
                        break;

                    case '\'':
                        token = procString(false);
                        break;
                    case '"':
                        token = procString(true);
                        break;
                    default:
                        if (Character.isLetter(currentChar) || currentChar == '_' || currentChar == '$')
                            token = procIdentifier();
                        else if (Character.isDigit(currentChar) || currentChar == '.') {
                            token = procNumber(); // FIXME Infinity, NaN, Octal
                        } else {
                            advance();
                        }
                }
            }

            if (token != null) {
                tokens.add(token);
            }
        }

        return tokens;
    }
}
