package lexical;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexicalAnalyzer {
    private Set<String> symbols;
    private Set<String> keywords;

    private static final String IDENTIFIER_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*";
    private static final String CONSTANT_PATTERN = "\\b\\d+\\b";
    private static final String OPERATOR_PATTERN = "[+\\-*/=<>]";

    public LexicalAnalyzer(String symbolsFilePath, String keywordsFilePath) throws IOException {
        symbols = loadFile(symbolsFilePath);
        keywords = loadFile(keywordsFilePath);
    }

    private Set<String> loadFile(String filePath) throws IOException {
        Set<String> data = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                data.add(line.trim());
            }
        }
        return data;
    }

    public String loadInputFile(String filePath) throws IOException {
        StringBuilder code = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line).append("\n");
            }
        }
        return code.toString();
    }

    public List<Token> analyze(String input) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;

        while (index < input.length()) {
            boolean matched = false;

            // Ignora espaços em branco
            if (Character.isWhitespace(input.charAt(index))) {
                index++;
                continue;
            }

            // Check for symbols
            for (String symbol : symbols) {
                if (input.startsWith(symbol, index)) {
                    tokens.add(new Token(TokenType.SYMBOL, symbol));
                    index += symbol.length();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                Matcher matcher = Pattern.compile(
                        IDENTIFIER_PATTERN + "|" +
                                CONSTANT_PATTERN + "|" +
                                OPERATOR_PATTERN
                ).matcher(input);

                if (matcher.find(index) && matcher.start() == index) {
                    String tokenValue = matcher.group();

                    // Classify token
                    TokenType type = classifyToken(tokenValue);
                    tokens.add(new Token(type, tokenValue));
                    index += tokenValue.length();
                    matched = true;
                }
            }

            if (!matched) {
                // Handle unknown characters
                tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(input.charAt(index))));
                index++;
            }
        }

        return tokens;
    }

    private TokenType classifyToken(String token) {
        if (keywords.contains(token)) return TokenType.KEYWORD;
        if (token.matches(IDENTIFIER_PATTERN)) return TokenType.IDENTIFIER;
        if (token.matches(CONSTANT_PATTERN)) return TokenType.CONSTANT;
        if (token.matches(OPERATOR_PATTERN)) return TokenType.OPERATOR;
        return TokenType.UNKNOWN;
    }

    public static void main(String[] args) {
        try {
            LexicalAnalyzer analyzer = new LexicalAnalyzer(
                "src/lexical/symbols.txt",
                "src/lexical/keywords.txt"
            );


            String inputFilePath = "src/lexical/input.txt";
            String code = analyzer.loadInputFile(inputFilePath);

            List<Token> tokens = analyzer.analyze(code);

            for (Token token : tokens) {
                System.out.println(token);
            }
        } catch (IOException e) {
            System.err.println("Error loading file: " + e.getMessage());
        }
    }
}
