package sintatica;

import lexical.LexicalAnalyzer;
import lexical.Token;
import lexical.TokenType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void parseProgram() {
        while (currentTokenIndex < tokens.size()) {
            declaracao();
        }
    }

    private void declaracao() {
        if (isFunctionDeclaration()) {
            parseFunction();
        } else if (isVariableDeclaration()) {
            parseVariableDeclaration();
        } else {
            throw new RuntimeException("Erro: Token inesperado fora de uma declaração: " + peek());
        }
    }

    private void parseFunction() {
        parseFunctionDeclaration();

        accept(TokenType.SYMBOL); // '{'

        while (isDeclaration()) {
            declaracao();
        }

        while (!peek().getType().equals(TokenType.SYMBOL) || !peek().getValue().equals("}")) {
            parseCommand();
        }

        accept(TokenType.SYMBOL); // '}'
    }

    private void parseFunctionDeclaration() {
        tipo();
        accept(TokenType.IDENTIFIER);
        accept(TokenType.SYMBOL); // '('
        // Lógica para análise de parâmetros pode ser adicionada aqui, se necessário
        accept(TokenType.SYMBOL); // ')'
    }

    private void parseVariableDeclaration() {
        tipo();
        accept(TokenType.IDENTIFIER);
        while (peek().getType() == TokenType.SYMBOL && peek().getValue().equals(",")) {
            consume();
            accept(TokenType.IDENTIFIER);
        }

        if (peek().getType() == TokenType.SYMBOL && peek().getValue().equals("[")) {
            consume();
            accept(TokenType.CONSTANT);
            accept(TokenType.SYMBOL); // ']'
        }

        accept(TokenType.SYMBOL); // ';'
    }

    private void parseCommand() {
        Token token = peek();

        switch (token.getType()) {
            case IDENTIFIER -> parseExpressionCommand();
            case KEYWORD -> {
                switch (token.getValue()) {
                    case "if" -> parseIfCommand();
                    case "while" -> parseWhileCommand();
                    case "return" -> parseReturnCommand();
                    default -> throw new RuntimeException("Erro: Comando inválido: " + token);
                }
            }
            case SYMBOL -> {
                if (token.getValue().equals("{")) {
                    parseCompoundCommand();
                } else {
                    throw new RuntimeException("Erro: Comando inválido: " + token);
                }
            }
            default -> throw new RuntimeException("Erro: Comando inesperado: " + token);
        }
    }

    private void parseExpressionCommand() {
        parseExpression();
        accept(TokenType.SYMBOL); // ';'
    }

    private void parseIfCommand() {
        accept(TokenType.KEYWORD); // "if"
        accept(TokenType.SYMBOL); // '('
        parseExpression();
        accept(TokenType.SYMBOL); // ')'
        parseCommand();
        if (peek().getType() == TokenType.KEYWORD && peek().getValue().equals("else")) {
            consume();
            parseCommand();
        }
    }

    private void parseWhileCommand() {
        accept(TokenType.KEYWORD); // "while"
        accept(TokenType.SYMBOL); // '('
        parseExpression();
        accept(TokenType.SYMBOL); // ')'
        parseCommand();
    }

    private void parseReturnCommand() {
        accept(TokenType.KEYWORD); // "return"
        if (peek().getType() != TokenType.SYMBOL || !peek().getValue().equals(";")) {
            parseExpression();
        }
        accept(TokenType.SYMBOL); // ';'
    }

    private void parseCompoundCommand() {
        accept(TokenType.SYMBOL); // '{'
        while (!peek().getType().equals(TokenType.SYMBOL) || !peek().getValue().equals("}")) {
            parseCommand();
        }
        accept(TokenType.SYMBOL); // '}'
    }

    private void parseExpression() {
        parseAssignmentExpression();
    }

    private void parseAssignmentExpression() {
        parseSimpleExpression();
        if (peek().getType() == TokenType.OPERATOR && peek().getValue().equals("=")) {
            consume(); // consome '='
            parseAssignmentExpression();
        } else if (isRelationalOperator(peek())) {
            consume(); // consome operador relacional
            parseSimpleExpression();
        }
    }

    private void parseSimpleExpression() {
        parseTerm();
        while (peek().getType() == TokenType.OPERATOR &&
                (peek().getValue().equals("+") || peek().getValue().equals("-"))) {
            consume();
            parseTerm();
        }
    }

    private void parseTerm() {
        parseFactor();
        while (peek().getType() == TokenType.OPERATOR &&
                (peek().getValue().equals("*") || peek().getValue().equals("/"))) {
            consume();
            parseFactor();
        }
    }

    private void parseFactor() {
        Token token = peek();
        switch (token.getType()) {
            case CONSTANT, IDENTIFIER -> consume();
            case SYMBOL -> {
                if (token.getValue().equals("(")) {
                    consume();
                    parseExpression();
                    accept(TokenType.SYMBOL); // ')'
                } else {
                    throw new RuntimeException("Erro: Fator inesperado: " + token);
                }
            }
            default -> throw new RuntimeException("Erro: Fator inesperado: " + token);
        }
    }

    private boolean isRelationalOperator(Token token) {
        return token.getType() == TokenType.OPERATOR && (
                token.getValue().equals("<") || token.getValue().equals("<=") ||
                token.getValue().equals(">") || token.getValue().equals(">=") ||
                token.getValue().equals("==") || token.getValue().equals("!="));
    }

    private void tipo() {
        Token token = consume();
        if (token.getType() != TokenType.KEYWORD ||
                !(token.getValue().equals("int") || token.getValue().equals("void") || token.getValue().equals("bool"))) {
            throw new RuntimeException("Erro: Tipo esperado, encontrado: " + token);
        }
    }

    private void accept(TokenType expected) {
        Token token = consume();
        if (token.getType() != expected) {
            throw new RuntimeException("Erro: Esperado " + expected + ", encontrado: " + token);
        }
    }

    private Token consume() {
        if (currentTokenIndex >= tokens.size()) {
            throw new RuntimeException("Erro: Fim dos tokens inesperado");
        }
        return tokens.get(currentTokenIndex++);
    }

    private Token peek() {
        if (currentTokenIndex >= tokens.size()) {
            throw new RuntimeException("Erro: Fim dos tokens inesperado");
        }
        return tokens.get(currentTokenIndex);
    }

    private boolean isDeclaration() {
        return isFunctionDeclaration() || isVariableDeclaration();
    }

    private boolean isFunctionDeclaration() {
        if (currentTokenIndex + 2 < tokens.size()) {
            Token typeToken = tokens.get(currentTokenIndex);
            Token identifierToken = tokens.get(currentTokenIndex + 1);
            Token nextToken = tokens.get(currentTokenIndex + 2);
            return typeToken.getType() == TokenType.KEYWORD &&
                    (typeToken.getValue().equals("int") || typeToken.getValue().equals("void") || typeToken.getValue().equals("bool")) &&
                    identifierToken.getType() == TokenType.IDENTIFIER &&
                    nextToken.getType() == TokenType.SYMBOL && nextToken.getValue().equals("(");
        }
        return false;
    }

    private boolean isVariableDeclaration() {
        if (currentTokenIndex + 1 < tokens.size()) {
            Token typeToken = tokens.get(currentTokenIndex);
            Token identifierToken = tokens.get(currentTokenIndex + 1);
            return typeToken.getType() == TokenType.KEYWORD &&
                    (typeToken.getValue().equals("int") || typeToken.getValue().equals("void") || typeToken.getValue().equals("bool")) &&
                    identifierToken.getType() == TokenType.IDENTIFIER;
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            LexicalAnalyzer lexer = new LexicalAnalyzer(
                "src/lexical/symbols.txt",
                "src/lexical/keywords.txt"
            );

            String inputFilePath = "src/lexical/input.txt";
            StringBuilder code = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    code.append(line).append("\n");
                }
            }

            List<Token> tokens = lexer.analyze(code.toString());

            Parser parser = new Parser(tokens);
            parser.parseProgram();

            System.out.println("Análise concluída com sucesso!");
        } catch (IOException e) {
            System.err.println("Erro ao carregar arquivos: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Erro de análise: " + e.getMessage());
        }
    }
}
