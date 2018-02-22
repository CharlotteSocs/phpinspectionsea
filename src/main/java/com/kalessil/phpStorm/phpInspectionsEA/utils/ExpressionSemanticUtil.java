package com.kalessil.phpStorm.phpInspectionsEA.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

final public class ExpressionSemanticUtil {

    public static boolean hasAlternativeBranches(@NotNull If ifStatement) {
        return (ifStatement.getElseBranch() != null || ifStatement.getElseIfBranches().length > 0);
    }

    /**
     * TODO: to re-check if API already has a method for this
     */
    @Nullable
    public static PhpExpression getReturnValue(@NotNull PhpReturn expression) {
        for (final PsiElement child : expression.getChildren()) {
            if (child instanceof PhpExpression) {
                return (PhpExpression) child;
            }
        }
        return null;
    }

    public static int countExpressionsInGroup(@NotNull GroupStatement groupStatement) {
        return (int) Stream.of(groupStatement.getChildren())
                .filter(statement -> statement instanceof PhpPsiElement)
                .filter(statement -> !(statement instanceof PhpDocType) && !(statement instanceof PhpDocComment))
                .count();
    }

    @Nullable
    public static PsiElement getLastStatement(@NotNull GroupStatement groupStatement) {
        PsiElement child = groupStatement.getLastChild();
        while (child != null) {
            if (child instanceof PhpPsiElement && !(child instanceof PhpDocComment)) {
                return child;
            }
            child = child.getPrevSibling();
        }
        return null;
    }

    @Nullable
    public static GroupStatement getGroupStatement(@NotNull PsiElement expression) {
        PsiElement child = expression.getLastChild();
        while (child != null) {
            if (child instanceof GroupStatement) {
                return (GroupStatement) child;
            }
            child = child.getPrevSibling();
        }
        return null;
    }

    @Nullable
    public static PsiElement getExpressionTroughParenthesis(@Nullable PsiElement expression) {
        PsiElement innerExpression = expression;
        while (innerExpression instanceof ParenthesizedExpression) {
            innerExpression = ((ParenthesizedExpression) innerExpression).getArgument();
        }
        return innerExpression;
    }

    @Nullable
    public static List<PsiElement> getConditions(@Nullable PsiElement objCondition, @Nullable IElementType[] arrOperationHolder) {
        /* get through unary and parenthesis wrappers */
        if (null != objCondition) {
            objCondition = ExpressionSemanticUtil.getExpressionTroughParenthesis(objCondition);
        }
        if (objCondition instanceof UnaryExpression) {
            objCondition = ExpressionSemanticUtil.getExpressionTroughParenthesis(
                    ((UnaryExpression) objCondition).getValue()
            );
        }
        if (null == objCondition) {
            return null;
        }

        /* init container */
        List<PsiElement> objPartsCollection = new ArrayList<>();

        /* return non-binary expressions, eg. callable execution */
        if (!(objCondition instanceof BinaryExpression)) {
            objPartsCollection.add(objCondition);
            return objPartsCollection;
        }


        /* check operation type and extract conditions */
        final IElementType operationType = ((BinaryExpression) objCondition).getOperationType();
        if (operationType != PhpTokenTypes.opOR && operationType != PhpTokenTypes.opAND) {
            /* binary expression, but not needed type => return it */
            objPartsCollection.add(objCondition);
            return objPartsCollection;
        }

        if (null != arrOperationHolder) {
            arrOperationHolder[0] = operationType;
        }

        return ExpressionSemanticUtil.getConditions((BinaryExpression) objCondition, operationType);
    }

    private static List<PsiElement> getConditions(@NotNull BinaryExpression expression, @NotNull IElementType operation) {
        final LinkedList<PsiElement> result = new LinkedList<>();

        /* right expression first */
        result.add(ExpressionSemanticUtil.getExpressionTroughParenthesis(expression.getRightOperand()));

        /* expand binary operation while it's a binary operation */
        PsiElement current = ExpressionSemanticUtil.getExpressionTroughParenthesis(expression.getLeftOperand());
        while (current instanceof BinaryExpression && ((BinaryExpression) current).getOperationType() == operation) {
            final BinaryExpression binary = (BinaryExpression) current;
            result.addFirst(ExpressionSemanticUtil.getExpressionTroughParenthesis(binary.getRightOperand()));
            current = ExpressionSemanticUtil.getExpressionTroughParenthesis(binary.getLeftOperand());
        }

        /* don't forget very first one */
        result.addFirst(current);

        return result.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Nullable
    public static Function getScope(@NotNull PsiElement expression) {
        PsiElement parent = expression.getParent();
        while (parent != null && !(parent instanceof PsiFile)) {
            if (parent instanceof Function) {
                return (Function) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    public static PsiElement getBlockScope(@NotNull PsiElement expression) {
        PsiElement parent = expression.getParent();
        while (parent != null && !(parent instanceof PsiFile)) {
            if (parent instanceof Function || parent instanceof PhpClass || parent instanceof PhpDocComment) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    public static List<Variable> getUseListVariables(@NotNull Function function) {
        for (final PsiElement child : function.getChildren()) {
            if (child instanceof PhpUseList) {
                return Stream.of(child.getChildren())
                        .filter(variableCandidate -> variableCandidate instanceof Variable)
                        .map(variableCandidate    -> (Variable) variableCandidate)
                        .collect(Collectors.toList());
            }
        }

        return null;
    }

    public static boolean isUsedAsLogicalOperand(@NotNull PsiElement expression) {
        final PsiElement parent = expression.getParent();
        if (parent instanceof If || parent instanceof ElseIf) {
            return true;
        }
        if (parent instanceof UnaryExpression) {
            return OpenapiTypesUtil.is(((UnaryExpression) parent).getOperation(), PhpTokenTypes.opNOT);
        }
        if (parent instanceof BinaryExpression) {
            final IElementType operation = ((BinaryExpression) parent).getOperationType();
            return PhpTokenTypes.tsSHORT_CIRCUIT_AND_OPS.contains(operation) ||
                   PhpTokenTypes.tsSHORT_CIRCUIT_OR_OPS.contains(operation);
        }
        if (parent instanceof TernaryExpression) {
            final TernaryExpression ternary = (TernaryExpression) parent;
            return !ternary.isShort() && expression == ternary.getCondition();
        }
        return false;
    }

    @Nullable
    public static StringLiteralExpression resolveAsStringLiteral(@Nullable PsiElement expression) {
        if (null == expression) {
            return null;
        }
        expression                     = ExpressionSemanticUtil.getExpressionTroughParenthesis(expression);
        StringLiteralExpression result = null;

        if (expression instanceof StringLiteralExpression) {
            result = (StringLiteralExpression) expression;
        } else if (expression instanceof FieldReference || expression instanceof ClassConstantReference) {
            final Field fieldOrConstant = (Field) OpenapiResolveUtil.resolveReference((MemberReference) expression);
            if (fieldOrConstant != null && fieldOrConstant.getDefaultValue() instanceof StringLiteralExpression) {
                result = (StringLiteralExpression) fieldOrConstant.getDefaultValue();
            }
        } else if (expression instanceof Variable) {
            final String variable = ((Variable) expression).getName();
            if (!variable.isEmpty()) {
                final Function scope = ExpressionSemanticUtil.getScope(expression);
                if (scope != null) {
                    final Set<StringLiteralExpression> matched         = new HashSet<>();
                    final Collection<AssignmentExpression> assignments = PsiTreeUtil.findChildrenOfType(scope, AssignmentExpression.class);
                    for (final AssignmentExpression assignment : assignments) {
                        final PhpPsiElement container = assignment.getVariable();
                        if (container instanceof Variable && variable.equals(container.getName())) {
                            final PsiElement value = assignment.getValue();
                            if (value instanceof StringLiteralExpression) {
                                matched.add((StringLiteralExpression) value);
                            }
                        }
                    }
                    assignments.clear();
                    if (matched.size() == 1) {
                        result = matched.iterator().next();
                    }
                    matched.clear();
                }
            }
        }

        return result;
    }
}
