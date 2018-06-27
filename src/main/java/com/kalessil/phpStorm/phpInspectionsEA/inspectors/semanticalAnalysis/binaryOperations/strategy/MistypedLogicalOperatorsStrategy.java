package com.kalessil.phpStorm.phpInspectionsEA.inspectors.semanticalAnalysis.binaryOperations.strategy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.BinaryExpression;
import org.jetbrains.annotations.NotNull;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

final public class MistypedLogicalOperatorsStrategy {
    private static final String messagePatternAnd = "It was probably was intended to use && here (if not, wrap into parentheses).";
    private static final String messagePatternOr  = "It was probably was intended to use || here (if not, wrap into parentheses).";

    public static boolean apply(@NotNull BinaryExpression expression, @NotNull ProblemsHolder holder) {
        boolean result              = false;
        final IElementType operator = expression.getOperationType();
        if (operator != null && (operator == PhpTokenTypes.opBIT_AND || operator == PhpTokenTypes.opBIT_OR)) {
            final PsiElement parent = expression.getParent();
            if (parent instanceof BinaryExpression) {
                final IElementType parentOperator = ((BinaryExpression) parent).getOperationType();
                if (parentOperator == PhpTokenTypes.opAND || parentOperator == PhpTokenTypes.opOR) {
                    final PsiElement target = expression.getOperation();
                    if (target != null) {
                        result = true;
                        holder.registerProblem(
                                target,
                                parentOperator == PhpTokenTypes.opAND ? messagePatternAnd : messagePatternOr
                        );
                    }
                }
            }
        }
        return result;
    }
}
