(ns pubgrub.core)

;;;; References:
;;;; https://nex3.medium.com/pubgrub-2fb6470504f
;;;; https://github.com/dart-lang/pub/blob/master/doc/solver.md

;;;; Let's start with semvers of any length (any number of segments separated by a period).
;;;; Ignore prereleases, release candiates, and things like that (for now). So, a version
;;;; could be something like "1.2" or "3.4.5". Once parsed, we can think of versions as a
;;;; sequence of integers. In this model, "1.2" becomes [1, 2] and "3.4.5" becomes [3, 4, 5].

;;;; The fundamental unit of the PubGrub solver is a "Term". A term consists of:
;;;; 1. A package name (required)
;;;; 2. A package version (required)
;;;; 3. A matching constraint (optional, default is exact)
;;;;    (exact, major (caret), minor (tilde), or range (inequality))
;;;; 4. A negation (optional)

;;;; Terms often come in sets. For a given set of a terms, it either "satisfies" (all
;;;; terms are true), "contradicts" (all terms are false), or is "inconclusive" (if neither
;;;; of the other cases are true) for another term.

;;;; In general, the algorithm deals with "incompatibilities": sets of terms that cannot
;;;; ALL be true. It builds a "derivation graph" (directed, acyclic, and binary) of these
;;;; incompatibilities. Through phases of "unit propagation", "conflict resolution", and
;;;; "decision making", a solution is hopefully found (complete, partial, or none).

(comment

  :rcf)
