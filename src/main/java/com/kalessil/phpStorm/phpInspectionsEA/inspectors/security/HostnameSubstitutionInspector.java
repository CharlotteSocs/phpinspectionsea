package com.kalessil.phpStorm.phpInspectionsEA.inspectors.security;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.OpenapiTypesUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

public class HostnameSubstitutionInspector extends BasePhpInspection {
    private static final String messageGeneral = "The email generation can be compromised, consider introducing whitelists.";
    private static final String messageNaming  = "The domain here can be compromised, consider introducing whitelists.";

    private static final Pattern regexTargetNames;
    static {
        regexTargetNames = Pattern.compile(".*(domain|email|host).*", Pattern.CASE_INSENSITIVE);
    }

    @NotNull
    public String getShortName() {
        return "HostnameSubstitutionInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            @Override
            public void visitPhpArrayAccessExpression(@NotNull ArrayAccessExpression expression) {
                final PsiElement variable = expression.getValue();
                if (variable instanceof Variable && ((Variable) variable).getName().equals("_SERVER")) {
                    final ArrayIndex index = expression.getIndex();
                    final PsiElement key   = index == null ? null : index.getValue();
                    if (key instanceof StringLiteralExpression) {
                        final String attribute = ((StringLiteralExpression) key).getContents();
                        if (attribute.equals("SERVER_NAME") || attribute.equals("HTTP_HOST")) {
                            this.identifyContextAndDelegateInspection(expression);
                        }
                    }
                }
            }

            private void identifyContextAndDelegateInspection(@NotNull ArrayAccessExpression expression) {
                PsiElement parent = expression.getParent();
                while (parent != null && !(parent instanceof PsiFile)) {
                    if (parent instanceof Function || parent instanceof PhpClass) {
                        /* at function/method/class level we can stop */
                        break;
                    } else if (parent instanceof ConcatenationExpression){
                        this.inspectConcatenationContext((ConcatenationExpression) parent);
                        break;
                    } else if (OpenapiTypesUtil.isAssignment(parent)) {
                        this.inspectAssignmentContext(expression, (AssignmentExpression) parent);
                        break;
                    }
                    parent = parent.getParent();
                }
            }

            /* direct/decorated concatenation with "...@" */
            private void inspectConcatenationContext(@NotNull ConcatenationExpression context) {
                PsiElement left = context.getLeftOperand();
                if (left instanceof ConcatenationExpression) {
                    left = ((ConcatenationExpression) left).getRightOperand();
                }
                final PsiElement right = context.getRightOperand();
                if (right != null && left instanceof StringLiteralExpression) {
                    if (((StringLiteralExpression) left).getContents().endsWith("@")) {
                        holder.registerProblem(right, messageGeneral);
                    }
                }
            }

            private void inspectAssignmentContext(
                @NotNull ArrayAccessExpression expression,
                @NotNull AssignmentExpression context
            ) {
                final PsiElement storage = context.getVariable();
                if (storage instanceof FieldReference) {
                    /* fields processing is too complex, just report it when naming matches */
                    final String storageName = ((FieldReference) storage).getName();
                    if (!StringUtils.isEmpty(storageName)) {
                        final Matcher matcher = regexTargetNames.matcher(storageName);
                        if (matcher.matches()) {
                            holder.registerProblem(expression, messageNaming);
                        }
                    }
                } else if (storage instanceof Variable) {
                    /* variables can be processed in scope only */
                    final Function scope = ExpressionSemanticUtil.getScope(storage);
                    if (scope != null) {
                        final String variableName = ((Variable) storage).getName();
                        boolean reachedExpression = false;
                        for (final Variable candidate : PsiTreeUtil.findChildrenOfType(scope, Variable.class)) {
                            if (!reachedExpression) {
                                reachedExpression = candidate == storage;
                            } else if (candidate.getName().equals(variableName)) {
                                final PsiElement parent = candidate.getParent();
                                if (parent instanceof ConcatenationExpression) {
                                    this.inspectConcatenationContext((ConcatenationExpression) parent);
                                }
                            }
                        }
                    } else {
                        /* variables in global context processing is too complex, just report it when naming matches */
                        final String storageName = ((PhpNamedElement) storage).getName();
                        if (!storageName.isEmpty()) {
                            final Matcher matcher = regexTargetNames.matcher(storageName);
                            if (matcher.matches()) {
                                holder.registerProblem(expression, messageNaming);
                            }
                        }
                    }
                }
            }
        };
    }
}
