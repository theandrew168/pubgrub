(ns pubgrub.core
  (:require
   [clojure.string :as str]
   [pubgrub.npm :as npm]
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

;;;; Example:
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

(defn deps!
  [reg package version]
  (println package version)
  (let [deps (registry/package-version-dependencies reg package version)]
    (doseq [[k v] deps]
      (deps! reg k (term/strip-constraint (get (str/split v #"\s+") 0))))))

(comment

  (concat [1 2 3] (take 2 (repeat 0)))
  (take 4 (repeat 0))
  (conj [1 2 3] 0)
  (count [1 2 3])

  (def reg (npm/new-registry))
  (deps! reg "tar" "7.4.3")

  :rcf)
