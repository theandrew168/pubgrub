(ns pubgrub.fake
  (:require
   [pubgrub.registry :as registry]))

(deftype FakeRegistry [packages]
  registry/Registry
  (package-versions
    [_ package]
    (keys (packages package)))
  (package-version-dependencies
    [_ package version]
    (get-in packages [package version])))

(defn new-registry
  "Convenience function for creating a new Fake Registry.
   The 'packages' param is a nested mapping of package names
   to versions to dependencies (map of package to version range)."
  [packages]
  (FakeRegistry. packages))

(comment

  (def packages
    {"root" {"1.0.0" {"foo" "^1.0.0"}}
     "foo" {"1.0.0" {"bar" "^1.0.0"}}
     "bar" {"1.0.0" {}
            "2.0.0" {}}})

  (def reg (new-registry packages))
  (registry/package-versions reg "root")
  (registry/package-versions reg "bar")
  (registry/package-version-dependencies reg "root" "1.0.0")

  :rcf)
