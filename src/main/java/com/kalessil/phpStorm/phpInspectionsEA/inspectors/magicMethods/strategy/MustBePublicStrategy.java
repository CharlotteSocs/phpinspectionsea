package com.kalessil.phpStorm.phpInspectionsEA.inspectors.magicMethods.strategy;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import org.jetbrains.annotations.NotNull;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

final public class MustBePublicStrategy {
    private static final String messagePattern = "%s must be public.";

    static public void apply(@NotNull Method method, @NotNull ProblemsHolder holder) {
        if (!method.getAccess().isPublic()) {
            final PsiElement nameNode = method.getNameIdentifier();
            if (nameNode != null) {
                final String message = String.format(messagePattern, method.getName());
                holder.registerProblem(nameNode, message, ProblemHighlightType.ERROR);
            }
        }
    }
}
