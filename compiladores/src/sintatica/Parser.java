package sintatica;

import lexical.LexicalAnalyzer;
import lexical.Token;
import lexical.TokenType;

import java.util.List;
import java.io.IOException;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public TreeNode parseProgram() {
        TreeNode root = new TreeNode("Programa");

        while (currentTokenIndex < tokens.size()) {
            Token token = peek();

            if (token.getType() == TokenType.KEYWORD &&
                (token.getValue().equals("void") || token.getValue().equals("int") || token.getValue().equals("bool"))) {
                if (isFunctionDeclaration()) {
                    root.addChild(parseFunction());
                } else {
                    root.addChild(parseDeclaration());
                }
            } else {
                throw new RuntimeException("Erro: Token inesperado fora de uma função ou declaração: " + token);
            }
        }

        return root;
    }

    private TreeNode parseFunction() {
        TreeNode functionNode = new TreeNode("Função");

        // Declaração da função
        functionNode.addChild(parseDeclaration());

        // Espera abertura do bloco '{'
        Token token = consume();
        if (token.getType() != TokenType.SYMBOL || !token.getValue().equals("{")) {
            throw new RuntimeException("Erro: '{' esperado após declaração da função, encontrado: " + token);
        }
        functionNode.addChild(new TreeNode("{"));

        // Processa declarações ou comandos dentro do bloco
        while (peek().getType() != TokenType.SYMBOL || !peek().getValue().equals("}")) {
            if (isDeclaration()) {
                functionNode.addChild(parseDeclaration());
            } else {
                functionNode.addChild(parseCommand());
            }
        }

        // Espera fechamento do bloco '}'
        token = consume();
        if (token.getType() != TokenType.SYMBOL || !token.getValue().equals("}")) {
            throw new RuntimeException("Erro: '}' esperado no final do bloco da função, encontrado: " + token);
        }
        functionNode.addChild(new TreeNode("}"));

        return functionNode;
    }

    private boolean isDeclaration() {
        if (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);
            return token.getType() == TokenType.KEYWORD &&
                   (token.getValue().equals("void") || token.getValue().equals("int") || token.getValue().equals("bool"));
        }
        return false;
    }


    private TreeNode parseDeclaration() {
        TreeNode declarationNode = new TreeNode("Declaração");

        // Tipo
        Token token = consume();
        if (token.getType() != TokenType.KEYWORD || !(token.getValue().equals("void") || token.getValue().equals("int") || token.getValue().equals("bool"))) {
            throw new RuntimeException("Erro: Tipo esperado, encontrado: " + token);
        }
        declarationNode.addChild(new TreeNode(token.getValue()));

        // Identificador
        token = consume();
        if (token.getType() != TokenType.IDENTIFIER) {
            throw new RuntimeException("Erro: Identificador esperado, encontrado: " + token);
        }
        declarationNode.addChild(new TreeNode(token.getValue()));

        // Verifica se é uma função
        if (peek().getType() == TokenType.SYMBOL && peek().getValue().equals("(")) {
            consume(); // Consome '('
            if (peek().getType() == TokenType.SYMBOL && peek().getValue().equals(")")) {
                consume(); // Consome ')'
                declarationNode.addChild(new TreeNode("Função"));
                return declarationNode;
            } else {
                throw new RuntimeException("Erro: ')' esperado após '(', encontrado: " + peek());
            }
        }

        // Declaração de variáveis (vírgulas opcionais)
        while (peek().getType() == TokenType.SYMBOL && peek().getValue().equals(",")) {
            consume(); // Consome ','
            token = consume(); // Consome próximo identificador
            if (token.getType() != TokenType.IDENTIFIER) {
                throw new RuntimeException("Erro: Identificador esperado após ',', encontrado: " + token);
            }
            declarationNode.addChild(new TreeNode(token.getValue()));
        }

        // ';'
        token = consume();
        if (token.getType() != TokenType.SYMBOL || !token.getValue().equals(";")) {
            throw new RuntimeException("Erro: ';' esperado, encontrado: " + token);
        }

        return declarationNode;
    }

    private TreeNode parseCommand() {
        TreeNode commandNode = new TreeNode("Comando");
        Token token = peek();

        if (token.getType() == TokenType.KEYWORD && token.getValue().equals("while")) {
            consume(); // Consome 'while'
            commandNode.addChild(new TreeNode("while"));

            // '('
            token = consume();
            if (token.getType() != TokenType.SYMBOL || !token.getValue().equals("(")) {
                throw new RuntimeException("Erro: '(' esperado, encontrado: " + token);
            }

            // Expressão
            commandNode.addChild(parseExpression());

            // ')'
            token = consume();
            if (token.getType() != TokenType.SYMBOL || !token.getValue().equals(")")) {
                throw new RuntimeException("Erro: ')' esperado, encontrado: " + token);
            }

            // Comando
            commandNode.addChild(parseCommand());
        } else if (token.getType() == TokenType.IDENTIFIER) {
            consume(); // Consome identificador
            commandNode.addChild(new TreeNode(token.getValue()));

            // '='
            token = consume();
            if (token.getType() != TokenType.OPERATOR || !token.getValue().equals("=")) {
                throw new RuntimeException("Erro: '=' esperado, encontrado: " + token);
            }

            // Expressão
            commandNode.addChild(parseExpression());

            // ';'
            token = consume();
            if (token.getType() != TokenType.SYMBOL || !token.getValue().equals(";")) {
                throw new RuntimeException("Erro: ';' esperado, encontrado: " + token);
            }
        } else if (token.getType() == TokenType.SYMBOL && token.getValue().equals("{")) {
            consume(); // Consome '{'
            commandNode.addChild(new TreeNode("{"));

            while (peek().getType() != TokenType.SYMBOL || !peek().getValue().equals("}")) {
                commandNode.addChild(parseCommand());
            }

            // '}'
            token = consume();
            if (token.getType() != TokenType.SYMBOL || !token.getValue().equals("}")) {
                throw new RuntimeException("Erro: '}' esperado, encontrado: " + token);
            }
            commandNode.addChild(new TreeNode("}"));
        } else {
            throw new RuntimeException("Erro: Comando inválido: " + token);
        }

        return commandNode;
    }


    private TreeNode parseExpression() {
        TreeNode expressionNode = new TreeNode("Expressão");
        Token token = consume();

        if (token.getType() == TokenType.IDENTIFIER || token.getType() == TokenType.CONSTANT) {
            expressionNode.addChild(new TreeNode(token.getValue()));

            if (peek().getType() == TokenType.OPERATOR) {
                token = consume();
                expressionNode.addChild(new TreeNode(token.getValue()));
                expressionNode.addChild(parseExpression());
            }
        } else {
            throw new RuntimeException("Erro: Expressão inválida: " + token);
        }

        return expressionNode;
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

    private boolean isFunctionDeclaration() {
        if (currentTokenIndex + 2 < tokens.size()) {
            Token identifierToken = tokens.get(currentTokenIndex + 1);
            Token nextToken = tokens.get(currentTokenIndex + 2);
            return identifierToken.getType() == TokenType.IDENTIFIER &&
                   nextToken.getType() == TokenType.SYMBOL && nextToken.getValue().equals("(");
        }
        return false;
    }


    public static void main(String[] args) {
        try {
            LexicalAnalyzer lexer = new LexicalAnalyzer(
                "src/lexical/symbols.txt",
                "src/lexical/keywords.txt"
            );

            String code = """
                void p() {
                    int a1, b, ccc;
                    bool e, d;
                    while (e > 100)
                        a1 = b * ccc;
                }
            """;

            List<Token> tokens = lexer.analyze(code);

            Parser parser = new Parser(tokens);
            TreeNode tree = parser.parseProgram();

            System.out.println(tree);
        } catch (IOException e) {
            System.err.println("Erro ao carregar arquivos: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Erro de análise: " + e.getMessage());
        }
    }
}
