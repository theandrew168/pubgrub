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
            self.constraint, self.version = Constraint.MAJOR, Version(range[1:])
        elif range.startswith("~"):
            self.constraint, self.version = Constraint.MINOR, Version(range[1:])
        elif range.startswith("<="):
            self.constraint, self.version = Constraint.LESS_THAN_OR_EQUAL, Version(
                range[2:]
            )
        elif range.startswith(">="):
            self.constraint, self.version = Constraint.GREATER_THAN_OR_EQUAL, Version(
                range[2:]
            )
        elif range.startswith("<"):
            self.constraint, self.version = Constraint.LESS_THAN, Version(range[1:])
        elif range.startswith(">"):
            self.constraint, self.version = Constraint.GREATER_THAN, Version(range[1:])
        else:
            self.constraint, self.version = Constraint.EXACT, Version(range)

    def __str__(self):
        if self.constraint == Constraint.MAJOR:
            return "^" + str(self.version)
        elif self.constraint == Constraint.MINOR:
            return "~" + str(self.version)
        elif self.constraint == Constraint.LESS_THAN_OR_EQUAL:
            return "<=" + str(self.version)
        elif self.constraint == Constraint.GREATER_THAN_OR_EQUAL:
            return ">=" + str(self.version)
        elif self.constraint == Constraint.LESS_THAN:
            return "<" + str(self.version)
        elif self.constraint == Constraint.GREATER_THAN:
            return ">" + str(self.version)
        else:
            return str(self.version)

    def __contains__(self, version):
        version = Version(version)
        if self.constraint == Constraint.MAJOR:
            clauses = [
                version >= self.version,
                version[0] == self.version[0],
            ]
            return all(clauses)
        elif self.constraint == Constraint.MINOR:
            clauses = [
                version >= self.version,
                version[0] == self.version[0],
                version[1] == self.version[1],
            ]
            return all(clauses)
        elif self.constraint == Constraint.LESS_THAN_OR_EQUAL:
            return version <= self.version
        elif self.constraint == Constraint.GREATER_THAN_OR_EQUAL:
            return version >= self.version
        elif self.constraint == Constraint.LESS_THAN:
            return version < self.version
        elif self.constraint == Constraint.GREATER_THAN:
            return version > self.version
        else:
            return version == self.version


class Relation(enum.Enum):
    DISJOINT = enum.auto()
    SUBSET = enum.auto()
    OVERLAPPING = enum.auto()


# satisfies    -> subset
# satisfied by -> includes
#
# Term operations:
# 1. Relation between terms (disjoint, subset, overlapping)
#    - https://github.com/dart-lang/pub/blob/85aeff4c2f28ce55ac5cbf7be251c009e3829e30/lib/src/solver/term.dart#L45
# 2. Intersection of two terms
#    - https://github.com/dart-lang/pub/blob/85aeff4c2f28ce55ac5cbf7be251c009e3829e30/lib/src/solver/term.dart#L118
# 3. Difference of two terms
#    - https://github.com/dart-lang/pub/blob/85aeff4c2f28ce55ac5cbf7be251c009e3829e30/lib/src/solver/term.dart#L161
# 4. Allows any
# 5. Allows all
class Term:
    def __init__(self, term):
        fields = re.split(r"[\s|]+", term)

        self.is_positive = True
        if fields[0] == "not":
            self.is_positive = False
            self.package, self.range = fields[1], Range(fields[2])
        else:
            self.package, self.range = fields[0], Range(fields[1])

    @classmethod
    def from_package_and_version(cls, package, version):
        return cls("{} {}".format(package, version))

    def __str__(self):
        term = "{} {}".format(self.package, self.range)
        if not self.is_positive:
            term = "not " + term
        return term

    def negate(self):
        self.is_positive = False
        return self

    def allows_all(self, other):
        pass

    def allows_any(self, other):
        pass

    def relation(self, other):
        pass

    def intersection(self, other):
        pass

    def difference(self, other):
        pass

    # True if this is a subset of other.
    def satisfies(self, other):
        return self.package == other.package and self.relation(other) == Relation.SUBSET


class Incompatibility:
    def __init__(self, terms):
        self.terms = [Term(term) for term in terms]
        self.parents = []

    def __str__(self):
        return "{" + ", ".join(str(term) for term in self.terms) + "}"

    # True if this incompatibility refers to a given package.
    def __contains__(self, package):
        return any(package == term.package for term in self.terms)

    def unsatisfied_terms(self, version):
        return [term for term in self.terms if version not in term]

    # TODO: This should work for other terms (including ranges)
    # We say that a set of terms S satisfies an incompatibility I if S satisfies every term in I.
    def satisfies(self, version):
        return all(version in term for term in self.terms)

    def contradicts(self, version):
        return all(version not in term for term in self.terms)

    def inconclusive(self, version):
        return not self.satisfies(version) and not self.contradicts(self, version)


class Category(enum.Enum):
    DECISION = enum.auto()
    DERIVATION = enum.auto()


class Assignment:
    def __init__(self, term, category):
        self.term = Term(term)
        self.category = category
        # cause is an incompat
        self.cause = None
        self.level = 0

    def __str__(self):
        category = "DECISION" if self.category == Category.DECISION else "DERIVATION"
        return "{} - {}".format(category, str(self.term))


class PartialSolution:
    def __init__(self):
        self.solution = []

    def add_decision(self, term):
        assignment = Assignment(term, Category.DECISION)
        self.solution.append(assignment)

    def add_derivation(self, term):
        assignment = Assignment(term, Category.DERIVATION)
        self.solution.append(assignment)

    def relation(self, term):
        pass

    # If a partial solution has, for every positive derivation,
    # a corresponding decision that satisfies that assignment,
    # it's a total solution and version solving has succeeded.
    def is_solved(self):
        pass


class Registry:
    def __init__(self, packages):
        self.packages = packages

    def package_versions(self, package):
        return self.packages[package].keys()

    def package_version_dependencies(self, package, version):
        return self.packages[package][version]


class Solver:
    def __init__(self, registry):
        self.registry = registry
        self.solution = PartialSolution()
        self.incompatibilities = []

    def solve(self, package, version):
        root = Term.from_package_and_version(package, version)
        self.incompatibilities.append(root.negate())

        next = package
        while True:
            self._unit_propagation(next)
            next = self._decision_making(next)
            break

    def _unit_propagation(self, next):
        changed = {next}
        while changed:
            package = changed.pop()
            incompatibilities = [
                incompatibility
                for incompatibility in self.incompatibilities
                if package in incompatibility
            ]
            incompatibilities = reversed(incompatibilities)
            for incompatibility in incompatibilities:
                unsatisfied = None
                for term in incompatibility.terms:
                    relation = self.solution.relation(incompatibility)
                    if relation == Relation.DISJOINT:
                        break
                    elif relation == Relation.OVERLAPPING:
                        if unsatisfied is not None:
                            break
                        unsatisfied = term

                if unsatisfied is None:
                    raise Exception("conflict resolution")

                self.solution.add_derivation(unsatisfied)
                changed.add(unsatisfied.package)

    def _decision_making(next):
        pass


# if __name__ == '__main__':
# 	packages = {
# 		'root': {
# 			'1.0.0': {
# 				'foo': '^1.0.0',
# 			},
# 		},
# 		'foo': {
# 			'1.0.0': {
# 				'bar': '^1.0.0',
# 			},
# 		},
# 		'bar': {
# 			'1.0.0': {},
# 			'2.0.0': {},
# 		},
# 	}
# 	registry = Registry(packages)
# 	print(registry.package_versions('root'))
# 	print(registry.package_version_dependencies('root', '1.0.0'))


if __name__ == "__main__":
    v1 = Version("1.0.0")
    print(v1)
    v2 = Version("1.0")
    print(v2)
    v3 = Version("1.0.1")

    print("v1 == v2", v1 == v2)
    print("v1 <= v2", v1 <= v2)
    print("v1 >= v2", v1 >= v2)
    print("v1 < v2", v1 < v2)
    print("v1 > v2", v1 > v2)

    print("v1 == v3", v1 == v3)
    print("v1 <= v3", v1 <= v3)
    print("v1 >= v3", v1 >= v3)
    print("v1 < v3", v1 < v3)
    print("v1 > v3", v1 > v3)

    r1 = Range("^1.0.0")
    print("1.0.0 in ^1.0.0", "1.0.0" in r1)
    print("2.0.0 in ^1.0.0", "2.0.0" in r1)
    print("1.5.0 in ^1.0.0", "1.5.0" in r1)

    t1 = Term("root 1.0.0")
    print(t1)
    t2 = Term("foo ^1.0.0 || ^2.0.0")
    print(t2)
    print("1.0.0 in ^1.0.0 || ^2.0.0", "1.0.0" in t2)
    print("2.0.0 in ^1.0.0 || ^2.0.0", "2.0.0" in t2)
    print("3.0.0 in ^1.0.0 || ^2.0.0", "3.0.0" in t2)

    i1 = Incompatibility(["root 1.0.0", "not foo ^1.0.0"])
    print(i1)

    a1 = Assignment("root 1.0.0", "decision")
    print(a1)
