import re

class Version:
	def __init__(self, version):
		self.fields = [int(s) for s in version.split('.')]
	
	def __str__(self):
		return '.'.join(str(f) for f in self.fields)
	
class Range:
	def __init__(self, range):
		if range.startswith('^'):
			self.constraint, self.version = 'major', Version(range[1:])
		elif range.startswith('~'):
			self.constraint, self.version  = 'minor', Version(range[1:])
		elif range.startswith('<='):
			self.constraint, self.version  = 'lte', Version(range[2:])
		elif range.startswith('>='):
			self.constraint, self.version  = 'gte', Version(range[2:])
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
		elif self.constraint == 'lte':
			return '<=' + str(self.version)
		elif self.constraint == 'gte':
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
	v = Version('1.0.0')
	print(v)
	t = Term('root 1.0.0')
	print(t)
	t = Term('not foo ^1.0.0 || ^2.0.0')
	print(t)
