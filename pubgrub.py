import copy
import re


class Version:
	def __init__(self, version):
		self.fields = [int(s) for s in version.split('.')]

	def __str__(self):
		return '.'.join(str(f) for f in self.fields)

	@staticmethod
	def extend(a, b):
		n = max(len(a), len(b))
		if len(a) < n:
			a += [0] * (n - len(a))
		if len(b) < n:
			b += [0] * (n - len(b))
		return a, b

	def __getitem__(self, key):
		return self.fields[key]

	def __eq__(self, other):
		a = copy.deepcopy(self.fields)
		b = copy.deepcopy(other.fields)
		a, b = Version.extend(a, b)
		return a == b

	def __lt__(self, other):
		a = copy.deepcopy(self.fields)
		b = copy.deepcopy(other.fields)
		a, b = Version.extend(a, b)
		for aa, bb in zip(a, b):
			if aa < bb:
				return True
			if aa > bb:
				return False
		return False

	def __le__(self, other):
		a = copy.deepcopy(self.fields)
		b = copy.deepcopy(other.fields)
		a, b = Version.extend(a, b)
		for aa, bb in zip(a, b):
			if aa < bb:
				return True
			if aa > bb:
				return False
		return True

	def __gt__(self, other):
		a = copy.deepcopy(self.fields)
		b = copy.deepcopy(other.fields)
		a, b = Version.extend(a, b)
		for aa, bb in zip(a, b):
			if aa > bb:
				return True
			if aa < bb:
				return False
		return False

	def __ge__(self, other):
		a = copy.deepcopy(self.fields)
		b = copy.deepcopy(other.fields)
		a, b = Version.extend(a, b)
		for aa, bb in zip(a, b):
			if aa > bb:
				return True
			if aa < bb:
				return False
		return True


class Range:
	def __init__(self, range):
		if range.startswith('^'):
			self.constraint, self.version = 'major', Version(range[1:])
		elif range.startswith('~'):
			self.constraint, self.version  = 'minor', Version(range[1:])
		elif range.startswith('<='):
			self.constraint, self.version  = 'le', Version(range[2:])
		elif range.startswith('>='):
			self.constraint, self.version  = 'ge', Version(range[2:])
		elif range.startswith('<'):
			self.constraint, self.version  = 'lt', Version(range[1:])
		elif range.startswith('>'):
			self.constraint, self.version  = 'gt', Version(range[1:])
		else:
			self.constraint, self.version  = 'exact', Version(range)

	def __str__(self):
		if self.constraint == 'major':
			return '^' + str(self.version)
		elif self.constraint == 'minor':
			return '~' + str(self.version)
		elif self.constraint == 'le':
			return '<=' + str(self.version)
		elif self.constraint == 'ge':
			return '>=' + str(self.version)
		elif self.constraint == 'lt':
			return '<' + str(self.version)
		elif self.constraint == 'gt':
			return '>' + str(self.version)
		else:
			return str(self.version)

	def __contains__(self, version):
		version = Version(version)
		if self.constraint == 'major':
			clauses = [
				version >= self.version,
				version[0] == self.version[0],
			]
			return all(clauses)
		elif self.constraint == 'minor':
			clauses = [
				version >= self.version,
				version[0] == self.version[0],
				version[1] == self.version[1],
			]
			return all(clauses)
		elif self.constraint == 'le':
			return version <= self.version
		elif self.constraint == 'ge':
			return version >= self.version
		elif self.constraint == 'lt':
			return version < self.version
		elif self.constraint == 'gt':
			return version > self.version
		else:
			return version == self.version


class Term:
	def __init__(self, term):
		fields = re.split(r'[\s|]+', term)

		self.is_positive = True
		if fields[0] == 'not':
			self.is_positive = False
			self.package, self.ranges = fields[1], [Range(range) for range in fields[2:]]
		else:
			self.package, self.ranges = fields[0], [Range(range) for range in fields[1:]]

	@classmethod
	def from_package_and_version(cls, package, version):
		return cls('{} {}'.format(package, version))

	def __str__(self):
		term = '{} {}'.format(self.package, ' || '.join(str(range) for range in self.ranges))
		if not self.is_positive:
			term = 'not ' + term
		return term

	def __contains__(self, version):
		return any(version in range for range in self.ranges)

	def negate(self):
		self.is_positive = False
		return self


class Incompatibility:
	def __init__(self, terms):
		self.terms = [Term(term) for term in terms]
		self.parents = []

	def __str__(self):
		return '{' + ', '.join(str(term) for term in self.terms) + '}'

	def __contains__(self, package):
		return any(package == term.package for term in self.terms)

	def unsatisfied_terms(self, version):
		return [term for term in self.terms if version not in term]

	def satisfies(self, version):
		return all(version in term for term in self.terms)

	def contradicts(self, version):
		return all(version not in term for term in self.terms)

	def inconclusive(self, version):
		return not self.satisfies(version) and not self.contradicts(self, version)


class Registry:
	def __init__(self, packages):
		self.packages = packages

	def package_versions(self, package):
		return self.packages[package].keys()

	def package_version_dependencies(self, package, version):
		return self.packages[package][version]


def solve(registry, package, version):
	root = Term.from_package_and_version(package, version)
	solution = []
	incompats = [root.negate()]
	next = package
	while True:
		unit_propagation(solution, incompats, next)
		next = decision_making(registry, solution, incompats, next)
		break


def unit_propagation(solution, incompats, next):
	pass
	# changed = {next}
	# while changed:
	# 	package = changed.pop()
	# 	references = [incompat for incompat in incompats if package in incompat]
	# 	references = reversed(references)
	# 	for incompat in references:
	# 		if incompat.satisfies()


def decision_making(registry, solution, incompats, next):
	pass


if __name__ == '__main__':
	packages = {
		'root': {
			'1.0.0': {
				'foo': '^1.0.0',
			},
		},
		'foo': {
			'1.0.0': {
				'bar': '^1.0.0',
			},
		},
		'bar': {
			'1.0.0': {},
			'2.0.0': {},
		},
	}
	registry = Registry(packages)
	print(registry.package_versions('root'))
	print(registry.package_version_dependencies('root', '1.0.0'))


# if __name__ == '__main__':
# 	v1 = Version('1.0.0')
# 	print(v1)
# 	v2 = Version('1.0')
# 	print(v2)
# 	v3 = Version('1.0.1')

# 	print('v1 == v2', v1 == v2)
# 	print('v1 <= v2', v1 <= v2)
# 	print('v1 >= v2', v1 >= v2)
# 	print('v1 < v2', v1 < v2)
# 	print('v1 > v2', v1 > v2)

# 	print('v1 == v3', v1 == v3)
# 	print('v1 <= v3', v1 <= v3)
# 	print('v1 >= v3', v1 >= v3)
# 	print('v1 < v3', v1 < v3)
# 	print('v1 > v3', v1 > v3)

# 	r1 = Range('^1.0.0')
# 	print('1.0.0 in ^1.0.0', '1.0.0' in r1)
# 	print('2.0.0 in ^1.0.0', '2.0.0' in r1)
# 	print('1.5.0 in ^1.0.0', '1.5.0' in r1)

# 	t1 = Term('root 1.0.0')
# 	print(t1)
# 	t2 = Term('foo ^1.0.0 || ^2.0.0')
# 	print(t2)
# 	print('1.0.0 in ^1.0.0 || ^2.0.0', '1.0.0' in t2)
# 	print('2.0.0 in ^1.0.0 || ^2.0.0', '2.0.0' in t2)
# 	print('3.0.0 in ^1.0.0 || ^2.0.0', '3.0.0' in t2)

# 	i1 = Incompatibility(['root 1.0.0', 'not foo ^1.0.0'])
# 	print(i1)
