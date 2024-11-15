(ns pubgrub.core
  (:require
   [pubgrub.fake :as fake]
   [pubgrub.registry :as registry]
   [pubgrub.term :as term]))

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
;;;; "decision making", a solution is hopefully found (complete, partial, or none). The
;;;; main algorithm is categorized as "Conflict-driven clause learning" which relates to
;;;; boolean satisfiability.

;; TODO: Create func for generating the initial set of incompats.
;; TODO: Find a better way of details with separate package/version and term parsing.

(defn- choose-next-package
  "Decide which package to choose next: the one with the fewest versions."
  [reg]
  "TODO")

(defn- choose-next-package-version
  "Decide which package version to choose next: the highest compatible version."
  [reg package]
  "TODO")

(defn- make-incompatibility
  [terms parents]
  {:terms (set terms)
   :parents parents})

(defn- incompatibility-terms
  [i]
  (i :terms))

(defn- incompatibility-parents
  [i]
  (i :parents))

(defn- make-assignment
  [term category cause level]
  {:term term
   :category category
   :cause cause
   :level level})

(defn- assignment-term
  [a]
  (a :term))

(defn- assignment-category
  [a]
  (a :category))

(defn- assignment-cause
  [a]
  (a :cause))

(defn- assignment-level
  [a]
  (a :level))

(defn- add-dep
  "Given a package and a dependency, add both terms to an incompat with the dep negated."
  [package version dep range]
  (make-incompatibility
   [(term/parse (format "%s %s" package version))
    (assoc (term/parse (format "%s %s" dep range)) :positive? false)]
   []))

(defn- unit-propagation
  [solution incompatibilities next]
  (let [changed #{next}]
    ;; While "changed" isn't empty
    (while (count changed)
      ;; Remove an element from "changed", call it "package"
      (let [package (first changed)
            changed (disj changed package)]
        ;; Find all incompats that refer to package (newest to oldest)
        ;; For each incompat:
        ;;   If it is satisfied by the current partial solution
        ;;     Run conflict resolution
        ;;   If it is almost satisfied (all but one) by the current partial solution
        ;;     Add the incompat's unsat term to the solution (negated) w/ incompat as cause
        ;;     Add term's name to changed
        )))
  "TODO")

(defn- conflict-resolution
  [solution incompatibilities]
  "TODO")

(defn- decision-making
  [reg solution incompatibilities next]
  ;; Pick a package where:
  ;; 1. The partial solution contains a positive derivation for that package. 
  ;; 2. The partial solution doesn't contain a decision for that package. 
  ;; 3. The package version matches all assignments in the partial solution.
  ;; From these, choose the latest matching version of the package with
  ;; the fewest total version (this is what pub does).
  ;; Then, add the package's deps to the list of incompats.
  ;; Optimization: collapse same deps from adjacent package versions 
  "TODO")

;; https://github.com/dart-lang/pub/blob/master/doc/solver.md#the-algorithm
(defn solve
  "Do the pubgrub thing!"
  [reg package version]
  (let [solution []
        incompatibilities [(make-incompatibility [(term/negate (term/parse (format "%s %s" package version)))] [])]
        next package]
    (unit-propagation solution incompatibilities next)
    (decision-making reg solution incompatibilities next)
    ;; derive top-level incompats
    ;;   for each dep, add "{root v, not dep v}"
    ;; unit propagation
    ;;   if conflict, try to resolve
    ;;   if resolution fails, solving has failed (report error)
    ;; decision making
    ;;   if no more work to do, solved!
    ;; repeat til solved or conflict
    {:solution solution
     :incompatibilities incompatibilities
     :next next}))

(comment

  (concat [1 2 3] (take 2 (repeat 0)))
  (take 4 (repeat 0))
  (conj [1 2 3] 0)
  (count [1 2 3])

  (first #{1 2 3})
  (disj #{1 2 3} 1)

  ;; https://github.com/dart-lang/pub/blob/master/doc/solver.md#no-conflicts
  (def example1
    {"root" {"1.0.0" {"foo" "^1.0.0"}}
     "foo" {"1.0.0" {"bar" "^1.0.0"}}
     "bar" {"1.0.0" {}
            "2.0.0" {}}})

  (def reg1 (fake/new-registry example1))
  (solve reg1 "root" "1.0.0")

  ;; https://github.com/dart-lang/pub/blob/master/doc/solver.md#avoiding-conflict-during-decision-making
  (def example2
    {"root" {"1.0.0" {"foo" "^1.0.0"
                      "bar" "^1.0.0"}}
     "foo" {"1.0.0" {}
            "1.1.0" {"bar" "^2.0.0"}}
     "bar" {"1.0.0" {}
            "1.1.0" {}
            "2.0.0" {}}})

  (def reg2 (fake/new-registry example2))
  (solve reg2 "root" "1.0.0")

  :rcf)

;;;; Larger Example:
;;;; tar 7.4.3 (https://registry.npmjs.org/tar/7.4.3)
;;;;   @isaacs/fs-minipass ^4.0.0 (4.0.1)
;;;;     minipass ^7.0.4 (7.1.2)
;;;;   chownr ^3.0.0 (3.0.0)
;;;;   minipass ^7.1.2 (7.1.2)
;;;;   minizlib ^3.0.1 (3.0.1)
;;;;     minipass ^7.0.4 (7.1.2)
;;;;     rimraf ^5.0.5 (5.0.10, latest is 6.0.1)
;;;;       glob ^10.3.7 (10.4.5, latest is 11.0.0)
;;;;         minipass ^7.1.2 (7.1.2)
;;;;         jackspeak ^3.1.2 (3.4.3, latest is 4.0.2)
;;;;           @isaacs/cliui ^8.0.2 (8.0.2)
;;;;             string-width ^5.1.2 (5.1.2, latest is 7.2.0)
;;;;               strip-ansi ^7.0.1 (7.1.0)
;;;;                 ansi-regex ^6.0.1 (6.1.0)
;;;;               emoji-regex ^9.2.2 (9.2.2, latest is 10.4.0)
;;;;               eastasianwidth ^0.2.0 (0.3.0)
;;;;             strip-ansi ^7.0.1 (7.1.0)
;;;;               ansi-regex ^6.0.1 (6.1.0)
;;;;             wrap-ansi ^8.1.0 (8.1.0, latest is 9.0.0)
;;;;               ansi-styles ^6.1.0 (6.2.1)
;;;;               string-width ^5.0.1 (5.1.2, latest is 7.2.0)
;;;;                 strip-ansi ^7.0.1 (7.1.0)
;;;;                   ansi-regex ^6.0.1 (6.1.0)
;;;;                 emoji-regex ^9.2.2 (9.2.2, latest is 10.4.0)
;;;;                 eastasianwidth ^0.2.0 (0.3.0)
;;;;               strip-ansi ^7.0.1 (7.1.0)
;;;;                 ansi-regex ^6.0.1 (6.1.0)
;;;;         minimatch ^9.0.4 (9.0.5, latest is 10.0.1)
;;;;           brace-expansion ^2.0.1 (2.0.1, latest is 4.0.0)
;;;;             balanced-match ^1.0.0 (1.0.2, latest is 3.0.1)
;;;;         path-scurry ^1.11.1 (1.11.1, latest is 2.0.0)
;;;;           minipass ^5.0.0 || ^6.0.2 || ^7.0.0 (7.1.2)
;;;;           lru-cache ^10.2.0 (10.4.3, latest is 11.0.2)
;;;;         foreground-child ^3.1.0 (3.3.0)
;;;;           cross-spawn ^7.0.0 (7.0.5)
;;;;             path-key ^3.1.0 (3.1.1, latest is 4.0.0)
;;;;             shebang-command ^2.0.0 (2.0.0)
;;;;               shebang-regex ^3.0.0 (3.0.0, latest is 4.0.0)
;;;;             which ^2.0.1 (2.0.2, latest is 5.0.0)
;;;;               isexe ^2.0.0 (2.0.0, latest is 3.1.1)
;;;;           signal-exit ^4.0.1 (4.1.0)
;;;;         package-json-from-dist ^1.0.0 (1.0.1)
;;;;   mkdirp ^3.0.1 (3.0.1)
;;;;   yallist ^5.0.0 (5.0.0)
