from collections import defaultdict
import copy
import enum
import re


class Version:
    def __init__(self, version):
        self._fields = [int(s) for s in version.split(".")]

    def __str__(self):
        return ".".join(str(f) for f in self._fields)

    @staticmethod
    def _extend(a, b):
        n = max(len(a), len(b))
        if len(a) < n:
            a += [0] * (n - len(a))
        if len(b) < n:
            b += [0] * (n - len(b))
        return a, b

    def __getitem__(self, key):
        return self._fields[key]

    def __eq__(self, other):
        a = copy.deepcopy(self._fields)
        b = copy.deepcopy(other._fields)
        a, b = Version._extend(a, b)
        return a == b

    def __lt__(self, other):
        a = copy.deepcopy(self._fields)
        b = copy.deepcopy(other._fields)
        a, b = Version._extend(a, b)
        for aa, bb in zip(a, b):
            if aa < bb:
                return True
            if aa > bb:
                return False
        return False

    def __le__(self, other):
        a = copy.deepcopy(self._fields)
        b = copy.deepcopy(other._fields)
        a, b = Version._extend(a, b)
        for aa, bb in zip(a, b):
            if aa < bb:
                return True
            if aa > bb:
                return False
        return True

    def __gt__(self, other):
        a = copy.deepcopy(self._fields)
        b = copy.deepcopy(other._fields)
        a, b = Version._extend(a, b)
        for aa, bb in zip(a, b):
            if aa > bb:
                return True
            if aa < bb:
                return False
        return False

    def __ge__(self, other):
        a = copy.deepcopy(self._fields)
        b = copy.deepcopy(other._fields)
        a, b = Version._extend(a, b)
        for aa, bb in zip(a, b):
            if aa > bb:
                return True
            if aa < bb:
                return False
        return True


class Constraint(enum.Enum):
    MAJOR = enum.auto()
    MINOR = enum.auto()
    LESS_THAN_OR_EQUAL = enum.auto()
    GREATER_THAN_OR_EQUAL = enum.auto()
    LESS_THAN = enum.auto()
    GREATER_THAN = enum.auto()
    EXACT = enum.auto()


class Range:
    def __init__(self, range):
        if range.startswith("^"):
            self._constraint, self._version = Constraint.MAJOR, Version(range[1:])
        elif range.startswith("~"):
            self._constraint, self._version = Constraint.MINOR, Version(range[1:])
        elif range.startswith("<="):
            self._constraint, self._version = Constraint.LESS_THAN_OR_EQUAL, Version(
                range[2:]
            )
        elif range.startswith(">="):
            self._constraint, self._version = Constraint.GREATER_THAN_OR_EQUAL, Version(
                range[2:]
            )
        elif range.startswith("<"):
            self._constraint, self._version = Constraint.LESS_THAN, Version(range[1:])
        elif range.startswith(">"):
            self._constraint, self._version = Constraint.GREATER_THAN, Version(
                range[1:]
            )
        else:
            self._constraint, self._version = Constraint.EXACT, Version(range)

    @property
    def constraint(self):
        return self._constraint

    @property
    def version(self):
        return self._version

    def __str__(self):
        if self._constraint == Constraint.MAJOR:
            return "^" + str(self._version)
        elif self._constraint == Constraint.MINOR:
            return "~" + str(self._version)
        elif self._constraint == Constraint.LESS_THAN_OR_EQUAL:
            return "<=" + str(self._version)
        elif self._constraint == Constraint.GREATER_THAN_OR_EQUAL:
            return ">=" + str(self._version)
        elif self._constraint == Constraint.LESS_THAN:
            return "<" + str(self._version)
        elif self._constraint == Constraint.GREATER_THAN:
            return ">" + str(self._version)
        else:
            return str(self._version)

    def __contains__(self, version):
        version = Version(version)
        if self._constraint == Constraint.MAJOR:
            clauses = [
                version >= self._version,
                version[0] == self._version[0],
            ]
            return all(clauses)
        elif self._constraint == Constraint.MINOR:
            clauses = [
                version >= self._version,
                version[0] == self._version[0],
                version[1] == self._version[1],
            ]
            return all(clauses)
        elif self._constraint == Constraint.LESS_THAN_OR_EQUAL:
            return version <= self._version
        elif self._constraint == Constraint.GREATER_THAN_OR_EQUAL:
            return version >= self._version
        elif self._constraint == Constraint.LESS_THAN:
            return version < self._version
        elif self._constraint == Constraint.GREATER_THAN:
            return version > self._version
        else:
            return version == self._version


class Relation(enum.Enum):
    SUBSET = enum.auto()
    DISJOINT = enum.auto()
    OVERLAPPING = enum.auto()


class MismatchedPackageError(Exception):
    pass


class Term:
    def __init__(self, term):
        fields = re.split(r"[\s|]+", term)

        self._is_positive = True
        if fields[0] == "not":
            self._is_positive = False
            self._package, self._range = fields[1], Range(fields[2])
        else:
            self._package, self._range = fields[0], Range(fields[1])

    @classmethod
    def from_package_and_version(cls, package, version):
        return cls("{} {}".format(package, version))

    @property
    def is_positive(self):
        return self._is_positive

    @property
    def package(self):
        return self._package

    @property
    def range(self):
        return self._range

    def __str__(self):
        term = "{} {}".format(self._package, self._range)
        if not self._is_positive:
            term = "not " + term
        return term

    def inverse(self):
        new = Term(str(self))
        new._is_positive = not new._is_positive
        return new

    def intersect(self, other):
        if self.package != other.package:
            raise MismatchedPackageError

        raise NotImplementedError

    def union(self, other):
        if self.package != other.package:
            raise MismatchedPackageError

        raise NotImplementedError

    def difference(self, other):
        if self.package != other.package:
            raise MismatchedPackageError

        raise NotImplementedError

    # Relation between self and other: is self a subset of other?
    def relation(self, other):
        if self.package != other.package:
            raise MismatchedPackageError

        # 2x2 matrix of is_positive
        #   7x7 matrix of constraints
        #     this needs a clever solution: something with allows_all and allows_any (wrt ranges)

        # + self, + other
        if self.is_positive and other.is_positive:
            # EXACT, EXACT
            if (
                self.range.constraint == Constraint.EXACT
                and other.range.constraint == Constraint.EXACT
            ):
                if self.range.version == other.range.version:
                    return Relation.SUBSET
                else:
                    return Relation.DISJOINT
            else:
                raise NotImplementedError
        # + self, - other
        elif self.is_positive and not other.is_positive:
            # EXACT, EXACT
            if (
                self.range.constraint == Constraint.EXACT
                and other.range.constraint == Constraint.EXACT
            ):
                if self.range.version == other.range.version:
                    return Relation.DISJOINT
                else:
                    return Relation.SUBSET
            else:
                raise NotImplementedError
        # - self, + other
        elif not self.is_positive and other.is_positive:
            # EXACT, EXACT
            if (
                self.range.constraint == Constraint.EXACT
                and other.range.constraint == Constraint.EXACT
            ):
                if self.range.version == other.range.version:
                    return Relation.DISJOINT
                else:
                    return Relation.SUBSET
            else:
                raise NotImplementedError
        # - self, - other
        elif not self.is_positive and not other.is_positive:
            # EXACT, EXACT
            if (
                self.range.constraint == Constraint.EXACT
                and other.range.constraint == Constraint.EXACT
            ):
                if self.range.version == other.range.version:
                    return Relation.SUBSET
                else:
                    return Relation.DISJOINT
            else:
                raise NotImplementedError

    # Many ways to say the same thing:
    # 1. True if self implies other.
    # 2. True if self is a subset of other.
    # 3. True if self being true means other must also be true.
    # 4. True if other must be true whenever self is true.
    def satisfies(self, other):
        if self.package != other.package:
            raise MismatchedPackageError

        return self.relation(other) == Relation.SUBSET


class TermSet:
    def __init__(self, *terms):
        self._terms = [Term(term) for term in terms]

    @property
    def terms(self):
        return self._terms

    # We say that a set of terms S "satisfies" a term t if t must be true whenever every term in S is true.
    def satisfies(self, term):
        for t in self._terms:
            if not t.satisfies(term):
                return False
        return True


class Incompatibility:
    def __init__(self, *terms):
        self._terms = [Term(term) if type(term) is str else term for term in terms]

    @property
    def terms(self):
        return self._terms

    def __str__(self):
        return "{" + ", ".join(str(term) for term in self._terms) + "}"

    # True if this incompatibility refers to a given package.
    def __contains__(self, package):
        return any(package == term._package for term in self._terms)

    def __iter__(self):
        return iter(self._terms)

    # We say that a set of terms S satisfies an incompatibility I if S satisfies every term in I.
    def satisfies(self, termset):
        return all(termset.satisfies(term) for term in self._terms)


class Category(enum.Enum):
    DECISION = enum.auto()
    DERIVATION = enum.auto()


class Assignment:
    def __init__(self, term, category):
        self.term = Term(term) if type(term) is str else term
        self.category = category
        # cause is an incompat
        self.cause = None
        self.level = 0

    def __str__(self):
        category = "DECISION" if self.category == Category.DECISION else "DERIVATION"
        return "{} ({})".format(str(self.term), category)


class PartialSolution:
    def __init__(self):
        self._solution = []
        # Track positive and negative assignments by package (one per package).
        self._positive = {}
        self._negative = {}

    def __iter__(self):
        return iter(self._solution)

    def decide(self, term):
        assignment = Assignment(term, Category.DECISION)
        self._solution.append(assignment)

        # TODO: intersect pos, union neg assignments for the same package
        if term.is_positive:
            self._positive[term.package] = term
        else:
            self._negative[term.package] = term

    def derive(self, term):
        assignment = Assignment(term, Category.DERIVATION)
        self._solution.append(assignment)

        # TODO: intersect pos, union neg assignments for the same package
        if term.is_positive:
            self._positive[term.package] = term
        else:
            self._negative[term.package] = term

    def relation(self, term):
        # If we have a positive assign for the term, apply it.
        pos = self._positive.get(term.package)
        if pos:
            return pos.relation(term)

        # If we have a negative assign for the term, apply it.
        neg = self._negative.get(term.package)
        if neg:
            return neg.relation(term)

        # Otherwise, we haven't seen the package yet which means
        # we can accept any version of it.
        return Relation.OVERLAPPING

    def satisfies(self, term):
        return self.relation(term) == Relation.SUBSET

    # If a partial solution has, for every positive derivation,
    # a corresponding decision that satisfies that assignment,
    # it's a total solution and version solving has succeeded.
    def is_solved(self):
        pass


class Registry:
    def __init__(self, packages):
        self._packages = packages

    def package_versions(self, package):
        return self._packages[package].keys()

    def package_version_dependencies(self, package, version):
        return self._packages[package][version]


class Solver:
    def __init__(self, registry):
        self._registry = registry
        self._solution = PartialSolution()
        self._incompatibilities = []

    def debug(self):
        print("=== SOLUTION ===")
        for s in self._solution:
            print(s)

        print("=== INCOMPATS ===")
        for i in self._incompatibilities:
            print(i)

    def solve(self, package, version):
        root = Term.from_package_and_version(package, version)
        self._incompatibilities.append(Incompatibility(root.inverse()))

        next = package
        while next:
            self._unit_propagation(next)
            next = self._decision_making(next)

        return [a.term for a in self._solution if a.category == Category.DECISION]

    def _unit_propagation(self, next):
        changed = {next}
        while changed:
            package = changed.pop()
            incompatibilities = [
                incompatibility
                for incompatibility in self._incompatibilities
                if package in incompatibility
            ]
            incompatibilities = list(reversed(incompatibilities))
            print(
                "checking {} incompat(s) for: {}".format(
                    len(incompatibilities), package
                )
            )
            for incompatibility in incompatibilities:
                unsat = [
                    term
                    for term in incompatibility
                    if not self._solution.satisfies(term)
                ]
                if not unsat:
                    raise Exception("conflict resolution")

                if len(unsat) == 1:
                    term = unsat[0]
                    self._solution.derive(term.inverse())
                    # TODO: Why is this infinite looping?
                    # changed.add(term.package)

    def _check_almost_satisfies(self, incompatibility):
        unsat = [term for term in incompatibility if not self._solution.satisfies(term)]
        if not unsat:
            raise Exception("conflict resolution")

        if len(unsat) == 1:
            return unsat[0]

        return None

    def _decision_making(self, next):
        categories_by_package = defaultdict(list)
        for assignment in self._solution:
            categories_by_package[assignment.term.package].append(assignment.category)

        package = None
        for choice, categories in categories_by_package.items():
            if (
                Category.DERIVATION in categories
                and Category.DECISION not in categories
            ):
                package = choice
                break

        if package is None:
            return None

        version = None
        versions = self._registry.package_versions(package)
        for choice in versions:
            if self._solution.satisfies(Term.from_package_and_version(package, choice)):
                version = choice
                break

        term = Term.from_package_and_version(package, version)
        deps = self._registry.package_version_dependencies(package, version)
        for k, v in deps.items():
            incompat = Incompatibility(
                term, Term.from_package_and_version(k, v).inverse()
            )
            self._incompatibilities.append(incompat)

        self._solution.decide(term)
        return package

    def _conflict_resolution():
        pass


if __name__ == "__main__":
    packages = {
        "root": {
            "1.0.0": {
                "foo": "1.0.0",
            },
        },
        "foo": {
            "1.0.0": {
                "bar": "1.0.0",
            },
        },
        "bar": {
            "1.0.0": {},
            "2.0.0": {},
        },
    }
    registry = Registry(packages)
    # print(registry.package_versions("root"))
    # print(registry.package_version_dependencies("root", "1.0.0"))

    solver = Solver(registry)
    solution = solver.solve("root", "1.0.0")
    solver.debug()

    print("=== SOLUTION ===")
    for s in solution:
        print(s)


# if __name__ == "__main__":
#     v1 = Version("1.0.0")
#     print(v1)
#     v2 = Version("1.0")
#     print(v2)
#     v3 = Version("1.0.1")

#     print("v1 == v2", v1 == v2)
#     print("v1 <= v2", v1 <= v2)
#     print("v1 >= v2", v1 >= v2)
#     print("v1 < v2", v1 < v2)
#     print("v1 > v2", v1 > v2)

#     print("v1 == v3", v1 == v3)
#     print("v1 <= v3", v1 <= v3)
#     print("v1 >= v3", v1 >= v3)
#     print("v1 < v3", v1 < v3)
#     print("v1 > v3", v1 > v3)

#     r1 = Range("^1.0.0")
#     print("1.0.0 in ^1.0.0", "1.0.0" in r1)
#     print("2.0.0 in ^1.0.0", "2.0.0" in r1)
#     print("1.5.0 in ^1.0.0", "1.5.0" in r1)

#     t1 = Term("root 1.0.0")
#     print(t1)
#     t2 = Term("not foo ^1.0.0")
#     print(t2)

#     i1 = Incompatibility("root 1.0.0", "not foo ^1.0.0")
#     print(i1)

#     a1 = Assignment("root 1.0.0", Category.DECISION)
#     print(a1)
