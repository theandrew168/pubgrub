import unittest

import pubgrub


class TestVersion(unittest.TestCase):

    def test_init(self):
        v1 = pubgrub.Version("1.2.3")
        self.assertEqual(v1[0], 1)
        self.assertEqual(v1[1], 2)
        self.assertEqual(v1[2], 3)

    def test_str(self):
        v1 = pubgrub.Version("1.2.3")
        self.assertEqual(str(v1), "1.2.3")

    def test_equal(self):
        v1 = pubgrub.Version("1.2.3")
        v2 = pubgrub.Version("1.2.3")
        v3 = pubgrub.Version("2.3.4")
        self.assertEqual(v1, v2)
        self.assertNotEqual(v1, v3)


if __name__ == "__main__":
    unittest.main()
