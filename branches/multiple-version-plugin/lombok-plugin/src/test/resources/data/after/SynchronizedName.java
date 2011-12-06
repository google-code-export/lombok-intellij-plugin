class SynchronizedName {

  private Object read = new Object();
  private static Object READ = new Object();

  void test1() {
    synchronized (this.read) {
      System.out.println("one");
    }
  }

  void test2() {
    System.out.println("two");
  }

  static void test3() {
    synchronized (SynchronizedName.read) {
      System.out.println("three");
    }
  }

  void test4() {
    synchronized (this.READ) {
      System.out.println("four");
    }
  }

  void test5() {
    synchronized (this.read) {
      System.out.println("five");
    }
  }
}