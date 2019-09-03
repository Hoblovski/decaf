package decaf.parsing;

import decaf.Driver;
import decaf.error.DecafError;
import decaf.error.MsgError;
import decaf.tree.Tree;

import java.util.Optional;

/**
 * The abstract parser specifies all methods that a concrete parser should implement, and provide a couple of helper
 * methods.
 */
public abstract class AbstractParser {

    /**
     * The exported method for invoking the parsing process.
     *
     * @return the parsed tree (if any)
     */
    public Optional<Tree.TopLevel> tree() {
        if (parse()) return Optional.of(tree);
        return Optional.empty();
    }

    /**
     * The entry to the concrete parser.
     *
     * @return if parse succeeds?
     */
    abstract protected boolean parse();

    /**
     * When parsing, we need to interact with the lexer.
     */
    AbstractLexer lexer;

    /**
     * Final parsing result to be written by the concrete parser. Remember in `Decaf.jacc`, we designed an action for
     * TopLevel:
     * {{{
     * tree = new Tree.TopLevel($1.clist, $1.loc);
     * }}}
     */
    protected Tree.TopLevel tree;

    /**
     * Helper variable used by the concrete parser: the semantic value of the current token.
     * <p>
     * Set by the lexer.
     */
    protected SemValue semValue;

    /**
     * Helper variable used by the concrete parser: the current token.
     */
    protected int token;

    /**
     * Helper method used by the concrete parser: fetch the next token.
     *
     * @return the next token.
     */
    protected int nextToken() {
        token = -1;
        try {
            token = lexer.yylex();
        } catch (Exception e) {
            yyerror("lexer error: " + e.getMessage());
        }

        return token;
    }

    /**
     * Helper method used by the concrete parser: report error.
     *
     * @param msg the error message
     */
    protected void yyerror(String msg) {
        issueError(new MsgError(lexer.getPos(), msg));
    }

    protected void issueError(DecafError error) {
        Driver.getDriver().issueError(error);
    }
}