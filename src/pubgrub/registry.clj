(ns pubgrub.registry)

(defprotocol Registry
  "Represents a generic package registry."
  (package-versions [this package]
    "Get all versions (sequence of strings) of a package.")
  (package-version-dependencies [this package version]
    "Get all dependencies (map of string to string, name to term) of a specific package version."))
