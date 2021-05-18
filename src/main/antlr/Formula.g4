grammar Formula;

@header {
package obdd.gen;
}

// Ignore whitespace
WS : [ \t\r\n\u000C]+ -> skip;

LPAR : '(';
RPAR : ')';
NOT : '!';
AND : '&';
OR : '|';
XOR : '^';
IMPL: '->' | '=>';
EQUIV: '<->' | '<=>';
TRUE: '1' | 'true' | 'True';
FALSE: '0' | 'false' | 'False';

VAR : [a-zA-Z0-9\-_]+ ;

formula : LPAR formula RPAR       # nest_formula
		| NOT formula             # not_formula
        | formula AND formula     # and_formula
        | formula OR formula      # or_formula
        | formula XOR formula     # xor_formula
        | formula IMPL formula    # impl_formula
        | formula EQUIV formula   # equiv_formula
        | TRUE                    # true_formula
        | FALSE                   # false_formula
        | VAR                     # var_formula
        ;