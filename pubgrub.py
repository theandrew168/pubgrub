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


class Term:
	def __init__(self, term):
		fields = re.split(r'[\s|]+', term)

		self.is_positive = True
		if fields[0] == 'not':
			self.is_positive = False
			self.package, self.ranges = fields[1], [Range(range) for range in fields[2:]]
		else:
			self.package, self.ranges = fields[0], [Range(range) for range in fields[1:]]

	def __str__(self):
		term = '{} {}'.format(self.package, ' || '.join(str(range) for range in self.ranges))
		if not self.is_positive:
			term = 'not ' + term
		return term


if __name__ == '__main__':
	v1 = Version('1.0.0')
	print(v1)
	v2 = Version('1.0')
	print(v2)
	v3 = Version('1.0.1')

	print('v1 == v2', v1 == v2)
	print('v1 <= v2', v1 <= v2)
	print('v1 >= v2', v1 >= v2)
	print('v1 < v2', v1 < v2)
	print('v1 > v2', v1 > v2)

	print('v1 == v3', v1 == v3)
	print('v1 <= v3', v1 <= v3)
	print('v1 >= v3', v1 >= v3)
	print('v1 < v3', v1 < v3)
	print('v1 > v3', v1 > v3)

	t = Term('root 1.0.0')
	print(t)
	t = Term('not foo ^1.0.0 || ^2.0.0')
	print(t)
