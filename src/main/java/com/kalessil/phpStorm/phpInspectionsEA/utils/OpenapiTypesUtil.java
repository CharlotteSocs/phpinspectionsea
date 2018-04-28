package com.kalessil.phpStorm.phpInspectionsEA.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final public class OpenapiTypesUtil {
    final public static TokenSet tsCOMPARE_EQUALITY_OPS = TokenSet.create(
            PhpTokenTypes.opEQUAL,
            PhpTokenTypes.opNOT_EQUAL,
            PhpTokenTypes.opIDENTICAL,
            PhpTokenTypes.opNOT_IDENTICAL
            /* PS 2017.3 included instanceof here, hence we are duplicating the TS */
    );

    final public static TokenSet DEFAULT_VALUES = TokenSet.create(
            PhpElementTypes.CONSTANT_REF,
            PhpElementTypes.STRING,
            PhpElementTypes.NUMBER,
            PhpElementTypes.CLASS_CONSTANT_REFERENCE,
            PhpElementTypes.ARRAY_CREATION_EXPRESSION
    );

    static public boolean isLambda(@Nullable PsiElement expression) {
        if (is(expression, PhpElementTypes.CLOSURE)) {
            expression = expression.getFirstChild();
        }
        return expression instanceof Function && ((Function) expression).isClosure();
    }

    static public boolean isAssignment(@Nullable PsiElement expression) {
        boolean result = expression != null && expression.getNode().getElementType() == PhpElementTypes.ASSIGNMENT_EXPRESSION;
        if (result) {
            final AssignmentExpression assignment = (AssignmentExpression) expression;
            final PsiElement container            = assignment.getVariable();
            final PsiElement value                = assignment.getValue();
            if (container != null && value != null) {
                PsiElement current = container;
                while (current != null && current != value) {
                    if (result = (current.getNode().getElementType() == PhpTokenTypes.opASGN)) {
                        break;
                    }
                    current = current.getNextSibling();
                }
            }
        }
        return result;
    }

    static public boolean isFunctionReference(@Nullable PsiElement expression) {
        return expression != null && expression.getNode().getElementType() == PhpElementTypes.FUNCTION_CALL;
    }

    static public boolean isLoop(@Nullable PsiElement expression) {
        return expression != null &&
            (expression instanceof ForeachStatement || expression instanceof For || expression instanceof While || expression instanceof DoWhile);
    }

    static public boolean isStatementImpl(@Nullable PsiElement expression) {
        return expression != null && expression.getNode().getElementType() == PhpElementTypes.STATEMENT;
    }

    static public boolean isPhpExpressionImpl(@Nullable PsiElement expression) {
        return expression != null && expression.getNode().getElementType() == PhpElementTypes.EXPRESSION;
    }

    static public boolean isNumber(@Nullable PsiElement expression) {
        return expression != null && expression.getNode().getElementType() == PhpElementTypes.NUMBER;
    }

    static public boolean is(@Nullable PsiElement expression, @NotNull IElementType type) {
        return expression != null && expression.getNode().getElementType() == type;
    }
}
