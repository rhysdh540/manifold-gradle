package xyz.wagyourtail;

public class TestMain {

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        #if DEBUG
            System.out.println("This is a debug build.");
        #else
            System.out.println("This is a release build.");
        #endif
    }

}
