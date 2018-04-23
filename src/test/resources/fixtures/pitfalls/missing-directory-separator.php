<?php

class CasesHolder
{

    public function directoryConstant() {
        return [
            __DIR__ . <warning descr="Looks like a directory separator is missing here.">''</warning>,
            '' . __DIR__ . <warning descr="Looks like a directory separator is missing here.">''</warning>,
            __DIR__ . <warning descr="Looks like a directory separator is missing here.">''</warning> . '',

            /* valid cases */
            __DIR__ . DIRECTORY_SEPARATOR,
            __DIR__ . '/',
            __DIR__ . '\\',
            __DIR__ . ' ...',
        ];
    }

    public function dirnameFunction() {
        return [
            dirname(__DIR__) . <warning descr="Looks like a directory separator is missing here.">''</warning>,
            '' . dirname(__DIR__) . <warning descr="Looks like a directory separator is missing here.">''</warning>,
            dirname(__DIR__) . <warning descr="Looks like a directory separator is missing here.">''</warning> . '',

            /* valid cases */
            dirname(__DIR__) . DIRECTORY_SEPARATOR,
            dirname(__DIR__) . '/',
            dirname(__DIR__) . '\\',
            dirname(__DIR__) . ' ...',
        ];
    }

}