# obdd-gen

obdd-gen is a too to generate Ordered Binary Decision Diagrams or (O)BDDs from boolean logic formulas. Additionally, obdd-gen includes an experimental tool for LUT-mapping based on QRBDDs.

## Building

To build an executable jar, run
`gradle assemble && gradle shadowJar`. The executable will be located in `build/libs/`.  

## BDD / QRBDD Generation

Builds a BDD from the given boolean formula, producing a DOT graph or JSON representation of the BDD.

```
java -jar obdd-gen.jar [flags] [formula]
Flags:
--naive         Use naive BDD generation (slow, bad)
--reduce        Produce fully reduced BDD
--quasireduce   Produce quasi-reduced BDD
--json          Output JSON rather than DOT
--out=[path]    Output location, default bdd.dot/bdd.json
--order=[order] Specify variable evaluation order
        none        Use variables in order of appearance
        weight      Use variable weight heuristic
        count       Use variables in order of global count
        subgraph    Use subgraph complexity heuristic
        a,b,c       Use custom variable order
```

## LUT Mapping

LUT mapping maps a BLIF input file to an equivalent BLIF output that is composed only of 5-input (or smaller) look-up tables, optimizing for the smallest number of LUTs.

**Note:** The BLIF parser currently only supports a small subset of BLIF. Inputs should be plain networks of logic gates using ON-set specification. Generic latches are somewhat supported.

```
java -jar obdd-gen.jar --blif-map [flags] [input.blif]
Flags:
--loglevel=[0-5]    Set verbosity 5=silent 0=full default 4
--out=[path]        Output location, default mapped.blif
```
