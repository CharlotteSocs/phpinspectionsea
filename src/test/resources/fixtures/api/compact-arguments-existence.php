<?php

function buggyCompact($x, $y) {
    $z = $x + $y;
    return
        <error descr="$zz might not be defined in the scope.">compact</error> ('x', 'y', 'z', 'zz', 'zz', "$$z")
        +
        <error descr="$$zz might not be defined in the scope.">compact</error>('x', 'y', 'z', '$zz', '$zz', "$$z")
        +
        compact(<weak_warning descr="There is chance that it should be 'z' here.">$z</weak_warning>)
    ;
}

function buggyCompactControlFlow($x, $y) {
    $temp = <error descr="$z might not be defined in the scope.">compact</error>('x', 'y', 'z');
    $z    = $x + $y;

    return $temp;
}