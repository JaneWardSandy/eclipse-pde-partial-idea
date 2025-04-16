package cn.varsa.idea.pde.partial.common.domain

enum class JavaVersions(val major: Int, val ee: String, val filter: String) {
  JDK1_1(45, "JRE-1.1", "(&(osgi.ee=JavaSE)(version=1.1))"),
  JDK1_2(46, "J2SE-1.2", "(&(osgi.ee=JavaSE)(version=1.2))"),
  JDK1_3(47, "J2SE-1.3", "(&(osgi.ee=JavaSE)(version=1.3))"),
  JDK1_4(48, "J2SE-1.4", "(&(osgi.ee=JavaSE)(version=1.4))"),
  J2SE5(49, "J2SE-1.5", "(&(osgi.ee=JavaSE)(version=1.5))"),
  J2SE6(50, "JavaSE-1.6", "(&(osgi.ee=JavaSE)(version=1.6))"),
  OpenJDK7(51, "JavaSE-1.7", "(&(osgi.ee=JavaSE)(version=1.7))"),
  OpenJDK8(52, "JavaSE-1.8", "(&(osgi.ee=JavaSE)(version=1.8))"),
  OpenJDK9(53, "JavaSE-9", "(&(osgi.ee=JavaSE)(version=9))"),
  OpenJDK10(54, "JavaSE-10", "(&(osgi.ee=JavaSE)(version=10))"),
  OpenJDK11(55, "JavaSE-11", "(&(osgi.ee=JavaSE)(version=11))"),
  OpenJDK12(56, "JavaSE-12", "(&(osgi.ee=JavaSE)(version=12))"),
  OpenJDK13(57, "JavaSE-13", "(&(osgi.ee=JavaSE)(version=13))"),
  OpenJDK14(58, "JavaSE-14", "(&(osgi.ee=JavaSE)(version=14))"),
  OpenJDK15(59, "JavaSE-15", "(&(osgi.ee=JavaSE)(version=15))"),
  OpenJDK16(60, "JavaSE-16", "(&(osgi.ee=JavaSE)(version=16))"),
  OpenJDK17(61, "JavaSE-17", "(&(osgi.ee=JavaSE)(version=17))"),
  OpenJDK18(62, "JavaSE-18", "(&(osgi.ee=JavaSE)(version=18))"),
  OpenJDK19(63, "JavaSE-19", "(&(osgi.ee=JavaSE)(version=19))"),
  OpenJDK20(64, "JavaSE-20", "(&(osgi.ee=JavaSE)(version=20))"),
  OpenJDK21(65, "JavaSE-21", "(&(osgi.ee=JavaSE)(version=21))"),
  UNKNOWN(Integer.MAX_VALUE, "<UNKNOWN>", "(osgi.ee=UNKNOWN)");

  companion object {
    fun getJava(major: Int) = entries.firstOrNull { it.major == major } ?: UNKNOWN
    fun getJava(ee: String) = entries.firstOrNull { it.ee == ee } ?: UNKNOWN
  }
}
